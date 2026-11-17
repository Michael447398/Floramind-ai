import express, { Request, Response, NextFunction } from 'express';
import admin from 'firebase-admin';
import { getFirestore } from 'firebase-admin/firestore';
import { loadBackendConfig, PlanType } from './config';
import {
  EntitlementError,
  FirestoreEntitlementStore,
  SharedEntitlementService,
} from './entitlementService';
import { GooglePlayService, VerifyPlayPurchaseInput } from './googlePlayService';
import { PaystackService, verifyPaystackWebhookSignature } from './paystackService';

const config = loadBackendConfig();

if (!admin.apps.length) {
  admin.initializeApp({
    projectId: config.firebaseProjectId,
  });
}

const firestore = getFirestore(admin.app(), config.firestoreDatabaseId);
const entitlementStore = new FirestoreEntitlementStore(firestore);
const entitlementService = new SharedEntitlementService(
  entitlementStore,
  config.freeIdentificationLimit
);
const paystackService = new PaystackService(config, entitlementService);
const googlePlayService = new GooglePlayService(config, entitlementService);

export const app = express();

// Capture raw body for Paystack HMAC-SHA512 webhook verification
app.use(
  express.json({
    verify: (req, _res, buf) => {
      (req as Request & { rawBody?: Buffer }).rawBody = buf;
    },
  })
);

interface AuthedRequest extends Request {
  authUser?: admin.auth.DecodedIdToken;
}

async function requireFirebaseAuth(req: AuthedRequest, res: Response, next: NextFunction) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7).trim() : '';
  if (!token) {
    res.status(401).json({ ok: false, error: 'Missing Firebase Bearer ID token.' });
    return;
  }
  try {
    req.authUser = await admin.auth().verifyIdToken(token);
    next();
  } catch (err) {
    res.status(401).json({ ok: false, error: 'Invalid or expired Firebase ID token.' });
  }
}

app.get('/healthz', (_req, res) => {
  res.json({ ok: true, service: 'floramind-entitlement-service' });
});

app.get('/api/entitlement', requireFirebaseAuth, async (req: AuthedRequest, res, next) => {
  try {
    const entitlement = await entitlementService.ensureEntitlement(req.authUser!.uid);
    res.json({ ok: true, entitlement });
  } catch (err) {
    next(err);
  }
});

app.post(
  '/api/identifications/consume',
  requireFirebaseAuth,
  async (req: AuthedRequest, res, next) => {
    try {
      const requestId = String(req.body?.requestId || '');
      const entitlement = await entitlementService.consumeIdentification(
        req.authUser!.uid,
        requestId
      );
      res.json({ ok: true, entitlement });
    } catch (err) {
      next(err);
    }
  }
);

app.post(
  '/api/payments/play/verify',
  requireFirebaseAuth,
  async (req: AuthedRequest, res, next) => {
    try {
      const entitlement = await googlePlayService.verifyAndGrantPurchase({
        userId: req.authUser!.uid,
        packageName: String(req.body?.packageName || config.googlePlay.packageName),
        productId: String(req.body?.productId || ''),
        purchaseToken: String(req.body?.purchaseToken || ''),
        orderId: req.body?.orderId ? String(req.body.orderId) : null,
      });
      res.json({ ok: true, entitlement });
    } catch (err) {
      next(err);
    }
  }
);

app.post(
  '/api/payments/play/restore',
  requireFirebaseAuth,
  async (req: AuthedRequest, res, next) => {
    try {
      const packageName = String(req.body?.packageName || config.googlePlay.packageName);
      const purchases = Array.isArray(req.body?.purchases)
        ? (req.body.purchases as VerifyPlayPurchaseInput[])
        : [];
      const entitlement = await googlePlayService.restorePurchases(
        req.authUser!.uid,
        packageName,
        purchases
      );
      res.json({ ok: true, entitlement });
    } catch (err) {
      next(err);
    }
  }
);

app.post(
  '/api/payments/paystack/initialize',
  requireFirebaseAuth,
  async (req: AuthedRequest, res, next) => {
    try {
      const planType = String(req.body?.planType || '') as Exclude<PlanType, 'FREE'>;
      const email = req.authUser?.email;
      if (!email) {
        throw new EntitlementError('INVALID_PURCHASE', 'Verified email required for checkout.', 400);
      }
      const data = await paystackService.initializeCheckout({
        userId: req.authUser!.uid,
        email,
        planType,
        callbackUrl: req.body?.callbackUrl ? String(req.body.callbackUrl) : undefined,
      });
      res.json({ ok: true, data });
    } catch (err) {
      next(err);
    }
  }
);

app.post(
  '/api/payments/paystack/verify',
  requireFirebaseAuth,
  async (req: AuthedRequest, res, next) => {
    try {
      const reference = String(req.body?.reference || '');
      const entitlement = await paystackService.verifyTransactionAndGrant(
        reference,
        req.authUser!.uid
      );
      res.json({ ok: true, entitlement });
    } catch (err) {
      next(err);
    }
  }
);

app.post('/api/webhooks/paystack', async (req: Request, res, next) => {
  try {
    const rawBody = (req as Request & { rawBody?: Buffer }).rawBody || JSON.stringify(req.body);
    const sig = req.headers['x-paystack-signature'] as string | undefined;
    if (!verifyPaystackWebhookSignature(rawBody, sig, config.paystack.webhookSecret)) {
      res.status(401).json({ ok: false, error: 'Invalid Paystack signature.' });
      return;
    }
    const result = await paystackService.handleWebhookEvent(req.body);
    res.json({ ok: true, ...result });
  } catch (err) {
    next(err);
  }
});

app.post('/api/webhooks/play-rtdn', async (req: Request, res, next) => {
  try {
    const expectedToken = config.googlePlay.rtdnVerificationToken;
    if (expectedToken && req.query.token !== expectedToken) {
      res.status(403).json({ ok: false, error: 'Forbidden RTDN push token.' });
      return;
    }
    const result = await googlePlayService.handleRtdnPush(req.body);
    res.json({ ok: true, ...result });
  } catch (err) {
    next(err);
  }
});

app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
  if (err instanceof EntitlementError) {
    res.status(err.httpStatus).json({ ok: false, code: err.code, error: err.message });
    return;
  }
  const msg = err instanceof Error ? err.message : 'Internal server error';
  res.status(500).json({ ok: false, error: msg });
});

if (require.main === module) {
  app.listen(config.port, () => {
    console.log(`FloraMind entitlement service listening on port ${config.port}`);
  });
}
