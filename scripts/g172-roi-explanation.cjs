// User-requested ROI coordinate illustration only; no OCR, matching or game input.
const fs = require('fs');
const path = require('path');
const sharp = require(process.argv[2]);
const root = path.resolve(__dirname, '..');
const input = path.join(root, 'images/temp/npc-arrival-frames/arrival-07-hwnd-161A00FA-frame1267395517571600-gen1267395517571600-replacement.png');
const output = path.join(root, 'images/debug/g172-roi-explanation');
async function main() {
  const bytes = fs.readFileSync(input);
  const info = await sharp(bytes).metadata();
  if (info.width !== 1024 || info.height !== 768) throw new Error('Unexpected coordinate canvas');
  fs.mkdirSync(output, { recursive: true });
  // Border is drawn just inside [0,0,1024,768] so none of its stroke is clipped.
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1064" height="886" viewBox="0 0 1064 886">
<rect width="1064" height="886" fill="#142027"/>
<text x="20" y="29" font-family="Microsoft YaHei, sans-serif" font-size="20" fill="#ffffff">当前数据接回旧推荐逻辑：无有效学习 ROI，使用全窗口</text>
<text x="20" y="55" font-family="Microsoft YaHei, sans-serif" font-size="16" fill="#66efb3">绿框：(0,0) → (1024,768)　｜　人物：宝象国 (80,75)</text>
<g transform="translate(20,72)"><image width="1024" height="768" xlink:href="data:image/png;base64,${bytes.toString('base64')}"/>
<rect x="2" y="2" width="1020" height="764" fill="none" stroke="#21ff8f" stroke-width="4"/></g>
<text x="20" y="868" font-family="Microsoft YaHei, sans-serif" font-size="15" fill="#ffffff">原图：01:03:51 实际识别帧。绿框是按现存数据推定的回接结果，不是当时已执行的学习 ROI。</text>
</svg>`;
  fs.writeFileSync(path.join(output, 'restored-roi.svg'), svg);
  await sharp(Buffer.from(svg)).png().toFile(path.join(output, 'restored-roi.png'));
  console.log(path.join(output, 'restored-roi.png'));
  // Geometry copied from SmartClickRecognizer.DEFAULT_OCR_MASKS, not a matching replay.
  const masks = [[0,0,258,200],[0,0,1024,54],[768,58,252,102],[4,735,702,33],[710,700,314,68]];
  const maskedSvg = `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1064" height="900">
<rect width="1064" height="900" fill="#142027"/>
<text x="20" y="29" font-family="Microsoft YaHei, sans-serif" font-size="20" fill="white">全窗截图 ≠ 全窗识别：通用 UI mask 仍然存在</text>
<text x="20" y="55" font-family="Microsoft YaHei, sans-serif" font-size="16" fill="#66efb3">绿色边界＝截图 ROI　白色块＝现有代码排除、不送入黄字识别的区域</text>
<g transform="translate(20,72)"><image width="1024" height="768" xlink:href="data:image/png;base64,${bytes.toString('base64')}"/>
${masks.map(([x,y,w,h])=>`<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="white"/>`).join('')}
<rect x="2" y="2" width="1020" height="764" fill="none" stroke="#21ff8f" stroke-width="4"/></g>
<text x="20" y="863" font-family="Microsoft YaHei, sans-serif" font-size="15" fill="white">本图仅展示现有通用 mask 的坐标范围，不是当时保存的洗字结果。</text>
<text x="20" y="887" font-family="Microsoft YaHei, sans-serif" font-size="15" fill="white">尚未叠加降魔侍卫错误的两个固定保留框，也未执行颜色筛选或 OCR。原截图不改写。</text>
</svg>`;
  fs.writeFileSync(path.join(output, 'existing-ui-mask.svg'), maskedSvg);
  await sharp(Buffer.from(maskedSvg)).png().toFile(path.join(output, 'existing-ui-mask.png'));
  console.log(path.join(output, 'existing-ui-mask.png'));
}
main().catch(error => { console.error(error); process.exitCode = 1; });
