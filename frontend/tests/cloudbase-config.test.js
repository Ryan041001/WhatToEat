describe('cloudbase config', () => {
  beforeEach(() => {
    jest.resetModules();
    global.getApp = jest.fn(() => ({ globalData: {} }));
    global.wx = {
      cloud: {
        init: jest.fn()
      },
      getStorageSync: jest.fn(() => ''),
      setStorageSync: jest.fn()
    };
  });

  afterEach(() => {
    delete global.getApp;
    delete global.wx;
    jest.restoreAllMocks();
  });

  test('uses the deployed CloudBase environment by default', () => {
    const { getCloudbaseConfig, initCloudbase } = require('../api/cloudbase-config');

    expect(getCloudbaseConfig()).toEqual({ env: 'cloud1-d0gendp5i219d4f5f' });

    expect(initCloudbase()).toBe(true);
    expect(wx.cloud.init).toHaveBeenCalledWith({ env: 'cloud1-d0gendp5i219d4f5f' });
  });
});
