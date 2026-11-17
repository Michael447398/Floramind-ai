import test from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const require = createRequire(import.meta.url);

// Lightweight in-memory store validating exact SharedEntitlementService business rules
function createInMemoryStore() {
  const entitlements = new Map();
  const ledger = new Map();
  const webhookEvents = new Map();

  return {
    entitlements,
    ledger,
    webhookEvents,
    async runTransaction(fn) {
      const tx = {
        getEntitlement: async (uid) => entitlements.get(uid) ?? null,
        setEntitlement: async (rec) => {
          entitlements.set(rec.userId, rec);
        },
        getLedger: async (key) => ledger.get(key) ?? null,
        setLedger: async (rec) => {
          ledger.set(rec.purchaseKey, rec);
        },
        hasWebhookEvent: async (id) => webhookEvents.has(id),
        markWebhookEvent: async (id, meta) => {
          webhookEvents.set(id, meta);
        },
      };
      return fn(tx);
    },
  };
}

// Compile TypeScript first in npm test, or import built dist when present
const distDir = path.join(__dirname, '..', 'dist');
const hasDist = fs.existsSync(path.join(distDir, 'entitlementService.js'));

test('Shared entitlement service dist artifacts and Paystack HMAC logic check', async () => {
  assert.ok(hasDist, 'Expected dist/entitlementService.js to exist after tsc build');
  const { SharedEntitlementService, EntitlementError, isEntitlementPremiumActive } = require(
    path.join(distDir, 'entitlementService.js')
  );
  const { verifyPaystackWebhookSignature } = require(
    path.join(distDir, 'paystackService.js')
  );

  const store = createInMemoryStore();
  const service = new SharedEntitlementService(store, 10);

  // 1. FREE plan starts at 0 of 10 and blocks at 11th request
  const uid = 'user_free_1';
  const initEnt = await service.ensureEntitlement(uid, 1000);
  assert.equal(initEnt.planType, 'FREE');
  assert.equal(initEnt.freeIdentificationsUsed, 0);

  for (let i = 1; i <= 10; i++) {
    const res = await service.consumeIdentification(uid, `scan_req_${i}`, 1000 + i);
    assert.equal(res.freeIdentificationsUsed, i);
  }

  // Idempotent retry with already-used requestId does NOT trigger quota exhausted
  const retry = await service.consumeIdentification(uid, 'scan_req_10', 2000);
  assert.equal(retry.freeIdentificationsUsed, 10);

  // 11th unique request MUST fail with QUOTA_EXHAUSTED
  await assert.rejects(
    () => service.consumeIdentification(uid, 'scan_req_11', 3000),
    (err) => err instanceof EntitlementError && err.code === 'QUOTA_EXHAUSTED'
  );

  // 2. MONTHLY -> EXPIRED -> downgraded back to FREE
  const monthlyUser = 'user_monthly_1';
  const futureExpiry = Date.now() + 30 * 24 * 60 * 60 * 1000;
  const activeMonthly = await service.applyVerifiedPurchase({
    userId: monthlyUser,
    provider: 'GOOGLE_PLAY',
    providerTokenOrRef: 'play_tok_monthly_abc',
    purchaseReference: 'GPA.1234',
    planType: 'PREMIUM_MONTHLY',
    productId: 'floramind_premium_monthly',
    status: 'ACTIVE',
    expiresAtMs: futureExpiry,
    willAutoRenew: true,
  });
  assert.equal(activeMonthly.planType, 'PREMIUM_MONTHLY');
  assert.equal(isEntitlementPremiumActive(activeMonthly), true);

  // Unlimited scans while monthly is active
  const postScan = await service.consumeIdentification(monthlyUser, 'scan_unlimited_1');
  assert.equal(postScan.freeIdentificationsUsed, 0);

  // Prevent purchase sharing across different UIDs
  await assert.rejects(
    () =>
      service.applyVerifiedPurchase({
        userId: 'other_thief_uid',
        provider: 'GOOGLE_PLAY',
        providerTokenOrRef: 'play_tok_monthly_abc',
        purchaseReference: 'GPA.1234',
        planType: 'PREMIUM_MONTHLY',
        productId: 'floramind_premium_monthly',
        status: 'ACTIVE',
        expiresAtMs: futureExpiry,
        willAutoRenew: true,
      }),
    (err) => err instanceof EntitlementError && err.code === 'PURCHASE_ALREADY_CLAIMED'
  );

  // Expiry check returns user to FREE
  const expiredCheck = await service.ensureEntitlement(monthlyUser, futureExpiry + 5000);
  assert.equal(expiredCheck.planType, 'FREE');
  assert.equal(expiredCheck.subscriptionStatus, 'EXPIRED');

  // 3. ANNUAL + LIFETIME preservation
  const annualUser = 'user_annual_lifetime';
  await service.applyVerifiedPurchase({
    userId: annualUser,
    provider: 'PAYSTACK',
    providerTokenOrRef: 'SUB_annual_1',
    purchaseReference: 'REF_annual_1',
    planType: 'PREMIUM_ANNUAL',
    productId: 'floramind_premium_annual',
    status: 'ACTIVE',
    expiresAtMs: Date.now() + 365 * 24 * 60 * 60 * 1000,
    willAutoRenew: true,
  });
  await service.applyVerifiedPurchase({
    userId: annualUser,
    provider: 'PAYSTACK',
    providerTokenOrRef: 'REF_lifetime_1',
    purchaseReference: 'REF_lifetime_1',
    planType: 'LIFETIME_PREMIUM',
    productId: 'floramind_lifetime_premium',
    status: 'ACTIVE',
    expiresAtMs: null,
    willAutoRenew: false,
  });
  // Simulated subscription disable webhook must NOT revoke permanent Lifetime entitlement
  const afterSubExpired = await service.applyVerifiedPurchase({
    userId: annualUser,
    provider: 'PAYSTACK',
    providerTokenOrRef: 'SUB_annual_1',
    purchaseReference: 'REF_annual_1',
    planType: 'PREMIUM_ANNUAL',
    productId: 'floramind_premium_annual',
    status: 'EXPIRED',
    expiresAtMs: Date.now(),
    willAutoRenew: false,
  });
  assert.equal(afterSubExpired.planType, 'LIFETIME_PREMIUM');
  assert.equal(afterSubExpired.hasLifetimeEntitlement, true);

  // 4. Paystack webhook HMAC-SHA512 verification
  const secret = 'test_webhook_secret';
  const payload = JSON.stringify({ event: 'charge.success', data: { reference: 'ref_1' } });
  const sig = crypto.createHmac('sha512', secret).update(payload).digest('hex');
  assert.equal(verifyPaystackWebhookSignature(payload, sig, secret), true);
  assert.equal(verifyPaystackWebhookSignature(payload, 'deadbeef', secret), false);
});
