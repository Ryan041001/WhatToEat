const FULL_TURN = Math.PI * 2;
const POINTER_ANGLE = -Math.PI / 2;
const SEGMENT_COLORS = [
  '#FF6B6B',
  '#FF8E53',
  '#FFB84D',
  '#FFC857',
  '#F4A261',
  '#F7B267',
  '#E76F51',
  '#FF9E2A'
];

function normalizeAngle(angle) {
  const normalized = angle % FULL_TURN;
  return normalized < 0 ? normalized + FULL_TURN : normalized;
}

function getAnglePerSlice(count) {
  if (!count) {
    return 0;
  }

  return FULL_TURN / count;
}

function getSliceStartAngle(count, index, rotation) {
  const anglePerSlice = getAnglePerSlice(count);
  const baseStartAngle = POINTER_ANGLE - anglePerSlice / 2;
  return baseStartAngle + anglePerSlice * index + rotation;
}

function getSliceCenterAngle(count, index, rotation) {
  const anglePerSlice = getAnglePerSlice(count);
  return getSliceStartAngle(count, index, rotation) + anglePerSlice / 2;
}

function createSpinPlan(options) {
  const { count, winnerIndex, currentRotation = 0, extraTurns = 5 } = options || {};
  const anglePerSlice = getAnglePerSlice(count);

  if (!count || winnerIndex < 0 || winnerIndex >= count) {
    throw new Error('invalid spin plan input');
  }

  const startRotation = normalizeAngle(currentRotation);
  const targetRotation = normalizeAngle(-winnerIndex * anglePerSlice);
  const forwardOffset = normalizeAngle(targetRotation - startRotation);
  const totalRotation = extraTurns * FULL_TURN + forwardOffset;
  const finalRotation = startRotation + totalRotation;

  return {
    anglePerSlice,
    winnerIndex,
    startRotation,
    targetRotation,
    totalRotation,
    finalRotation,
    normalizedFinalRotation: normalizeAngle(finalRotation)
  };
}

function getSegmentColor(index) {
  return SEGMENT_COLORS[index % SEGMENT_COLORS.length];
}

function formatRestaurantLabel(name, count) {
  const value = typeof name === 'string' ? name.trim() : '';
  if (!value) {
    return '';
  }

  const maxLength = count <= 6 ? 6 : count <= 9 ? 5 : 4;
  return value.length > maxLength ? `${value.slice(0, maxLength)}…` : value;
}

function attachDisplayNames(restaurants) {
  const list = Array.isArray(restaurants) ? restaurants : [];
  return list.map((restaurant) => {
    const fullName = restaurant.name || '';
    const displayName = formatRestaurantLabel(fullName, list.length);

    return {
      ...restaurant,
      fullName,
      displayName,
      showFullName: displayName !== fullName
    };
  });
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function assertAlmostEqual(actual, expected, message) {
  const epsilon = 1e-6;
  if (Math.abs(actual - expected) > epsilon) {
    throw new Error(`${message}: expected ${expected}, got ${actual}`);
  }
}

function runSelfTest() {
  const plan = createSpinPlan({
    count: 8,
    winnerIndex: 3,
    currentRotation: 1.2,
    extraTurns: 5
  });
  const alignedCenter = normalizeAngle(getSliceCenterAngle(8, 3, plan.finalRotation));
  const pointer = normalizeAngle(POINTER_ANGLE);

  assertAlmostEqual(alignedCenter, pointer, 'winning slice should align with top pointer');
  assert(plan.finalRotation > plan.startRotation, 'spin should move forward from current rotation');
  assertAlmostEqual(plan.normalizedFinalRotation, normalizeAngle(-3 * plan.anglePerSlice), 'final rotation should land on the computed target sector');

  const repeatPlan = createSpinPlan({
    count: 6,
    winnerIndex: 1,
    currentRotation: FULL_TURN * 7 + 0.3,
    extraTurns: 4
  });
  const repeatAlignedCenter = normalizeAngle(getSliceCenterAngle(6, 1, repeatPlan.finalRotation));

  assertAlmostEqual(repeatAlignedCenter, pointer, 'repeat spins should not drift away from pointer');

  const displayName = formatRestaurantLabel('特别特别好吃的砂锅饭', 10);
  assert(displayName.endsWith('…'), 'long names should be truncated consistently');
  const shortName = attachDisplayNames([{ name: '牛肉面' }])[0];
  assert(shortName.displayName === '牛肉面', 'short names should stay intact');
  assert(shortName.showFullName === false, 'short names should not show a duplicate full-name line');

  return 'spin-logic self-test passed';
}

if (typeof process !== 'undefined' && process.argv && process.argv.includes('--self-test')) {
  const result = runSelfTest();
  process.stdout.write(`${result}\n`);
}

module.exports = {
  FULL_TURN,
  POINTER_ANGLE,
  SEGMENT_COLORS,
  normalizeAngle,
  getAnglePerSlice,
  getSliceStartAngle,
  getSliceCenterAngle,
  createSpinPlan,
  getSegmentColor,
  formatRestaurantLabel,
  attachDisplayNames,
  runSelfTest
};
