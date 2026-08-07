const fs = require("fs");
const path = require("path");

const ALLOWED_STATUSES = new Set([
  "DRAFT",
  "BLOCKED_USER_DECISION",
  "APPROVED_IMPLEMENTATION",
  "REVIEW_PASSED"
]);
const ALLOWED_CHANGE_TYPES = new Set(["business", "governance", "docs"]);
const ALLOWED_DIFFERENCE_KINDS = new Set(["NONE", "UNRESOLVED", "USER_APPROVED"]);

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function nonBlank(value) {
  return typeof value === "string" && value.trim().length > 0;
}

function resolveRepoPath(root, repositories, entry) {
  if (typeof entry === "string") {
    return path.resolve(root, entry);
  }
  if (!entry || typeof entry !== "object" || !nonBlank(entry.path)) {
    return null;
  }
  const repoName = entry.repo || "client";
  const repoBase = repositories[repoName];
  return repoBase ? path.resolve(repoBase, entry.path) : null;
}

function repositoryRoots(root, manifest) {
  const roots = { client: root };
  for (const [name, relativePath] of Object.entries(manifest.repositories || {})) {
    if (nonBlank(name) && nonBlank(relativePath)) {
      roots[name] = path.resolve(root, relativePath);
    }
  }
  return roots;
}

function validateRegistry(root, registry) {
  const errors = [];
  if (registry.schemaVersion !== 1 || !Array.isArray(registry.rules)) {
    return ["docs/business-rules.json 必须使用 schemaVersion=1 并包含 rules 数组"];
  }

  const seen = new Set();
  for (const rule of registry.rules) {
    if (!rule || !/^BR-[A-Z0-9]+-[0-9]{3}$/.test(rule.id || "")) {
      errors.push(`非法业务规则 ID: ${rule && rule.id}`);
      continue;
    }
    if (seen.has(rule.id)) {
      errors.push(`重复业务规则 ID: ${rule.id}`);
    }
    seen.add(rule.id);
    if (!nonBlank(rule.title) || !nonBlank(rule.source) || !nonBlank(rule.heading)) {
      errors.push(`${rule.id} 缺少 title/source/heading`);
      continue;
    }
    const sourcePath = path.resolve(root, rule.source);
    if (!fs.existsSync(sourcePath)) {
      errors.push(`${rule.id} 的权威业务文档不存在: ${rule.source}`);
      continue;
    }
    const source = fs.readFileSync(sourcePath, "utf8");
    if (!source.includes(rule.heading)) {
      errors.push(`${rule.id} 的权威标题未在 ${rule.source} 中找到: ${rule.heading}`);
    }
  }
  return errors;
}

function validateManifest(root, manifestPath, manifest, registry) {
  const errors = [];
  const cardFromFile = path.basename(manifestPath, ".json");
  if (manifest.schemaVersion !== 1) {
    errors.push(`${cardFromFile}: schemaVersion 必须为 1`);
  }
  if (!/^G[0-9]+[A-Z]?$/.test(manifest.card || "") || manifest.card !== cardFromFile) {
    errors.push(`${cardFromFile}: card 必须与文件名一致并使用 G 编号`);
  }
  if (!ALLOWED_CHANGE_TYPES.has(manifest.changeType)) {
    errors.push(`${cardFromFile}: 非法 changeType ${manifest.changeType}`);
  }
  if (!ALLOWED_STATUSES.has(manifest.status)) {
    errors.push(`${cardFromFile}: 非法 status ${manifest.status}`);
  }
  if (!manifest.baseline || !nonBlank(manifest.baseline.clientBranch)
      || !nonBlank(manifest.baseline.clientCommit) || !nonBlank(manifest.baseline.pushedReference)) {
    errors.push(`${cardFromFile}: baseline 必须记录 clientBranch/clientCommit/pushedReference`);
  }

  const ruleMap = new Map((registry.rules || []).map(rule => [rule.id, rule]));
  if (!Array.isArray(manifest.rules)) {
    errors.push(`${cardFromFile}: rules 必须是数组`);
  } else {
    if (manifest.changeType === "business" && manifest.rules.length === 0) {
      errors.push(`${cardFromFile}: business 改动至少关联一个业务规则 ID`);
    }
    for (const ruleId of manifest.rules) {
      if (!ruleMap.has(ruleId)) {
        errors.push(`${cardFromFile}: 未登记的业务规则 ${ruleId}`);
      }
    }
  }

  const repos = repositoryRoots(root, manifest);
  if (!Array.isArray(manifest.affectedPaths) || manifest.affectedPaths.length === 0) {
    errors.push(`${cardFromFile}: affectedPaths 不能为空`);
  } else {
    for (const entry of manifest.affectedPaths) {
      const resolved = resolveRepoPath(root, repos, entry);
      if (!resolved || !fs.existsSync(resolved)) {
        errors.push(`${cardFromFile}: affected path 不存在或 repo 未声明: ${JSON.stringify(entry)}`);
      }
    }
  }

  const difference = manifest.businessDifference || {};
  if (!ALLOWED_DIFFERENCE_KINDS.has(difference.kind)) {
    errors.push(`${cardFromFile}: businessDifference.kind 必须为 NONE/UNRESOLVED/USER_APPROVED`);
  } else if (difference.kind === "UNRESOLVED") {
    if (manifest.status !== "BLOCKED_USER_DECISION") {
      errors.push(`${cardFromFile}: 未决冲突必须使用 BLOCKED_USER_DECISION`);
    }
    errors.push(`${cardFromFile}: 存在未获用户裁决的业务冲突，禁止实施`);
  } else if (difference.kind === "USER_APPROVED") {
    for (const field of ["conflictSummary", "userDecision", "decisionRecordedAt", "cardReference"]) {
      if (!nonBlank(difference[field])) {
        errors.push(`${cardFromFile}: USER_APPROVED 冲突缺少 ${field}`);
      }
    }
  } else if (manifest.status === "BLOCKED_USER_DECISION") {
    errors.push(`${cardFromFile}: BLOCKED_USER_DECISION 必须说明 UNRESOLVED 冲突`);
  }

  const connectivity = Array.isArray(manifest.connectivity) ? manifest.connectivity : [];
  for (const ruleId of manifest.rules || []) {
    const rule = ruleMap.get(ruleId);
    if (!rule || !rule.crossBoundary) {
      continue;
    }
    const links = connectivity.filter(item => item && item.rule === ruleId);
    if (links.length !== 1) {
      errors.push(`${cardFromFile}: 跨边界规则 ${ruleId} 必须且只能有一条 connectivity 记录`);
      continue;
    }
    const link = links[0];
    for (const field of ["producer", "transport", "consumer", "ordering"]) {
      if (!nonBlank(link[field])) {
        errors.push(`${cardFromFile}: ${ruleId} connectivity 缺少 ${field}`);
      }
    }
    if (!Array.isArray(link.contracts) || link.contracts.length === 0) {
      errors.push(`${cardFromFile}: ${ruleId} 缺少连通性/顺序合同`);
      continue;
    }
    for (const contract of link.contracts) {
      const contractPath = resolveRepoPath(root, repos, contract && (contract.repo
        ? { repo: contract.repo, path: contract.path }
        : contract && contract.path));
      if (!contractPath || !fs.existsSync(contractPath)) {
        errors.push(`${cardFromFile}: 连通性合同文件不存在: ${contract && contract.path}`);
      }
      if (!contract || !nonBlank(contract.command) || !Array.isArray(contract.proves)
          || contract.proves.length === 0) {
        errors.push(`${cardFromFile}: 连通性合同必须包含 command 和 proves`);
      }
    }
  }

  if (manifest.status === "REVIEW_PASSED") {
    if (!Array.isArray(manifest.verification) || manifest.verification.length === 0) {
      errors.push(`${cardFromFile}: REVIEW_PASSED 必须包含 verification`);
    } else if (manifest.verification.some(item => !item || item.result !== "PASS"
        || !nonBlank(item.name) || !nonBlank(item.evidence))) {
      errors.push(`${cardFromFile}: REVIEW_PASSED 的每项 verification 都必须有 name/result=PASS/evidence`);
    }
  }
  return errors;
}

function run(root, options) {
  const registryPath = path.join(root, "docs", "business-rules.json");
  if (!fs.existsSync(registryPath)) {
    return ["缺少 docs/business-rules.json"];
  }
  const registry = readJson(registryPath);
  const errors = validateRegistry(root, registry);
  const traceabilityDir = path.join(root, "docs", "rule-traceability");
  let manifestPaths = [];
  if (options.all) {
    manifestPaths = fs.existsSync(traceabilityDir)
      ? fs.readdirSync(traceabilityDir)
        .filter(name => /^G[0-9]+[A-Z]?\.json$/.test(name))
        .map(name => path.join(traceabilityDir, name))
      : [];
  } else if (options.card) {
    manifestPaths = [path.join(traceabilityDir, `${options.card}.json`)];
  }
  if (manifestPaths.length === 0) {
    errors.push("没有找到需要审核的 G 卡追踪文件");
  }
  for (const manifestPath of manifestPaths) {
    if (!fs.existsSync(manifestPath)) {
      errors.push(`缺少追踪文件: ${path.relative(root, manifestPath)}`);
      continue;
    }
    errors.push(...validateManifest(root, manifestPath, readJson(manifestPath), registry));
  }
  return errors;
}

function parseArgs(argv) {
  const cardIndex = argv.indexOf("--card");
  if (argv.includes("--all")) {
    return { all: true };
  }
  if (cardIndex >= 0 && nonBlank(argv[cardIndex + 1])) {
    return { card: argv[cardIndex + 1] };
  }
  return null;
}

if (require.main === module) {
  const options = parseArgs(process.argv.slice(2));
  if (!options) {
    console.error("Usage: node scripts/check-business-rule-gate.js --card G016 | --all");
    process.exit(2);
  }
  const root = path.resolve(__dirname, "..");
  const errors = run(root, options);
  if (errors.length > 0) {
    console.error("BUSINESS RULE GATE FAILED");
    errors.forEach(error => console.error(`- ${error}`));
    process.exit(1);
  }
  console.log(`BUSINESS RULE GATE PASSED (${options.card || "all cards"})`);
}

module.exports = {
  parseArgs,
  run,
  validateManifest,
  validateRegistry
};
