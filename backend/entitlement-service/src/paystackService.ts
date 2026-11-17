import crypto from 'node:crypto';
import { BackendConfig, PlanType, SubscriptionStatus } from './config';
import { EntitlementError, SharedEntitlementService } from './entitlementService';

export function verifyPaystackWebhookSignature(
  rawBody: Buffer | string,
  signatureHeader: string | undefined,
  webhookSecret: string
): boolean {
  if (!webhookSecret || !signatureHeader) return false;
  const computed = crypto
    .createHmac('sha512', webhookSecret)
    .update(rawBody)
    .digest('hex');
  const a = Buffer.from(computed, 'utf8');
  const b = Buffer.from(signatureHeader.trim().toLowerCase(), 'utf8');
  if (a.length !== b.length) return false;
  return crypto.timingSafeEqual(a, b);
}

export class PaystackService {
  constructor(
    private readonly config: BackendConfig,
    private readonly entitlementService: SharedEntitlementService
  ) {}

  async initializeCheckout(params: {
    userId: string;
    email: string;
    planType: Exclude<PlanType, 'FREE'>;
    callbackUrl?: string;
  }) {
    const secret = this.config.paystack.secretKey;
    if (!secret || secret.startsWith('sk_live_or_test')) {
      throw new EntitlementError(
        'CONFIGURATION_MISSING',
        'Paystack secret key is not configured on the server.',
        503
      );
    }

    let amount = 0;
    let planCode: string | undefined;
    let productId = this.config.productIds.lifetime;

    if (params.planType === 'PREMIUM_MONTHLY') {
      amount = this.config.paystack.monthlyAmountSmallestUnit;
      planCode = this.config.paystack.monthlyPlanCode || undefined;
      productId = this.config.productIds.monthly;
    } else if (params.planType === 'PREMIUM_ANNUAL') {
      amount = this.config.paystack.annualAmountSmallestUnit;
      planCode = this.config.paystack.annualPlanCode || undefined;
      productId = this.config.productIds.annual;
    } else {
      amount = this.config.paystack.lifetimeAmountSmallestUnit;
    }

    if (amount <= 0 && !planCode) {
      throw new EntitlementError(
        'CONFIGURATION_MISSING',
        `Paystack price/plan for ${params.planType} is not configured on the server.`,
        503
      );
    }

    const body: Record<string, unknown> = {
      email: params.email,
      currency: this.config.paystack.currency,
      callback_url: params.callbackUrl || this.config.paystack.callbackUrl || undefined,
      metadata: {
        firebaseUid: params.userId,
        planType: params.planType,
        productId,
      },
    };
    if (planCode) body.plan = planCode;
    if (amount > 0) body.amount = amount;

    const resp = await fetch('https://api.paystack.co/transaction/initialize', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${secret}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    const json = (await resp.json()) as {
      status: boolean;
      message?: string;
      data?: { authorization_url: string; access_code: string; reference: string };
    };
    if (!resp.ok || !json.status || !json.data) {
      throw new EntitlementError('INVALID_PURCHASE', json.message || 'Paystack init failed.', 502);
    }
    return json.data;
  }

  async verifyTransactionAndGrant(reference: string, expectedUserId: string) {
    const secret = this.config.paystack.secretKey;
    if (!secret || secret.startsWith('sk_live_or_test')) {
      throw new EntitlementError(
        'CONFIGURATION_MISSING',
        'Paystack secret key is not configured on the server.',
        503
      );
    }

    const resp = await fetch(
      `https://api.paystack.co/transaction/verify/${encodeURIComponent(reference)}`,
      {
        headers: { Authorization: `Bearer ${secret}` },
      }
    );
    const json = (await resp.json()) as {
      status: boolean;
      message?: string;
      data?: Record<string, unknown>;
    };
    if (!resp.ok || !json.status || !json.data) {
      throw new EntitlementError(
        'INVALID_PURCHASE',
        json.message || 'Unable to verify Paystack transaction.',
        400
      );
    }

    const txData = json.data;
    if (txData.status !== 'success') {
      throw new EntitlementError(
        'INVALID_PURCHASE',
        `Paystack transaction status is "${String(txData.status)}".`,
        400
      );
    }

    const metadata = (txData.metadata || {}) as Record<string, unknown>;
    const metaUid = String(metadata.firebaseUid || '');
    if (metaUid && metaUid !== expectedUserId) {
      throw new EntitlementError(
        'PURCHASE_ALREADY_CLAIMED',
        'Paystack transaction belongs to a different account.',
        403
      );
    }

    const planType = this.resolvePaystackPlan(txData, metadata);
    const productId = this.resolveProductId(planType);
    const subscriptionCode =
      (txData.subscription as Record<string, unknown> | undefined)?.subscription_code ||
      String(txData.reference || reference);

    const expiresAtMs = this.computeExpiry(planType, txData.paid_at as string | undefined);

    return this.entitlementService.applyVerifiedPurchase({
      userId: expectedUserId,
      provider: 'PAYSTACK',
      providerTokenOrRef: String(subscriptionCode),
      purchaseReference: String(txData.reference || reference),
      planType,
      productId,
      status: 'ACTIVE',
      expiresAtMs,
      willAutoRenew: planType === 'PREMIUM_MONTHLY' || planType === 'PREMIUM_ANNUAL',
    });
  }

  async handleWebhookEvent(payload: { event?: string; data?: Record<string, unknown> }) {
    const event = payload.event || '';
    const data = payload.data || {};
    const metadata = (data.metadata || {}) as Record<string, unknown>;
    const subscriptionCode =
      String(data.subscription_code || '') ||
      String((data.subscription as Record<string, unknown> | undefined)?.subscription_code || '') ||
      String(data.reference || '');

    let userId = String(metadata.firebaseUid || '');
    if (!userId && subscriptionCode) {
      userId =
        (await this.entitlementService.lookupUserByPurchaseKey('PAYSTACK', subscriptionCode)) || '';
    }
    if (!userId) {
      return { ignored: true, reason: 'No Firebase UID attached or indexed for purchase reference' };
    }

    const planType = this.resolvePaystackPlan(data, metadata);
    const productId = this.resolveProductId(planType);
    const eventKey = `PAYSTACK_${event}_${String(data.id || subscriptionCode || Date.now())}`;

    let status: SubscriptionStatus = 'ACTIVE';
    let willAutoRenew = planType === 'PREMIUM_MONTHLY' || planType === 'PREMIUM_ANNUAL';
    let expiresAtMs: number | null = this.computeExpiry(planType, data.paid_at as string | undefined);

    if (event === 'charge.success' || event === 'subscription.create') {
      status = 'ACTIVE';
    } else if (event === 'subscription.not_renew') {
      status = 'CANCELLED';
      willAutoRenew = false;
      expiresAtMs = parseIsoMillis(data.next_payment_date) ?? expiresAtMs;
    } else if (event === 'invoice.payment_failed') {
      status = 'PAST_DUE';
    } else if (event === 'subscription.disable') {
      status = 'EXPIRED';
      willAutoRenew = false;
      expiresAtMs = Date.now();
    } else if (event === 'refund.processed') {
      status = 'REFUNDED';
      willAutoRenew = false;
    } else {
      return { ignored: true, reason: `Unhandled Paystack event ${event}` };
    }

    const entitlement = await this.entitlementService.applyVerifiedPurchase({
      userId,
      provider: 'PAYSTACK',
      providerTokenOrRef: subscriptionCode || String(data.reference || eventKey),
      purchaseReference: String(data.reference || subscriptionCode || eventKey),
      planType,
      productId,
      status,
      expiresAtMs,
      willAutoRenew,
      webhookEventId: eventKey,
    });

    return { ignored: false, entitlement };
  }

  private resolvePaystackPlan(
    data: Record<string, unknown>,
    metadata: Record<string, unknown>
  ): Exclude<PlanType, 'FREE'> {
    const rawMetaPlan = String(metadata.planType || '');
    if (
      rawMetaPlan === 'PREMIUM_MONTHLY' ||
      rawMetaPlan === 'PREMIUM_ANNUAL' ||
      rawMetaPlan === 'LIFETIME_PREMIUM'
    ) {
      return rawMetaPlan;
    }
    const planCode = String(
      (data.plan as Record<string, unknown> | undefined)?.plan_code || data.plan_object || ''
    );
    if (planCode && planCode === this.config.paystack.monthlyPlanCode) return 'PREMIUM_MONTHLY';
    if (planCode && planCode === this.config.paystack.annualPlanCode) return 'PREMIUM_ANNUAL';
    return 'LIFETIME_PREMIUM';
  }

  private resolveProductId(planType: Exclude<PlanType, 'FREE'>): string {
    if (planType === 'PREMIUM_MONTHLY') return this.config.productIds.monthly;
    if (planType === 'PREMIUM_ANNUAL') return this.config.productIds.annual;
    return this.config.productIds.lifetime;
  }

  private computeExpiry(planType: PlanType, paidAtIso?: string): number | null {
    if (planType === 'LIFETIME_PREMIUM' || planType === 'FREE') return null;
    const baseMs = parseIsoMillis(paidAtIso) ?? Date.now();
    const days = planType === 'PREMIUM_ANNUAL' ? 366 : 31;
    return baseMs + days * 24 * 60 * 60 * 1000;
  }
}

function parseIsoMillis(value: unknown): number | null {
  if (typeof value !== 'string' || !value) return null;
  const parsed = Date.parse(value);
  return Number.isFinite(parsed) ? parsed : null;
}
