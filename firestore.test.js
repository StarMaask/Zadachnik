const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

function validTaskPayload(userId, overrides = {}) {
  const now = new Date();
  return {
    userId,
    title: "Проверка армирования плиты перекрытия",
    description: "Выполнить фотофиксацию узлов вязальной проволоки и защитного слоя",
    creatorId: "emp_itr_1",
    creatorName: "Соколов А.В.",
    assigneeId: "emp_worker_1",
    assigneeName: "Морозов И.П.",
    groupId: "grp_monolith",
    groupName: "Бригада монолитчиков №1",
    siteId: "site_north",
    priority: "HIGH",
    status: "NEW",
    dueDateEpochDay: 20768,
    originalDueDateEpochDay: 20768,
    deadlineTime: "17:30",
    carryoverCount: 0,
    requiresPhotoReport: true,
    minPhotoCount: 2,
    requiresApproval: true,
    recurrenceRule: "NONE",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user: cannot read or create tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tasks").get());
  await assertFails(
    unauthDb.collection("tasks").doc("task_1").set(validTaskPayload(ALICE_UID))
  );
});

test("Authenticated user: can create and query own tasks with matching filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tasks").doc("task_alice_1").set(validTaskPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("tasks").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: can read shared site tasks and update status, but cannot spoof creator userId", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("task_bob_1").set(validTaskPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tasks").doc("task_bob_1").get());
  await assertSucceeds(aliceDb.collection("tasks").get());
  const sampleBase64Photo = "ph_1|/data/user/0/app/files/cam_1.jpg#b64=" + "A".repeat(32000) + "|emp_1|Плиткин Д.|13.11 16:16|Участок №1|Фотофиксация СМР #1";
  await assertSucceeds(
    aliceDb.collection("tasks").doc("task_bob_1").update({
      photoItems: [sampleBase64Photo],
      status: "ON_REVIEW",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    aliceDb.collection("tasks").doc("task_bob_1").update({
      userId: ALICE_UID,
      updatedAt: new Date(),
    })
  );
});

test("Shadow Update & Terminal State Lock: rejects ghost fields and archived task updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tasks").doc("task_alice_2").set(validTaskPayload(ALICE_UID))
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("tasks").doc("task_alice_2").update({
      status: "IN_PROGRESS",
      isSuperAdmin: true,
      updatedAt: new Date(),
    })
  );

  // Move to ARCHIVED
  await assertSucceeds(
    aliceDb.collection("tasks").doc("task_alice_2").update({
      status: "ARCHIVED",
      updatedAt: new Date(),
    })
  );

  // Reject any update after ARCHIVED terminal state
  await assertFails(
    aliceDb.collection("tasks").doc("task_alice_2").update({
      title: "Попытка изменить архивную задачу",
      updatedAt: new Date(),
    })
  );
});

test("Audit log: allows create by owner, blocks update and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("audit_logs").doc("log_1").set({
      userId: ALICE_UID,
      actorId: "emp_head_1",
      actorName: "Ковалёв Д.С.",
      actorRole: "HEAD",
      actionType: "APPROVE_REPORT",
      targetType: "TASK",
      targetId: "task_1",
      details: "Отчёт подтверждён руководителем участка",
      createdAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("audit_logs").doc("log_1").update({
      details: "Подделка журнала аудита",
    })
  );
  await assertFails(aliceDb.collection("audit_logs").doc("log_1").delete());
});
