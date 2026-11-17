import crypto from 'node:crypto';
import type { Firestore, Timestamp } from 'firebase-admin/firestore';
import { PlanType, PurchaseProvider, SubscriptionStatus } from './config';

export interface UserEntitlementRecord {
  userId: string;
  planType: PlanType;
  freeIdentificationsUsed: number;
  freeIdentificationsLimit: number;
  subscriptionStatus: SubscriptionStatus;
  expiresAtMs: number | null;
  purchaseProvider: PurchaseProvider;
  purchaseReference: string;
  productId: string;
  willAutoRenew: boolean;
  hasLifetimeEntitlement: boolean;
  lastScanRequestId: string;
  schemaVersion: number;
  updatedAtMs: number;
}

export interface PurchaseLedgerRecord {
  purchaseKey: string;
  userId: string;
  provider: PurchaseProvider;
  planType: PlanType;
  productId: string;
  purchaseReference: string;
  status: SubscriptionStatus;
  expiresAtMs: number | null;
  updatedAtMs: number;
}

export interface ApplyVerifiedPurchaseInput {
  userId: string;
  provider: Exclude<PurchaseProvider, 'NONE'>;
  providerTokenOrRef: string;
  purchaseReference: string;
  planType: PlanType;
  productId: string;
  status: SubscriptionStatus;
  expiresAtMs: number | null;
  willAutoRenew: boolean;
  webhookEventId?: string;
}

export interface EntitlementStore {
  runTransaction<T>(fn: (tx: EntitlementTransaction) => Promise<T>): Promise<T>;
}

export interface EntitlementTransaction {
  getEntitlement(userId: string): Promise<UserEntitlementRecord | null>;
  setEntitlement(record: UserEntitlementRecord): Promise<void>;
  getLedger(purchaseKey: string): Promise<PurchaseLedgerRecord | null>;
  setLedger(record: PurchaseLedgerRecord): Promise<void>;
  hasWebhookEvent(eventId: string): Promise<boolean>;
  markWebhookEvent(eventId: string, metadata: Record<string, unknown>): Promise<void>;
}

export class EntitlementError extends Error {
  constructor(
    public readonly code:
      | 'QUOTA_EXHAUSTED'
      | 'PURCHASE_ALREADY_CLAIMED'
      | 'INVALID_PURCHASE'
      | 'CONFIGURATION_MISSING',
    message: string,
    public readonly httpStatus: number
  ) {
    super(message);
  }
}

export function buildPurchaseKey(provider: PurchaseProvider, tokenOrRef: string): string {
  const hash = crypto.createHash('sha256').update(`${provider}:${tokenOrRef}`).digest('hex');
  return `${provider}_${hash.slice(0, 40)}`;
}

export function isEntitlementPremiumActive(
  record: UserEntitlementRecord,
  nowMs: number = Date.now()
): boolean {
  if (
    record.hasLifetimeEntitlement &&
    record.subscriptionStatus !== 'REFUNDED' &&
    record.subscriptionStatus !== 'REVOKED'
  ) {
    return true;
  }
  if (record.planType === 'FREE') return false;
  if (record.planType === 'LIFETIME_PREMIUM') {
    return record.subscriptionStatus !== 'REFUNDED' && record.subscriptionStatus !== 'REVOKED';
  }
  const activeState = record.subscriptionStatus === 'ACTIVE' || record.subscriptionStatus === 'CANCELLED';
  const notExpired = record.expiresAtMs === null || record.expiresAtMs > nowMs;
  return activeState && notExpired;
}

export class SharedEntitlementService {
  constructor(
    private readonly store: EntitlementStore,
    private readonly freeLimit: number = 10
  ) {}

  defaultEntitlement(userId: string, nowMs: number = Date.now()): UserEntitlementRecord {
    return {
      userId,
      planType: 'FREE',
      freeIdentificationsUsed: 0,
      freeIdentificationsLimit: this.freeLimit,
      subscriptionStatus: 'FREE',
      expiresAtMs: null,
      purchaseProvider: 'NONE',
      purchaseReference: '',
      productId: '',
      willAutoRenew: false,
      hasLifetimeEntitlement: false,
      lastScanRequestId: '',
      schemaVersion: 1,
      updatedAtMs: nowMs,
    };
  }

  async ensureEntitlement(userId: string, nowMs: number = Date.now()): Promise<UserEntitlementRecord> {
    return this.store.runTransaction(async (tx) => {
      const existing = await tx.getEntitlement(userId);
      if (existing) {
        return this.normalizeExpiry(existing, nowMs, tx);
      }
      const created = this.defaultEntitlement(userId, nowMs);
      await tx.setEntitlement(created);
      return created;
    });
  }

  async consumeIdentification(
    userId: string,
    requestId: string,
    nowMs: number = Date.now()
  ): Promise<UserEntitlementRecord> {
    if (!requestId || requestId.trim().length < 6) {
      throw new EntitlementError('INVALID_PURCHASE', 'Valid scan requestId is required.', 400);
    }

    return this.store.runTransaction(async (tx) => {
      const currentRaw = (await tx.getEntitlement(userId)) ?? this.defaultEntitlement(userId, nowMs);
      const current = await this.normalizeExpiry(currentRaw, nowMs, tx);

      if (isEntitlementPremiumActive(current, nowMs)) {
        return current;
      }

      // Idempotent retry: same requestId does not double-charge
      if (current.lastScanRequestId === requestId) {
        return current;
      }

      if (current.freeIdentificationsUsed >= current.freeIdentificationsLimit) {
        throw new EntitlementError(
          'QUOTA_EXHAUSTED',
          `Free limit of ${current.freeIdentificationsLimit} identifications reached.`,
          402
        );
      }

      const updated: UserEntitlementRecord = {
        ...current,
        freeIdentificationsUsed: current.freeIdentificationsUsed + 1,
        lastScanRequestId: requestId,
        updatedAtMs: nowMs,
      };
      await tx.setEntitlement(updated);
      return updated;
    });
  }

  async applyVerifiedPurchase(
    input: ApplyVerifiedPurchaseInput,
    nowMs: number = Date.now()
  ): Promise<UserEntitlementRecord> {
    const purchaseKey = buildPurchaseKey(input.provider, input.providerTokenOrRef);

    return this.store.runTransaction(async (tx) => {
      if (input.webhookEventId) {
        const seen = await tx.hasWebhookEvent(input.webhookEventId);
        if (seen) {
          const current =
            (await tx.getEntitlement(input.userId)) ?? this.defaultEntitlement(input.userId, nowMs);
          return current;
        }
      }

      const existingLedger = await tx.getLedger(purchaseKey);
      if (existingLedger && existingLedger.userId !== input.userId) {
        throw new EntitlementError(
          'PURCHASE_ALREADY_CLAIMED',
          'This purchase is already linked to a different FloraMind account.',
          409
        );
      }

      const current =
        (await tx.getEntitlement(input.userId)) ?? this.defaultEntitlement(input.userId, nowMs);

      const isRevokedLifetime =
        input.planType === 'LIFETIME_PREMIUM' &&
        (input.status === 'REFUNDED' || input.status === 'REVOKED');

      // Preserve Lifetime Premium even if a prior recurring subscription expires
      const nextHasLifetime =
        isRevokedLifetime
          ? false
          : current.hasLifetimeEntitlement ||
            (input.planType === 'LIFETIME_PREMIUM' && input.status === 'ACTIVE');

      let nextPlan: PlanType = input.planType;
      let nextStatus: SubscriptionStatus = input.status;
      let nextExpiresAtMs = input.expiresAtMs;
      let nextAutoRenew = input.willAutoRenew;

      if (nextHasLifetime && input.planType !== 'LIFETIME_PREMIUM') {
        nextPlan = 'LIFETIME_PREMIUM';
        nextStatus = 'ACTIVE';
        nextExpiresAtMs = null;
        nextAutoRenew = false;
      } else if (
        (input.status === 'REFUNDED' || input.status === 'REVOKED' || input.status === 'EXPIRED') &&
        !nextHasLifetime
      ) {
        nextPlan = 'FREE';
        nextStatus = input.status;
        nextAutoRenew = false;
      }

      const updated: UserEntitlementRecord = {
        ...current,
        userId: input.userId,
        planType: nextPlan,
        subscriptionStatus: nextStatus,
        expiresAtMs: nextExpiresAtMs,
        purchaseProvider: input.provider,
        purchaseReference: input.purchaseReference,
        productId: input.productId,
        willAutoRenew: nextAutoRenew,
        hasLifetimeEntitlement: nextHasLifetime,
        updatedAtMs: nowMs,
      };

      await tx.setLedger({
        purchaseKey,
        userId: input.userId,
        provider: input.provider,
        planType: input.planType,
        productId: input.productId,
        purchaseReference: input.purchaseReference,
        status: input.status,
        expiresAtMs: input.expiresAtMs,
        updatedAtMs: nowMs,
      });

      await tx.setEntitlement(updated);

      if (input.webhookEventId) {
        await tx.markWebhookEvent(input.webhookEventId, {
          userId: input.userId,
          provider: input.provider,
          planType: input.planType,
          status: input.status,
          processedAtMs: nowMs,
        });
      }

      return updated;
    });
  }

  async lookupUserByPurchaseKey(
    provider: PurchaseProvider,
    tokenOrRef: string
  ): Promise<string | null> {
    const key = buildPurchaseKey(provider, tokenOrRef);
    return this.store.runTransaction(async (tx) => {
      const ledger = await tx.getLedger(key);
      return ledger?.userId ?? null;
    });
  }

  private async normalizeExpiry(
    record: UserEntitlementRecord,
    nowMs: number,
    tx: EntitlementTransaction
  ): Promise<UserEntitlementRecord> {
    if (
      (record.planType === 'PREMIUM_MONTHLY' || record.planType === 'PREMIUM_ANNUAL') &&
      (record.subscriptionStatus === 'ACTIVE' || record.subscriptionStatus === 'CANCELLED') &&
      record.expiresAtMs !== null &&
      record.expiresAtMs <= nowMs &&
      !record.hasLifetimeEntitlement
    ) {
      const expired: UserEntitlementRecord = {
        ...record,
        planType: 'FREE',
        subscriptionStatus: 'EXPIRED',
        willAutoRenew: false,
        updatedAtMs: nowMs,
      };
      await tx.setEntitlement(expired);
      return expired;
    }
    return record;
  }
}

export class FirestoreEntitlementStore implements EntitlementStore {
  constructor(private readonly db: Firestore) {}

  async runTransaction<T>(fn: (tx: EntitlementTransaction) => Promise<T>): Promise<T> {
    const { Timestamp: AdminTimestamp, FieldValue } = await import('firebase-admin/firestore');
    return this.db.runTransaction(async (fsTx) => {
      const adapter: EntitlementTransaction = {
        getEntitlement: async (userId) => {
          const snap = await fsTx.get(this.db.collection('entitlements').doc(userId));
          if (!snap.exists) return null;
          const d = snap.data() || {};
          const exp = d.expiresAt as Timestamp | undefined | null;
          const upd = d.updatedAt as Timestamp | undefined | null;
          return {
            userId: String(d.userId || userId),
            planType: (d.planType as PlanType) || 'FREE',
            freeIdentificationsUsed: Number(d.freeIdentificationsUsed || 0),
            freeIdentificationsLimit: Number(d.freeIdentificationsLimit || 10),
            subscriptionStatus: (d.subscriptionStatus as SubscriptionStatus) || 'FREE',
            expiresAtMs: exp ? exp.toMillis() : null,
            purchaseProvider: (d.purchaseProvider as PurchaseProvider) || 'NONE',
            purchaseReference: String(d.purchaseReference || ''),
            productId: String(d.productId || ''),
            willAutoRenew: Boolean(d.willAutoRenew),
            hasLifetimeEntitlement: Boolean(d.hasLifetimeEntitlement),
            lastScanRequestId: String(d.lastScanRequestId || ''),
            schemaVersion: Number(d.schemaVersion || 1),
            updatedAtMs: upd ? upd.toMillis() : Date.now(),
          };
        },
        setEntitlement: async (record) => {
          const ref = this.db.collection('entitlements').doc(record.userId);
          const snap = await fsTx.get(ref);
          const payload: Record<string, unknown> = {
            userId: record.userId,
            planType: record.planType,
            freeIdentificationsUsed: record.freeIdentificationsUsed,
            freeIdentificationsLimit: record.freeIdentificationsLimit,
            subscriptionStatus: record.subscriptionStatus,
            expiresAt: record.expiresAtMs ? AdminTimestamp.fromMillis(record.expiresAtMs) : null,
            purchaseProvider: record.purchaseProvider,
            purchaseReference: record.purchaseReference,
            productId: record.productId,
            willAutoRenew: record.willAutoRenew,
            hasLifetimeEntitlement: record.hasLifetimeEntitlement,
            lastScanRequestId: record.lastScanRequestId,
            schemaVersion: record.schemaVersion,
            updatedAt: FieldValue.serverTimestamp(),
          };
          if (!snap.exists) {
            payload.createdAt = FieldValue.serverTimestamp();
          }
          fsTx.set(ref, payload, { merge: true });
        },
        getLedger: async (purchaseKey) => {
          const snap = await fsTx.get(this.db.collection('purchase_ledger').doc(purchaseKey));
          if (!snap.exists) return null;
          return snap.data() as PurchaseLedgerRecord;
        },
        setLedger: async (record) => {
          fsTx.set(this.db.collection('purchase_ledger').doc(record.purchaseKey), record, {
            merge: true,
          });
        },
        hasWebhookEvent: async (eventId) => {
          const snap = await fsTx.get(this.db.collection('webhook_events').doc(eventId));
          return snap.exists;
        },
        markWebhookEvent: async (eventId, metadata) => {
          fsTx.set(this.db.collection('webhook_events').doc(eventId), metadata);
        },
      };
      return fn(adapter);
    });
  }
}
