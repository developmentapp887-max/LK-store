const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_999";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

async function seedAdmin() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(ADMIN_UID).set({ role: "admin" });
  });
}

async function seedApprovedApp(appId = "app_notes_1", developerId = BOB_UID) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("apps").doc(appId).set({
      title: "Emerald Notes",
      packageName: "com.lkdev.emeraldnotes",
      developerId,
      developerName: "LK Studio",
      category: "Productivity",
      appType: "app",
      summary: "Fast offline-first markdown notebook",
      description: "Write notes cleanly with markdown and sync.",
      apkUrl: "https://cdn.example.com/emeraldnotes-v1.2.apk",
      sha256: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      versionName: "1.2.0",
      versionCode: 12,
      fileSizeBytes: 8450000,
      minSdk: 24,
      status: "approved",
      featured: true,
      verified: true,
      downloadCount: 1200,
      ratingAverage: 4.8,
      ratingCount: 42,
      tags: ["notes", "markdown"],
      createdAt: pastDate(60000),
      updatedAt: pastDate(10000),
    });
  });
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  await seedApprovedApp();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertFails(
    unauthDb.collection("apps").where("status", "==", "approved").get()
  );
  await assertFails(unauthDb.collection("apps").doc("app_notes_1").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      appId: "app_notes_1",
      appTitle: "Emerald Notes",
      reason: "spam",
      details: "Test",
      status: "open",
      createdAt: pastDate(),
    })
  );
});

test("Approved apps query succeeds; pending/draft apps and self-approval are blocked", async () => {
  await seedApprovedApp("app_approved", BOB_UID);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("apps").doc("app_pending").set({
      title: "Unapproved Draft",
      packageName: "com.lkdev.draft",
      developerId: BOB_UID,
      developerName: "LK Studio",
      category: "Tools",
      appType: "app",
      description: "Pending review app",
      apkUrl: "https://cdn.example.com/draft.apk",
      versionName: "0.1.0",
      versionCode: 1,
      fileSizeBytes: 1024,
      status: "pending",
      createdAt: pastDate(20000),
      updatedAt: pastDate(20000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("apps").where("status", "==", "approved").get()
  );
  await assertSucceeds(aliceDb.collection("apps").doc("app_approved").get());

  await assertFails(aliceDb.collection("apps").doc("app_pending").get());
  await assertFails(aliceDb.collection("apps").get());

  // Non-admin cannot self-approve
  await assertFails(
    aliceDb.collection("apps").doc("app_forged").set({
      title: "Forged App",
      packageName: "com.forged.app",
      developerId: ALICE_UID,
      developerName: "Alice",
      category: "Tools",
      appType: "app",
      description: "Self approved attempt",
      apkUrl: "https://cdn.example.com/forged.apk",
      versionName: "1.0.0",
      versionCode: 1,
      fileSizeBytes: 5000,
      status: "approved",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Developer publishing workflow: draft -> pending -> admin approval -> releases & audit log", async () => {
  await seedAdmin();
  const devDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(20000);

  // Developer registers profile (cannot self-verify)
  await assertSucceeds(
    devDb.collection("developers").doc(BOB_UID).set({
      developerId: BOB_UID,
      name: "Bob Studio",
      organizationName: "Bob Labs",
      status: "active",
      verified: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    devDb.collection("developers").doc(BOB_UID).update({
      verified: true,
      updatedAt: pastDate(1000),
    })
  );

  // Developer creates draft app & submits for review
  await assertSucceeds(
    devDb.collection("apps").doc("bob_app_1").set({
      title: "Bob Utility",
      packageName: "com.bob.utility",
      developerId: BOB_UID,
      developerName: "Bob Studio",
      category: "Tools",
      appType: "app",
      description: "Clean utility app",
      apkUrl: "https://cdn.example.com/bob-v1.apk",
      versionName: "1.0.0",
      versionCode: 1,
      fileSizeBytes: 4200000,
      status: "draft",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    devDb.collection("apps").doc("bob_app_1").update({
      status: "pending",
      updatedAt: pastDate(2000),
    })
  );

  // Developer cannot approve their own submission
  await assertFails(
    devDb.collection("apps").doc("bob_app_1").update({
      status: "approved",
      updatedAt: pastDate(1000),
    })
  );

  // Developer adds a pending release
  await assertSucceeds(
    devDb.collection("apps").doc("bob_app_1").collection("releases").doc("rel_1").set({
      appId: "bob_app_1",
      developerId: BOB_UID,
      versionName: "1.0.0",
      versionCode: 1,
      apkUrl: "https://cdn.example.com/bob-v1.apk",
      fileSizeBytes: 4200000,
      track: "production",
      status: "pending",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Admin approves app and logs publishing history
  await assertSucceeds(
    adminDb.collection("apps").doc("bob_app_1").update({
      status: "approved",
      verified: true,
      reviewedBy: ADMIN_UID,
      reviewedAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    adminDb.collection("publishing_history").doc("evt_1").set({
      appId: "bob_app_1",
      appTitle: "Bob Utility",
      developerId: BOB_UID,
      actorId: ADMIN_UID,
      action: "approved",
      versionName: "1.0.0",
      createdAt: pastDate(500),
    })
  );

  // Non-owner cannot read Bob's publishing history or spoof admin approval event
  await assertFails(aliceDb.collection("publishing_history").doc("evt_1").get());
  await assertFails(
    devDb.collection("publishing_history").doc("evt_2").set({
      appId: "bob_app_1",
      appTitle: "Bob Utility",
      developerId: BOB_UID,
      actorId: BOB_UID,
      action: "approved",
      createdAt: pastDate(),
    })
  );
});

test("User profile, wishlist, library, download history, and notifications enforce owner isolation", async () => {
  await seedApprovedApp("app_notes_1", BOB_UID);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(20000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Dev",
      photoUrl: "https://example.com/alice.png",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice K.",
      bio: "Android enthusiast",
      updatedAt: pastDate(1000),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("wishlist")
      .doc("app_notes_1")
      .set({
        userId: ALICE_UID,
        appId: "app_notes_1",
        appTitle: "Emerald Notes",
        developerName: "LK Studio",
        category: "Productivity",
        createdAt: pastDate(),
      })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wishlist").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("wishlist").get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("download_history")
      .doc("hist_1")
      .set({
        userId: ALICE_UID,
        appId: "app_notes_1",
        appTitle: "Emerald Notes",
        developerName: "LK Studio",
        versionName: "1.2.0",
        versionCode: 12,
        fileSizeBytes: 8450000,
        verified: true,
        downloadedAt: pastDate(),
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("library")
      .doc("app_notes_1")
      .set({
        userId: ALICE_UID,
        appId: "app_notes_1",
        appTitle: "Emerald Notes",
        packageName: "com.lkdev.emeraldnotes",
        developerName: "LK Studio",
        versionName: "1.2.0",
        versionCode: 12,
        fileSizeBytes: 8450000,
        apkUrl: "https://cdn.example.com/emeraldnotes-v1.2.apk",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("notifications")
      .doc("notif_1")
      .set({
        userId: ALICE_UID,
        title: "APK Download Started: Emerald Notes",
        message: "Version 1.2.0 queued via DownloadManager.",
        type: "download",
        appId: "app_notes_1",
        read: false,
        createdAt: created,
        updatedAt: created,
      })
  );
});

test("Reviews, reports, and support tickets enforce ownership and admin moderation", async () => {
  await seedAdmin();
  await seedApprovedApp("app_notes_1", BOB_UID);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const created = pastDate(15000);

  await assertSucceeds(
    aliceDb
      .collection("apps")
      .doc("app_notes_1")
      .collection("reviews")
      .doc(ALICE_UID)
      .set({
        appId: "app_notes_1",
        userId: ALICE_UID,
        userName: "Alice",
        rating: 5,
        comment: "Clean UI and verified APK!",
        versionName: "1.2.0",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      appId: "app_notes_1",
      appTitle: "Emerald Notes",
      reason: "broken_link",
      details: "APK link returned 404.",
      status: "open",
      createdAt: pastDate(),
    })
  );

  // Non-admin cannot resolve report; admin can
  await assertFails(
    bobDb.collection("reports").doc("rep_1").update({ status: "dismissed" })
  );
  await assertSucceeds(
    adminDb.collection("reports").doc("rep_1").update({
      status: "reviewed",
      resolutionNote: "Verified and resolved.",
    })
  );

  await assertSucceeds(
    aliceDb.collection("support_tickets").doc("tick_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      subject: "Question about checksum",
      category: "security",
      message: "How do I verify SHA-256?",
      status: "open",
      createdAt: pastDate(),
    })
  );
  await assertFails(bobDb.collection("support_tickets").doc("tick_1").get());
  await assertSucceeds(
    adminDb.collection("support_tickets").doc("tick_1").update({ status: "resolved" })
  );
});
