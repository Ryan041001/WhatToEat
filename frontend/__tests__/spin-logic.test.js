const {
  FULL_TURN,
  normalizeAngle,
  getAnglePerSlice,
  createSpinPlan,
  getSegmentColor,
  formatRestaurantLabel,
  attachDisplayNames,
  getSliceCenterAngle,
  POINTER_ANGLE
} = require('../pages/spin/spin-logic');

describe('spin logic', () => {
  test('normalizeAngle should normalize negative value to [0, 2pi)', () => {
    const normalized = normalizeAngle(-Math.PI / 2);
    expect(normalized).toBeCloseTo(FULL_TURN - Math.PI / 2, 6);
  });

  test('getAnglePerSlice should return 0 when count is 0', () => {
    expect(getAnglePerSlice(0)).toBe(0);
  });

  test('getAnglePerSlice should compute equal slice angle', () => {
    expect(getAnglePerSlice(8)).toBeCloseTo(Math.PI / 4, 6);
  });

  test('createSpinPlan should throw for invalid input', () => {
    expect(() => createSpinPlan({ count: 0, winnerIndex: 0 })).toThrow('invalid spin plan input');
    expect(() => createSpinPlan({ count: 4, winnerIndex: -1 })).toThrow('invalid spin plan input');
    expect(() => createSpinPlan({ count: 4, winnerIndex: 4 })).toThrow('invalid spin plan input');
  });

  test('createSpinPlan should align winner center to pointer', () => {
    const plan = createSpinPlan({
      count: 6,
      winnerIndex: 2,
      currentRotation: 0.7,
      extraTurns: 5
    });

    const center = normalizeAngle(getSliceCenterAngle(6, 2, plan.finalRotation));
    const pointer = normalizeAngle(POINTER_ANGLE);

    expect(center).toBeCloseTo(pointer, 6);
    expect(plan.finalRotation).toBeGreaterThan(plan.startRotation);
  });

  test('getSegmentColor should cycle by color list length', () => {
    expect(getSegmentColor(0)).toBe(getSegmentColor(8));
  });

  test('formatRestaurantLabel should truncate long name with ellipsis', () => {
    expect(formatRestaurantLabel('特别特别好吃的砂锅饭', 10).endsWith('…')).toBe(true);
  });

  test('attachDisplayNames should keep short name unchanged', () => {
    const [item] = attachDisplayNames([{ name: '牛肉面' }]);
    expect(item.displayName).toBe('牛肉面');
    expect(item.showFullName).toBe(false);
  });
});
