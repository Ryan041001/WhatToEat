const fs = require('fs');
const path = require('path');

test('spin template renders result card after action row', () => {
  const templatePath = path.join(__dirname, '..', 'pages', 'spin', 'spin.wxml');
  const template = fs.readFileSync(templatePath, 'utf8');
  const resultCardIndex = template.indexOf('class="result-card');
  const actionRowIndex = template.indexOf('class="action-row"');

  expect(resultCardIndex).toBeGreaterThan(-1);
  expect(actionRowIndex).toBeGreaterThan(-1);
  expect(resultCardIndex).toBeGreaterThan(actionRowIndex);
});