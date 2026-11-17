const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-shotdesk-f3de44fd-4106-48ad-8438-57b0c00a0eff";
const ALICE_UID = "alice_director_1";
const BOB_UID = "bob_director_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProject(uid, title = "Short Film") {
  const now = new Date();
  return {
    userId: uid,
    title,
    status: "in_progress",
    createdAtMillis: 1700000000000,
    updatedAtMillis: 1700000000000,
    createdAt: now,
    updatedAt: now,
  };
}

function validShot(uid, projectId, shotNumber = "Scene 1 - Shot 1") {
  const now = new Date();
  return {
    userId: uid,
    projectId,
    shotNumber,
    sortOrder: 0,
    shotType: "Wide Shot",
    dialogueScript: "Interior coffee shop, morning light.",
    visualDescription: "Slow dolly push toward table.",
    vfxNotes: "Subtle film grain",
    sfxMusicNotes: "Warm acoustic cue",
    directorsNotes: "Use 35mm lens",
    durationSeconds: 12,
    isCompleted: false,
    createdAt: now,
    updatedAt: now,
  };
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

test("Unauthenticated user cannot read or write projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(validProject(ALICE_UID))
  );
});

test("Owner can create, read, update, and list their own projects", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const projRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");

  await assertSucceeds(projRef.set(validProject(ALICE_UID)));
  await assertSucceeds(projRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertSucceeds(
    projRef.update({
      status: "completed",
      updatedAtMillis: 1700000100000,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user access to another user's project and shots is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(validProject(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").collection("shots").doc("shot_1").set(validShot(ALICE_UID, "proj_1"))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").collection("shots").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").collection("shots").doc("shot_2").set(validShot(BOB_UID, "proj_1"))
  );
});

test("Shadow field and orphaned shot creation are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);

  // Orphaned shot without parent project fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("missing_proj").collection("shots").doc("shot_1").set(validShot(ALICE_UID, "missing_proj"))
  );

  // Ghost field in project fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_ghost").set({
      ...validProject(ALICE_UID),
      isAdmin: true,
    })
  );
});

test("Owner can toggle shot completion status and revert for reshoot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const projRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");
  const shotRef = projRef.collection("shots").doc("shot_1");

  await assertSucceeds(projRef.set(validProject(ALICE_UID)));
  await assertSucceeds(shotRef.set(validShot(ALICE_UID, "proj_1")));
  await assertSucceeds(shotRef.update({ isCompleted: true, updatedAt: new Date() }));
  await assertSucceeds(shotRef.update({ isCompleted: false, updatedAt: new Date() }));
});
