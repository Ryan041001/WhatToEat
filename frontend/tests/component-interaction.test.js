const path = require('path');

function loadComponentDefinition(relativePath) {
  let capturedDefinition = null;

  global.Component = (definition) => {
    capturedDefinition = definition;
  };

  jest.isolateModules(() => {
    require(path.resolve(__dirname, '..', relativePath));
  });

  return capturedDefinition;
}

describe('component interactions', () => {
  beforeEach(() => {
    jest.resetModules();
    global.wx = {
      redirectTo: jest.fn(),
      navigateBack: jest.fn(),
      getMenuButtonBoundingClientRect: jest.fn(() => ({ left: 280 })),
      getDeviceInfo: jest.fn(() => ({ platform: 'android' })),
      getWindowInfo: jest.fn(() => ({ windowWidth: 360, safeArea: { top: 20, bottom: 0 } })),
      getSystemInfoSync: jest.fn(() => ({ platform: 'android', windowWidth: 360, safeArea: { top: 20, bottom: 0 } }))
    };
  });

  afterEach(() => {
    delete global.Component;
    delete global.wx;
    jest.restoreAllMocks();
  });

  test('bottom-nav keeps current page unchanged', () => {
    const definition = loadComponentDefinition('components/bottom-nav/bottom-nav.js');
    const instance = { data: { current: 'home' } };

    definition.methods.onNavTap.call(instance, {
      currentTarget: { dataset: { target: 'home' } }
    });

    expect(wx.redirectTo).not.toHaveBeenCalled();
  });

  test('bottom-nav redirects to tapped target', () => {
    const definition = loadComponentDefinition('components/bottom-nav/bottom-nav.js');
    const instance = { data: { current: 'home' } };

    definition.methods.onNavTap.call(instance, {
      currentTarget: { dataset: { target: 'spin' } }
    });

    expect(wx.redirectTo).toHaveBeenCalledWith({ url: '/pages/spin/spin' });
  });

  test('restaurant-card emits tapcard event', () => {
    const definition = loadComponentDefinition('components/restaurant-card/restaurant-card.js');
    const restaurant = { id: 'poi-1', name: '牛肉面' };
    const instance = {
      data: { restaurant },
      triggerEvent: jest.fn()
    };

    definition.methods.onTap.call(instance);

    expect(instance.triggerEvent).toHaveBeenCalledWith('tapcard', { restaurant });
  });

  test('loading-spinner exposes default loading text', () => {
    const definition = loadComponentDefinition('components/loading-spinner/loading-spinner.js');

    expect(definition.properties.message.value).toBe('加载中...');
    expect(definition.properties.show.type).toBe(Boolean);
  });

  test('navigation-bar attached sets layout state', () => {
    const definition = loadComponentDefinition('components/navigation-bar/navigation-bar.js');
    const instance = { setData: jest.fn() };

    definition.lifetimes.attached.call(instance);

    expect(instance.setData).toHaveBeenCalledWith(expect.objectContaining({
      ios: false,
      innerPaddingRight: 'padding-right: 80px',
      leftWidth: 'width: 80px'
    }));
  });

  test('navigation-bar uses opacity when animated', () => {
    const definition = loadComponentDefinition('components/navigation-bar/navigation-bar.js');
    const instance = {
      data: { animated: true },
      setData: jest.fn()
    };

    definition.methods._showChange.call(instance, false);

    expect(instance.setData).toHaveBeenCalledWith({
      displayStyle: 'opacity: 0;transition:opacity 0.5s;'
    });
  });

  test('navigation-bar hides with display style when animation is off', () => {
    const definition = loadComponentDefinition('components/navigation-bar/navigation-bar.js');
    const instance = {
      data: { animated: false },
      setData: jest.fn()
    };

    definition.methods._showChange.call(instance, false);

    expect(instance.setData).toHaveBeenCalledWith({
      displayStyle: 'display: none'
    });
  });

  test('navigation-bar back triggers back navigation and event', () => {
    const definition = loadComponentDefinition('components/navigation-bar/navigation-bar.js');
    const instance = {
      data: { delta: 2 },
      triggerEvent: jest.fn()
    };

    definition.methods.back.call(instance);

    expect(wx.navigateBack).toHaveBeenCalledWith({ delta: 2 });
    expect(instance.triggerEvent).toHaveBeenCalledWith('back', { delta: 2 }, {});
  });
});