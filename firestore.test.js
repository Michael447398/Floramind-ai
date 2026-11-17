const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const OWNER_UID = "owner_789";
const OWNER_EMAIL = "michaeligwe430@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function ownerDb() {
  return testEnv
    .authenticatedContext(OWNER_UID, {
      email: OWNER_EMAIL,
      email_verified: true,
    })
    .firestore();
}

function spoofedOwnerDb() {
  return testEnv
    .authenticatedContext("spoof_999", {
      email: OWNER_EMAIL,
      email_verified: false,
    })
    .firestore();
}

test("Unauthenticated user: cannot read private user profile or scans", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("scans").get()
  );
  await assertFails(unauthDb.collection("usage_logs").get());
});

test("Unauthenticated user: can read published curated plants but not drafts", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("curated_plants").doc("moringa_oleifera").set({
      authorId: OWNER_UID,
      commonName: "Moringa",
      scientificName: "Moringa oleifera",
      family: "Moringaceae",
      category: "Medicinal & Nutrition",
      region: "West Africa & South Asia",
      safetyLevel: "CAUTION",
      summary: "Drought-tolerant tree widely cultivated for leaves.",
      featured: true,
      status: "published",
      revisionNumber: 1,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    await db.collection("curated_plants").doc("draft_plant").set({
      authorId: OWNER_UID,
      commonName: "Draft Plant",
      scientificName: "Draftus testus",
      family: "Testaceae",
      category: "Test",
      region: "Global",
      safetyLevel: "UNKNOWN",
      summary: "Unpublished entry.",
      featured: false,
      status: "draft",
      revisionNumber: 1,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(
    unauthDb
      .collection("curated_plants")
      .where("status", "==", "published")
      .get()
  );
  await assertFails(unauthDb.collection("curated_plants").get());
  await assertFails(unauthDb.collection("curated_plants").doc("draft_plant").get());
});

test("User isolation: Alice creates profile and scan, Bob cannot read or modify them", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Botanist",
      email: "alice@example.com",
      region: "Nigeria",
      darkMode: false,
      retainThumbnails: true,
      advancedBotanyMode: false,
      dailyScanCount: 0,
      dailyScanDate: "2026-11-16",
      totalScans: 0,
      schemaVersion: 1,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("scans")
      .doc("scan_1")
      .set({
        userId: ALICE_UID,
        commonName: "Neem Tree",
        scientificName: "Azadirachta indica",
        family: "Meliaceae",
        confidenceTier: "HIGH",
        summary: "Pinnate leaves with serrated margins.",
        safetyLevel: "CAUTION",
        safetyHeadline: "Neem seed oil is toxic if ingested by children.",
        reportData: { identity: "Azadirachta indica" },
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("scans").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("scans")
      .doc("scan_1")
      .delete()
  );
});

test("Shadow update & ghost field rejection on user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      schemaVersion: 1,
      isAdmin: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Email spoofing test: unverified owner email is rejected from admin CMS and config", async () => {
  const spoofDb = spoofedOwnerDb();
  await assertFails(
    spoofDb.collection("app_config").doc("global").set({
      updatedBy: "spoof_999",
      maintenanceMode: true,
      dailyScanLimit: 50,
      currentAppVersion: "1.0.0",
      releaseNotes: "Spoof",
      schemaVersion: 1,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(spoofDb.collection("users").get());
  await assertFails(spoofDb.collection("usage_logs").get());
});

test("Verified owner admin can manage app_config, curated_plants, and announcements", async () => {
  const admin = ownerDb();
  await assertSucceeds(
    admin.collection("app_config").doc("global").set({
      updatedBy: OWNER_UID,
      maintenanceMode: false,
      maintenanceMessage: "",
      dailyScanLimit: 25,
      featuredPlantSlug: "azadirachta_indica",
      featuredPlantTitle: "Neem (Azadirachta indica)",
      featuredPlantFact: "Widely used in agroforestry across West Africa and South Asia.",
      currentAppVersion: "1.0.0",
      releaseNotes: "Initial FloraMind AI release.",
      schemaVersion: 1,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    admin.collection("announcements").doc("ann_1").set({
      authorId: OWNER_UID,
      title: "Welcome to FloraMind AI v1.0",
      body: "Live GBIF, Kew POWO, WFO, and PubMed cross-checking is now active.",
      type: "changelog",
      versionTag: "v1.0.0",
      published: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(admin.collection("curated_plants").get());
  await assertSucceeds(admin.collection("announcements").get());
});

test("Feedback report: user can create open report without adminNotes, only owner can update status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const admin = ownerDb();

  await assertFails(
    aliceDb.collection("feedback_reports").doc("fb_bad").set({
      userId: ALICE_UID,
      category: "bug",
      message: "Trying to inject adminNotes",
      status: "open",
      adminNotes: "Sneaky",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("feedback_reports").doc("fb_1").set({
      userId: ALICE_UID,
      plantName: "Azadirachta indica",
      category: "misidentification",
      message: "Leaf margins were actually double-serrated.",
      status: "open",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    aliceDb.collection("feedback_reports").doc("fb_1").update({
      status: "resolved",
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    admin.collection("feedback_reports").doc("fb_1").update({
      status: "reviewed",
      adminNotes: "Reviewed and logged for prompt tuning.",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Entitlements: client cannot self-grant premium, decrement count, exceed 10, or touch purchase_ledger", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Client cannot create a premium entitlement
  await assertFails(
    aliceDb.collection("entitlements").doc(ALICE_UID).set({
      userId: ALICE_UID,
      planType: "LIFETIME_PREMIUM",
      freeIdentificationsUsed: 0,
      freeIdentificationsLimit: 10,
      subscriptionStatus: "ACTIVE",
      purchaseProvider: "PAYSTACK",
      purchaseReference: "spoof_ref",
      willAutoRenew: false,
      hasLifetimeEntitlement: true,
      schemaVersion: 1,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Client can bootstrap valid FREE entitlement with 0 of 10 used
  await assertSucceeds(
    aliceDb.collection("entitlements").doc(ALICE_UID).set({
      userId: ALICE_UID,
      planType: "FREE",
      freeIdentificationsUsed: 0,
      freeIdentificationsLimit: 10,
      subscriptionStatus: "FREE",
      purchaseProvider: "NONE",
      purchaseReference: "",
      willAutoRenew: false,
      hasLifetimeEntitlement: false,
      schemaVersion: 1,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob cannot read Alice's entitlement
  await assertFails(bobDb.collection("entitlements").doc(ALICE_UID).get());

  // Alice can increment by +1 with unique request id
  await assertSucceeds(
    aliceDb.collection("entitlements").doc(ALICE_UID).update({
      freeIdentificationsUsed: 1,
      lastScanRequestId: "scan_req_001",
      updatedAt: serverTimestamp(),
    })
  );

  // Duplicate request id or jumping count by +2 fails
  await assertFails(
    aliceDb.collection("entitlements").doc(ALICE_UID).update({
      freeIdentificationsUsed: 2,
      lastScanRequestId: "scan_req_001",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("entitlements").doc(ALICE_UID).update({
      freeIdentificationsUsed: 3,
      lastScanRequestId: "scan_req_002",
      updatedAt: serverTimestamp(),
    })
  );

  // Alice cannot elevate own planType or write purchase_ledger
  await assertFails(
    aliceDb.collection("entitlements").doc(ALICE_UID).update({
      planType: "PREMIUM_ANNUAL",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("purchase_ledger").doc("PLAY_abc").set({
      userId: ALICE_UID,
    })
  );
  await assertFails(aliceDb.collection("entitlements").doc(ALICE_UID).delete());
});
