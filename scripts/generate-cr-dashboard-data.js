const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const sourcePath = path.join(root, "docs", "PACKAGE_ARCHITECTURE.md");
const outputPath = path.join(root, "docs", "cr-dashboard-data.js");

function normalizeKind(status) {
  const raw = status || "";
  const value = raw.toLowerCase();
  if (raw.startsWith("Done")) return "done";
  if (value.includes("deprecated")) return "deprecated";
  if (
    value.includes("reopened") ||
    value.includes("reopen") ||
    value.includes("重开") ||
    value.includes("重新打开") ||
    value.includes("打回")
  ) return "reopened";
  if (value.includes("ready") || value.includes("unclaimed")) return "ready";
  if (value.includes("partial")) return "partial";
  if (value.includes("review") || value.includes("pending")) return "review";
  return "open";
}

function verificationFromStatus(status) {
  const raw = status || "";
  const value = raw.toLowerCase();
  if (raw.startsWith("Done")) return "已验证/已关闭";
  if (
    value.includes("reopened") ||
    value.includes("reopen") ||
    value.includes("重开") ||
    value.includes("重新打开") ||
    value.includes("打回")
  ) return "已重开，需重新验证";
  if (
    value.includes("fresh runtime pending") ||
    value.includes("needs fresh") ||
    value.includes("runtime pending")
  ) return "缺 fresh runtime";
  if (value.includes("ready") || value.includes("unclaimed")) return "未实现/未认领";
  if (value.includes("partial")) return "部分验证";
  return "需复核";
}

function inferDomain(summary) {
  const text = summary || "";
  if (/修罗|Xiuluo|XIULUO|xiuluo/.test(text)) return "修罗";
  if (/五倍|Wubei|WUBEI|wubei/.test(text)) return "五倍";
  if (/五环|FiveRing|wuhuan|Wuhuan/.test(text)) return "五环";
  if (/window|Window|窗口|Runner|runner/.test(text)) return "窗口/Runner";
  if (/navigation|Navigation|导航|pathing|PATHING/.test(text)) return "导航";
  if (/Dialog|dialog|对话|broadcast/.test(text)) return "Dialog";
  return "通用";
}

function parseMarkdownTable(markdown) {
  const parsed = [];
  for (const line of markdown.split(/\r?\n/)) {
    if (!/^\| (?:CR|G)\d+\s*\|/.test(line)) continue;
    const parts = line.trim().replace(/^\|/, "").replace(/\|$/, "").split("|").map(part => part.trim());
    if (parts.length < 5) continue;
    const [id, owner, status, files, summary] = parts;
    if (!/^(?:CR|G)\d+$/.test(id)) continue;
    const number = Number(id.replace(/^(?:CR|G)/, ""));
    parsed.push({
      id,
      number,
      owner,
      status,
      kind: normalizeKind(status),
      domain: inferDomain(summary),
      files,
      summary,
      verification: verificationFromStatus(status)
    });
  }
  return parsed;
}

const markdown = fs.readFileSync(sourcePath, "utf8");
const data = parseMarkdownTable(markdown);
if (!data.length) {
  throw new Error(`No CR rows parsed from ${sourcePath}`);
}

fs.writeFileSync(outputPath, `window.CR_DASHBOARD_DATA = ${JSON.stringify(data, null, 2)};\n`, "utf8");
console.log(`Generated ${data.length} CR rows -> ${path.relative(root, outputPath)}`);
