export type PlanType = 'FREE' | 'PREMIUM_MONTHLY' | 'PREMIUM_ANNUAL' | 'LIFETIME_PREMIUM';

export type SubscriptionStatus =
  | 'FREE'
  | 'ACTIVE'
  | 'CANCELLED'
  | 'EXPIRED'
  | 'PAST_DUE'
  | 'REFUNDED'
  | 'REVOKED';

export type PurchaseProvider = 'NONE' | 'GOOGLE_PLAY' | 'PAYSTACK';

export interface BackendConfig {
  firebaseProjectId: string;
  firestoreDatabaseId: string;
  port: number;
  freeIdentificationLimit: number;
  productIds: {
    monthly: string;
    annual: string;
    lifetime: string;
  };
  paystack: {
    secretKey: string;
    webhookSecret: string;
    currency: string;
    callbackUrl: string;
    monthlyPlanCode: string;
    annualPlanCode: string;
    monthlyAmountSmallestUnit: number;
    annualAmountSmallestUnit: number;
    lifetimeAmountSmallestUnit: number;
  };
  googlePlay: {
    packageName: string;
    serviceAccountJson: string;
    rtdnVerificationToken: string;
  };
}

export function loadBackendConfig(env: NodeJS.ProcessEnv = process.env): BackendConfig {
  return {
    firebaseProjectId: env.FIREBASE_PROJECT_ID || 'gen-lang-client-0826091970',
    firestoreDatabaseId:
      env.FIRESTORE_DATABASE_ID || 'ai-studio-android-529ff59b-3583-4895-a9f6-00a671592740',
    port: Number(env.PORT || 8080),
    freeIdentificationLimit: Number(env.FREE_IDENTIFICATION_LIMIT || 10),
    productIds: {
      monthly: env.PRODUCT_ID_MONTHLY || 'floramind_premium_monthly',
      annual: env.PRODUCT_ID_ANNUAL || 'floramind_premium_annual',
      lifetime: env.PRODUCT_ID_LIFETIME || 'floramind_lifetime_premium',
    },
    paystack: {
      secretKey: env.PAYSTACK_SECRET_KEY || '',
      webhookSecret: env.PAYSTACK_WEBHOOK_SECRET || env.PAYSTACK_SECRET_KEY || '',
      currency: env.PAYSTACK_CURRENCY || 'NGN',
      callbackUrl: env.PAYSTACK_CALLBACK_URL || '',
      monthlyPlanCode: env.PAYSTACK_MONTHLY_PLAN_CODE || '',
      annualPlanCode: env.PAYSTACK_ANNUAL_PLAN_CODE || '',
      monthlyAmountSmallestUnit: Number(env.PAYSTACK_MONTHLY_AMOUNT_SMALLEST_UNIT || 0),
      annualAmountSmallestUnit: Number(env.PAYSTACK_ANNUAL_AMOUNT_SMALLEST_UNIT || 0),
      lifetimeAmountSmallestUnit: Number(env.PAYSTACK_LIFETIME_AMOUNT_SMALLEST_UNIT || 0),
    },
    googlePlay: {
      packageName: env.GOOGLE_PLAY_PACKAGE_NAME || 'com.aistudio.floramind.qrzmkn',
      serviceAccountJson: env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON || '',
      rtdnVerificationToken: env.PUBSUB_RTDN_VERIFICATION_TOKEN || '',
    },
  };
}

export function resolvePlanFromProductId(productId: string, config: BackendConfig): PlanType | null {
  if (productId === config.productIds.monthly) return 'PREMIUM_MONTHLY';
  if (productId === config.productIds.annual) return 'PREMIUM_ANNUAL';
  if (productId === config.productIds.lifetime) return 'LIFETIME_PREMIUM';
  return null;
}
