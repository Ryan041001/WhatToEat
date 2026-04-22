const fs = require('fs');
const path = require('path');

function loadComponentDefinition(relativePath) {
  let captured = null;
  global.Component = (definition) => {
    captured = definition;
  };

  jest.isolateModules(() => {
    require(path.resolve(__dirname, '..', relativePath));
  });

  return captured;
}

function loadPageDefinition(relativePath, appMock) {
  let captured = null;
  global.Page = (definition) => {
    captured = definition;
  };
  global.getApp = jest.fn(() => appMock);

  jest.isolateModules(() => {
    require(path.resolve(__dirname, '..', relativePath));
  });

  return captured;
}

describe('component/page interaction tests', () => {
  beforeEach(() => {
    jest.resetModules();
    global.wx = {
      redirectTo: jest.fn(),
      navigateTo: jest.fn(),
      navigateBack: jest.fn(),
      showToast: jest.fn(),
      vibrateShort: jest.fn(),
      startAccelerometer: jest.fn(),
      offAccelerometerChange: jest.fn(),
      onAccelerometerChange: jest.fn(),
      stopAccelerometer: jest.fn(),
      getMenuButtonBoundingClientRect: jest.fn(() => ({ left: 280 })),
      getDeviceInfo: jest.fn(() => ({ platform: 'android' })),
      getWindowInfo: jest.fn(() => ({ windowWidth: 360, safeArea: { top: 20, bottom: 0 } })),
      getSystemInfoSync: jest.fn(() => ({ pixelRatio: 2, platform: 'android', windowWidth: 360, safeArea: { top: 20, bottom: 0 } }))
    };
  });

  afterEach(() => {
    delete global.Component;
    delete global.Page;
    delete global.getApp;
    delete global.wx;
    jest.restoreAllMocks();
  });

  test('bottom-nav: same target should not redirect', () => {
    const definition = loadComponentDefinition('components/bottom-nav/bottom-nav.js');
    const instance = { data: { current: 'home' } };

    definition.methods.onNavTap.call(instance, {
      currentTarget: { dataset: { target: 'home' } }
    });

    expect(wx.redirectTo).not.toHaveBeenCalled();
  });

  test('bottom-nav: different target should redirect to target page', () => {
    const definition = loadComponentDefinition('components/bottom-nav/bottom-nav.js');
    const instance = { data: { current: 'home' } };

    definition.methods.onNavTap.call(instance, {
      currentTarget: { dataset: { target: 'spin' } }
    });

    expect(wx.redirectTo).toHaveBeenCalledWith({
      url: '/pages/spin/spin'
    });
  });

  test('restaurant-card: tap should emit tapcard with restaurant payload', () => {
    const definition = loadComponentDefinition('components/restaurant-card/restaurant-card.js');
    const restaurant = { id: 'poi_1', name: '牛肉面' };
    const instance = {
      data: { restaurant },
      triggerEvent: jest.fn()
    };

    definition.methods.onTap.call(instance);

    expect(instance.triggerEvent).toHaveBeenCalledWith('tapcard', { restaurant });
  });

  test('loading-spinner: should define default message text', () => {
    const definition = loadComponentDefinition('components/loading-spinner/loading-spinner.js');
    expect(definition.properties.message.value).toBe('加载中...');
    expect(definition.properties.show.type).toBe(Boolean);
  });

  test('navigation-bar: attached should set layout data', () => {
    const definition = loadComponentDefinition('components/navigation-bar/navigation-bar.js');
    const instance = {
      setData: jest.fn()
    };

    definition.lifetimes.attached.call(instance);

    expect(instance.setData).toHaveBeenCalled();
    const payload = instance.setData.mock.calls[0][0];
    expect(payload.ios).toBe(false);
    expect(payload.innerPaddingRight).toContain('padding-right');
  });

  test('navigation-bar: _showChange with animated=true should use opacity style', () => {
    const definition = loadComponentDefinition('components/navigation-bar/navigation-bar.js');
    const instance = {
      data: { animated: true },
      setData: jest.fn()
    };

    definition.methods._showChange.call(instance, false);

    expect(instance.setData).toHaveBeenCalledWith({
      displayStyle: expect.stringContaining('opacity: 0')
    });
  });

  test('navigation-bar: _showChange with animated=false should use display style', () => {
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

  test('navigation-bar: back should navigate and emit back event', () => {
    const definition = loadComponentDefinition('components/navigation-bar/navigation-bar.js');
    const instance = {
      data: { delta: 2 },
      triggerEvent: jest.fn()
    };

    definition.methods.back.call(instance);

    expect(wx.navigateBack).toHaveBeenCalledWith({ delta: 2 });
    expect(instance.triggerEvent).toHaveBeenCalledWith('back', { delta: 2 }, {});
  });

  test('spin page template: result card should render after action row', () => {
    const templatePath = path.resolve(__dirname, '..', 'pages/spin/spin.wxml');
    const template = fs.readFileSync(templatePath, 'utf8');

    const resultCardIndex = template.indexOf('class="result-card');
    const actionRowIndex = template.indexOf('class="action-row"');

    expect(resultCardIndex).toBeGreaterThan(-1);
    expect(actionRowIndex).toBeGreaterThan(-1);
    expect(resultCardIndex).toBeGreaterThan(actionRowIndex);
  });

  test('home page: handleShake should show toast when no active restaurants', () => {
    const appMock = {
      getActiveRestaurants: jest.fn(() => [])
    };
    const page = loadPageDefinition('pages/home/home.js', appMock);
    const instance = {
      data: { shaking: false, shakeResult: null },
      setData: jest.fn()
    };

    page.handleShake.call(instance);

    expect(wx.showToast).toHaveBeenCalledWith({
      title: '暂时没有可选餐厅',
      icon: 'none'
    });
  });

  test('home page: handleShake should set picked result after vibration', () => {
    const activeRestaurants = [
      { id: 'r1', name: '饺子馆' },
      { id: 'r2', name: '盖饭店' }
    ];
    const appMock = {
      getActiveRestaurants: jest.fn(() => activeRestaurants)
    };
    const page = loadPageDefinition('pages/home/home.js', appMock);
    const instance = {
      data: { shaking: false, shakeResult: null },
      setData(update) {
        this.data = { ...this.data, ...update };
      }
    };

    jest.spyOn(Math, 'random').mockReturnValue(0);
    jest.spyOn(global, 'setTimeout').mockImplementation((callback) => {
      callback();
      return 1;
    });

    page.handleShake.call(instance);

    expect(wx.vibrateShort).toHaveBeenCalledWith({ type: 'medium' });
    expect(instance.data.shaking).toBe(false);
    expect(instance.data.shakeResult).toEqual(activeRestaurants[0]);
  });
});