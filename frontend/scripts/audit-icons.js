const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SCAN_DIRS = ['pages', 'components', 'styles'];
const SCAN_EXTENSIONS = new Set(['.wxml', '.wxss']);
const WXML_ICON_TEXT_RE = />[^<]*[▼✓→›⭐📍📌⚙ℹ♥♡❤❌✔✖✕][^<]*<\/text>/u;
const WXML_PLUS_ICON_RE = />\s*\+\s*<\/text>/u;
const WXML_EMOJI_RE = />[^<]*\p{Extended_Pictographic}[^<]*<\/text>/u;
const DATA_URI_RE = /data:image\/svg\+xml/i;
const LOCAL_ICON_URL_RE = /url\((['"]?)\/assets\/icons\/lucide\/[^)]+\1\)/i;
const ICON_CLASS_DEF_RE = /\.(icon-[a-z0-9-]+)\b/g;
const SHARED_ICON_STYLES = path.join(ROOT, 'styles', 'icons.wxss');

function collectFiles(currentPath, output) {
  const entries = fs.readdirSync(currentPath, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(currentPath, entry.name);
    if (entry.isDirectory()) {
      collectFiles(fullPath, output);
      continue;
    }
    if (SCAN_EXTENSIONS.has(path.extname(entry.name))) {
      output.push(fullPath);
    }
  }
}

function getLine(content, index) {
  return content.slice(0, index).split('\n').length;
}

function addViolation(violations, type, filePath, line, detail) {
  violations.push({ type, filePath: path.relative(ROOT, filePath), line, detail });
}

const files = [];
for (const dir of SCAN_DIRS) {
  const fullDir = path.join(ROOT, dir);
  if (fs.existsSync(fullDir)) {
    collectFiles(fullDir, files);
  }
}
if (fs.existsSync(path.join(ROOT, 'app.wxss'))) {
  files.push(path.join(ROOT, 'app.wxss'));
}

const definedIconClasses = new Set();
const usedIconClasses = [];
const violations = [];

for (const filePath of files) {
  const content = fs.readFileSync(filePath, 'utf8');
  const ext = path.extname(filePath);

  if (ext === '.wxss') {
    let match;
    while ((match = ICON_CLASS_DEF_RE.exec(content))) {
      definedIconClasses.add(match[1]);
    }

    const localIconUrlIndex = content.search(LOCAL_ICON_URL_RE);
    if (localIconUrlIndex !== -1) {
      addViolation(
        violations,
        'wxss-local-icon-url',
        filePath,
        getLine(content, localIconUrlIndex),
        'Found Lucide local asset URL in WXSS; Mini Program WXSS cannot load local assets directly'
      );
    }

    const dataUriIndex = content.search(DATA_URI_RE);
    if (dataUriIndex !== -1 && filePath !== SHARED_ICON_STYLES) {
      addViolation(
        violations,
        'wxss-data-uri',
        filePath,
        getLine(content, dataUriIndex),
        'Found inline SVG data URI outside the shared icon layer'
      );
    }
    continue;
  }

  const lines = content.split('\n');
  lines.forEach((line, index) => {
    if (!line.includes('<text')) {
      return;
    }
    if (WXML_PLUS_ICON_RE.test(line)) {
      addViolation(violations, 'wxml-icon-glyph', filePath, index + 1, 'Found standalone plus icon text');
      return;
    }
    if (WXML_ICON_TEXT_RE.test(line)) {
      addViolation(violations, 'wxml-icon-glyph', filePath, index + 1, 'Found text-based icon glyph');
      return;
    }
    if (WXML_EMOJI_RE.test(line)) {
      addViolation(violations, 'wxml-emoji-icon', filePath, index + 1, 'Found emoji-based UI icon text');
    }
  });

  let classMatch;
  const classAttrRe = /class\s*=\s*"([^"]+)"/g;
  while ((classMatch = classAttrRe.exec(content))) {
    const classValue = classMatch[1];
    const classTokens = classValue.split(/\s+/).filter(Boolean);
    for (const classToken of classTokens) {
      if (!classToken.startsWith('icon-')) {
        continue;
      }
      usedIconClasses.push({
        filePath,
        className: classToken,
        line: getLine(content, classMatch.index),
      });
    }
  }
}

for (const used of usedIconClasses) {
  if (!definedIconClasses.has(used.className)) {
    addViolation(
      violations,
      'undefined-icon-class',
      used.filePath,
      used.line,
      `Class ${used.className} is used but not defined in WXSS`
    );
  }
}

if (violations.length === 0) {
  console.log('PASS: frontend icon audit found no remaining icon debt.');
  process.exit(0);
}

console.error(`FAIL: frontend icon audit found ${violations.length} issue(s).`);
for (const violation of violations) {
  console.error(`- [${violation.type}] ${violation.filePath}:${violation.line} ${violation.detail}`);
}
process.exit(1);
