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

test("Unauthenticated users cannot read or write profiles or care_notes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("care_notes").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Unauthorized note",
      note: "Should fail",
      category: "GENERAL",
      resolved: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create, update, read, and delete own UserProfile; cross-user & ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid update preserving userId and createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      photoUrl: "https://example.com/avatar.png",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field / role escalation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read/write must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).delete());
});

test("Owner can create, query (orderBy updatedAt desc), toggle resolved, update, and delete care_notes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Mirror Kotlin map payload (no `id` field inside body)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Akku-Sparmodus prüfen",
      note: "Prüfen ob Adaptive Battery aktiv ist.",
      category: "BATTERY",
      resolved: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Query matches CareNotesRepository.observeNotes(): orderBy updatedAt desc
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("care_notes")
      .orderBy("updatedAt", "desc")
      .get()
  );

  // Toggle resolved partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("note_1").update({
      resolved: true,
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Full editable fields partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("note_1").update({
      title: "Akku-Sparmodus geprüft",
      note: "Adaptive Battery aktiv.",
      category: "QA",
      resolved: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Delete succeeds for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("note_1").delete()
  );
});

test("care_notes rejects @DocumentId id key, invalid category, future timestamp, oversized string, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Writing `id` inside body must fail (prevents @DocumentId deserialization crash)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("note_bad_id").set({
      id: "note_bad_id",
      userId: ALICE_UID,
      title: "Bad ID payload",
      note: "Includes id field",
      category: "GENERAL",
      resolved: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Invalid enum category must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("note_bad_cat").set({
      userId: ALICE_UID,
      title: "Bad category",
      note: "Invalid enum",
      category: "INVALID_CAT",
      resolved: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Future timestamp must fail <= request.time bound
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("note_future").set({
      userId: ALICE_UID,
      title: "Future timestamp",
      note: "Clock skew attack",
      category: "GENERAL",
      resolved: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Oversized title > 120 chars must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("note_long").set({
      userId: ALICE_UID,
      title: "x".repeat(121),
      note: "Too long title",
      category: "GENERAL",
      resolved: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid note created by Alice cannot be listed, read, updated, or deleted by Bob
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("alice_only").set({
      userId: ALICE_UID,
      title: "Private check",
      note: "Only Alice",
      category: "PERMISSIONS",
      resolved: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("care_notes").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("alice_only").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("alice_only").update({
      resolved: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("care_notes").doc("alice_only").delete()
  );
});
