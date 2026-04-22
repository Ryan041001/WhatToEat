const fs = require('fs');
const path = require('path');
const assert = require('assert');

function readSpinTemplate() {
  const templatePath = path.join(__dirname, 'spin.wxml');
  return fs.readFileSync(templatePath, 'utf8');
}

function testResultCardStaysBelowWheelActions() {
  const template = readSpinTemplate();
  const resultCardIndex = template.indexOf('class="result-card');
  const actionRowIndex = template.indexOf('class="action-row"');

  assert.notStrictEqual(resultCardIndex, -1, 'result card should exist in spin template');
  assert.notStrictEqual(actionRowIndex, -1, 'action row should exist in spin template');
  assert(
    resultCardIndex > actionRowIndex,
    'result card should render after the wheel action row so revealing it does not push the wheel downward'
  );
}

function run() {
  testResultCardStaysBelowWheelActions();
  process.stdout.write('spin-layout test passed\n');
}

if (require.main === module) {
  run();
}

module.exports = {
  testResultCardStaysBelowWheelActions,
  run
};
