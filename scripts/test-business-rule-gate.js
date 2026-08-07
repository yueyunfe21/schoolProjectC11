const assert = require("assert");
const fs = require("fs");
const os = require("os");
const path = require("path");
const { run } = require("./check-business-rule-gate");

function write(root, relativePath, content) {
  const target = path.join(root, relativePath);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, content, "utf8");
}

function fixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "dhxy-business-rule-gate-"));
  write(root, "docs/logic.md", "# [BR-TEST-001] Test rule\n");
  write(root, "src/Producer.java", "producer\n");
  write(root, "src/ContractTest.java", "contract\n");
  write(root, "docs/business-rules.json", JSON.stringify({
    schemaVersion: 1,
    rules: [{
      id: "BR-TEST-001",
      title: "Test rule",
      source: "docs/logic.md",
      heading: "[BR-TEST-001] Test rule",
      crossBoundary: true,
      status: "active"
    }]
  }, null, 2));
  return root;
}

function validManifest() {
  return {
    schemaVersion: 1,
    card: "G999",
    changeType: "business",
    status: "APPROVED_IMPLEMENTATION",
    baseline: {
      clientBranch: "test",
      clientCommit: "0123456789abcdef",
      pushedReference: "origin/test"
    },
    rules: ["BR-TEST-001"],
    affectedPaths: ["src/Producer.java", "src/ContractTest.java"],
    businessDifference: { kind: "NONE", summary: "equivalent" },
    connectivity: [{
      rule: "BR-TEST-001",
      producer: "producer",
      transport: "transport",
      consumer: "consumer",
      ordering: "producer before consumer",
      contracts: [{
        path: "src/ContractTest.java",
        command: "test command",
        proves: ["producer reachable"]
      }]
    }],
    verification: []
  };
}

function check(root, manifest) {
  write(root, "docs/rule-traceability/G999.json", JSON.stringify(manifest, null, 2));
  return run(root, { card: "G999" });
}

const root = fixture();
try {
  assert.deepStrictEqual(check(root, validManifest()), []);

  const missingProducer = validManifest();
  missingProducer.connectivity[0].producer = "";
  assert(check(root, missingProducer).some(error => error.includes("缺少 producer")));

  const unresolved = validManifest();
  unresolved.status = "BLOCKED_USER_DECISION";
  unresolved.businessDifference = { kind: "UNRESOLVED", conflictSummary: "conflict" };
  assert(check(root, unresolved).some(error => error.includes("禁止实施")));

  const agentApproved = validManifest();
  agentApproved.businessDifference = { kind: "USER_APPROVED", conflictSummary: "conflict" };
  assert(check(root, agentApproved).some(error => error.includes("缺少 userDecision")));

  const userApproved = validManifest();
  userApproved.businessDifference = {
    kind: "USER_APPROVED",
    conflictSummary: "old and new differ",
    userDecision: "user selected new behavior",
    decisionRecordedAt: "2026-08-04",
    cardReference: "G999 decision section"
  };
  assert.deepStrictEqual(check(root, userApproved), []);

  console.log("BUSINESS RULE GATE SELF-TEST PASSED (5 cases)");
} finally {
  fs.rmSync(root, { recursive: true, force: true });
}
