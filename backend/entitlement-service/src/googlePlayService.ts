import { google } from 'googleapis';
import { BackendConfig, PlanType, resolvePlanFromProductId, SubscriptionStatus } from './config';
import { EntitlementError, SharedEntitlementService, UserEntitlementRecord } from './entitlementService';

export interface VerifyPlayPurchaseInput {
  userId: string;
  packageName: string;
  productId: string;
  purchaseToken: string;
  orderId?: string | null;
}

export class GooglePlayService {
  constructor(
    private readonly config: BackendConfig,
    private readonly entitlementService: SharedEntitlementService
  ) {}

  private getAndroidPublisher() {
    const rawJson = this.config.googlePlay.serviceAccountJson;
    if (!rawJson) {
      throw new EntitlementError(
        'CONFIGURATION_MISSING',
        'GOOGLE_PLAY_SERVICE_ACCOUNT_JSON is not configured on the backend server.',
        503
      );
    }
    const credentials = rawJson.trim().startsWith('{')
      ? JSON.parse(rawJson)
      : require(rawJson);

    const auth = new google.auth.GoogleAuth({
      credentials,
      scopes: ['https://www.googleapis.com/auth/androidpublisher'],
    });
    return google.androidpublisher({ version: 'v3', auth });
  }

  async verifyAndGrantPurchase(input: VerifyPlayPurchaseInput): Promise<UserEntitlementRecord> {
    if (input.packageName !== this.config.googlePlay.packageName) {
      throw new EntitlementError('INVALID_PURCHASE', 'Package name mismatch.', 400);
    }

    const planType = resolvePlanFromProductId(input.productId, this.config);
    if (!planType || planType === 'FREE') {
      throw new EntitlementError('INVALID_PURCHASE', `Unknown productId ${input.productId}.`, 400);
    }

    const publisher = this.getAndroidPublisher();
    if (planType === 'LIFETIME_PREMIUM') {
      return this.verifyOneTimeProduct(publisher, input);
    }
    return this.verifySubscriptionV2(publisher, input, planType);
  }

  private async verifyOneTimeProduct(
    publisher: ReturnType<typeof google.androidpublisher>,
    input: VerifyPlayPurchaseInput
  ): Promise<UserEntitlementRecord> {
    const res = await publisher.purchases.products.get({
      packageName: input.packageName,
      productId: input.productId,
      token: input.purchaseToken,
    });
    const data = res.data;

    if (
      data.obfuscatedExternalAccountId &&
      data.obfuscatedExternalAccountId !== input.userId
    ) {
      throw new EntitlementError(
        'PURCHASE_ALREADY_CLAIMED',
        'Google Play purchase is tied to another account.',
        403
      );
    }

    // purchaseState: 0 = Purchased, 1 = Canceled, 2 = Pending
    let status: SubscriptionStatus = 'ACTIVE';
    if (data.purchaseState === 1) status = 'REVOKED';
    else if (data.purchaseState === 2) status = 'PAST_DUE';
    else if (data.purchaseState !== 0) {
      throw new EntitlementError('INVALID_PURCHASE', 'Unexpected Play product purchaseState.', 400);
    }

    // Server-side acknowledgement (mandatory by Google Play within 3 days)
    if (status === 'ACTIVE' && data.acknowledgementState === 0) {
      await publisher.purchases.products.acknowledge({
        packageName: input.packageName,
        productId: input.productId,
        token: input.purchaseToken,
        requestBody: {},
      });
    }

    return this.entitlementService.applyVerifiedPurchase({
      userId: input.userId,
      provider: 'GOOGLE_PLAY',
      providerTokenOrRef: input.purchaseToken,
      purchaseReference: data.orderId || input.orderId || input.purchaseToken.slice(0, 24),
      planType: 'LIFETIME_PREMIUM',
      productId: input.productId,
      status,
      expiresAtMs: null,
      willAutoRenew: false,
    });
  }

  private async verifySubscriptionV2(
    publisher: ReturnType<typeof google.androidpublisher>,
    input: VerifyPlayPurchaseInput,
    planType: Exclude<PlanType, 'FREE' | 'LIFETIME_PREMIUM'>
  ): Promise<UserEntitlementRecord> {
    const res = await publisher.purchases.subscriptionsv2.get({
      packageName: input.packageName,
      token: input.purchaseToken,
    });
    const sub = res.data;

    const boundUid = sub.externalAccountIdentifiers?.obfuscatedExternalAccountId;
    if (boundUid && boundUid !== input.userId) {
      throw new EntitlementError(
        'PURCHASE_ALREADY_CLAIMED',
        'Google Play subscription belongs to a different FloraMind account.',
        403
      );
    }

    const lineItem = sub.lineItems?.[0];
    const expiryIso = lineItem?.expiryTime;
    const expiresAtMs = expiryIso ? Date.parse(expiryIso) : null;
    const willAutoRenew = Boolean(lineItem?.autoRenewingPlan?.autoRenewEnabled);

    let status: SubscriptionStatus = 'FREE';
    switch (sub.subscriptionState) {
      case 'SUBSCRIPTION_STATE_ACTIVE':
        status = willAutoRenew ? 'ACTIVE' : 'CANCELLED';
        break;
      case 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD':
      case 'SUBSCRIPTION_STATE_ON_HOLD':
        status = 'PAST_DUE';
        break;
      case 'SUBSCRIPTION_STATE_CANCELED':
        status = expiresAtMs && expiresAtMs > Date.now() ? 'CANCELLED' : 'EXPIRED';
        break;
      case 'SUBSCRIPTION_STATE_EXPIRED':
        status = 'EXPIRED';
        break;
      default:
        status = 'EXPIRED';
    }

    if (
      (status === 'ACTIVE' || status === 'CANCELLED') &&
      sub.acknowledgementState === 'ACKNOWLEDGEMENT_STATE_PENDING'
    ) {
      await publisher.purchases.subscriptions.acknowledge({
        packageName: input.packageName,
        subscriptionId: input.productId,
        token: input.purchaseToken,
        requestBody: {},
      });
    }

    return this.entitlementService.applyVerifiedPurchase({
      userId: input.userId,
      provider: 'GOOGLE_PLAY',
      providerTokenOrRef: input.purchaseToken,
      purchaseReference: sub.latestOrderId || input.orderId || input.purchaseToken.slice(0, 24),
      planType,
      productId: input.productId,
      status,
      expiresAtMs: Number.isFinite(expiresAtMs) ? expiresAtMs : null,
      willAutoRenew,
    });
  }

  async restorePurchases(userId: string, packageName: string, purchases: VerifyPlayPurchaseInput[]) {
    let latest = await this.entitlementService.ensureEntitlement(userId);
    for (const p of purchases) {
      latest = await this.verifyAndGrantPurchase({ ...p, userId, packageName });
    }
    return latest;
  }

  async handleRtdnPush(pubsubEnvelope: {
    message?: { data?: string; messageId?: string };
  }) {
    const encoded = pubsubEnvelope.message?.data;
    if (!encoded) return { ignored: true, reason: 'Empty RTDN payload' };

    const decoded = JSON.parse(Buffer.from(encoded, 'base64').toString('utf8')) as {
      packageName?: string;
      eventTimeMillis?: string;
      subscriptionNotification?: {
        subscriptionId: string;
        purchaseToken: string;
        notificationType: number;
      };
      oneTimeProductNotification?: {
        sku: string;
        purchaseToken: string;
        notificationType: number;
      };
    };

    const pkg = decoded.packageName || this.config.googlePlay.packageName;
    const subNote = decoded.subscriptionNotification;
    const otpNote = decoded.oneTimeProductNotification;
    const token = subNote?.purchaseToken || otpNote?.purchaseToken;
    const productId = subNote?.subscriptionId || otpNote?.sku;

    if (!token || !productId) {
      return { ignored: true, reason: 'Test or non-purchase Play notification' };
    }

    const userId = await this.entitlementService.lookupUserByPurchaseKey('GOOGLE_PLAY', token);
    if (!userId) {
      return { ignored: true, reason: 'No indexed user found for purchaseToken' };
    }

    const entitlement = await this.verifyAndGrantPurchase({
      userId,
      packageName: pkg,
      productId,
      purchaseToken: token,
    });
    return { ignored: false, entitlement };
  }
}
