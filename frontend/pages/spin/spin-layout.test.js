const fs = require('fs');
const path = require('path');

function readSpinTemplate() {
  const templatePath = path.join(__dirname, 'spin.wxml');
  return fs.readFileSync(templatePath, 'utf8');
}

function testResultCardStaysBelowWheelActions() {
  const template = readSpinTemplate();
  const resultCardIndex = template.indexOf('class="result-card');
  const actionRowIndex = template.indexOf('class="action-row"');

  expect(resultCardIndex).not.toBe(-1);
  expect(actionRowIndex).not.toBe(-1);
  expect(
    resultCardIndex > actionRowIndex,
  ).toBe(true);
}

test('result card should stay below action row in spin template', () => {
  testResultCardStaysBelowWheelActions();
});

module.exports = {
  testResultCardStaysBelowWheelActions
};
