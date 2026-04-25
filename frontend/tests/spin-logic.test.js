const {
  FULL_TURN,
  POINTER_ANGLE,
  normalizeAngle,
  getAnglePerSlice,
  getSliceCenterAngle,
  createSpinPlan,
  getSegmentColor,
  formatRestaurantLabel,
  attachDisplayNames
} = require('../pages/spin/spin-logic');

describe('spin logic', () => {
  test('normalizes negative angles into positive range', () => {
    expect(normalizeAngle(-Math.PI / 2)).toBeCloseTo(FULL_TURN - Math.PI / 2, 6);
  });

  test('returns zero angle when count is zero', () => {
    expect(getAnglePerSlice(0)).toBe(0);
  });

  test('computes even slice angle for eight restaurants', () => {
    expect(getAnglePerSlice(8)).toBeCloseTo(Math.PI / 4, 6);
  });

  test('throws on invalid spin input', () => {
    expect(() => createSpinPlan({ count: 0, winnerIndex: 0 })).toThrow('invalid spin plan input');
    expect(() => createSpinPlan({ count: 4, winnerIndex: -1 })).toThrow('invalid spin plan input');
    expect(() => createSpinPlan({ count: 4, winnerIndex: 4 })).toThrow('invalid spin plan input');
  });

  test('aligns winning slice center with pointer', () => {
    const plan = createSpinPlan({
      count: 6,
      winnerIndex: 2,
      currentRotation: 0.7,
      extraTurns: 5
    });

    const center = normalizeAngle(getSliceCenterAngle(6, 2, plan.finalRotation));

    expect(center).toBeCloseTo(normalizeAngle(POINTER_ANGLE), 6);
    expect(plan.finalRotation).toBeGreaterThan(plan.startRotation);
  });

  test('cycles segment colors by index', () => {
    expect(getSegmentColor(0)).toBe(getSegmentColor(8));
  });

  test('truncates long labels with ellipsis', () => {
    expect(formatRestaurantLabel('特别特别好吃的砂锅饭', 10).endsWith('…')).toBe(true);
  });

  test('keeps short display names unchanged', () => {
    const [item] = attachDisplayNames([{ name: '牛肉面' }]);

    expect(item.displayName).toBe('牛肉面');
    expect(item.showFullName).toBe(false);
  });
});