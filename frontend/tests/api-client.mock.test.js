describe('api client mock requests', () => {
  beforeEach(() => {
    jest.resetModules();
    jest.spyOn(global, 'setTimeout').mockImplementation((callback) => {
      callback();
      return 1;
    });
    jest.spyOn(console, 'error').mockImplementation(() => {});

    const appState = { globalData: { token: 'old-token', user: { id: 1 }, blacklistPoiIds: ['poi-1'] } };
    global.getApp = jest.fn(() => appState);
    global.wx = {
      getStorageSync: jest.fn((key) => {
        if (key === 'token') {
          return 'mock-token';
        }
        if (key === 'apiBaseUrl') {
          return '';
        }
        return '';
      }),
      removeStorageSync: jest.fn(),
      showToast: jest.fn(),
      redirectTo: jest.fn(({ complete }) => {
        if (typeof complete === 'function') {
          complete();
        }
      }),
      request: jest.fn()
    };
  });

  afterEach(() => {
    delete global.getApp;
    delete global.wx;
    jest.restoreAllMocks();
  });

  test('resolves payload on 200 response', async () => {
    const client = require('../api/client').default;

    wx.request.mockImplementation((options) => {
      options.success({
        statusCode: 200,
        data: { code: 0, data: { ok: true } },
        header: { 'x-trace-id': 'trace-1' }
      });
    });

    const response = await client.get('/health');

    expect(response).toEqual({ code: 0, data: { ok: true } });
    expect(wx.request).toHaveBeenCalledWith(expect.objectContaining({
      method: 'GET',
      url: expect.stringContaining('/health'),
      header: expect.objectContaining({
        Authorization: 'Bearer mock-token'
      })
    }));
  });

  test('returns full response when status is allowed', async () => {
    const client = require('../api/client').default;

    wx.request.mockImplementation((options) => {
      options.success({
        statusCode: 404,
        data: { code: 4040, message: 'not found' },
        header: { 'x-trace-id': 'trace-2' }
      });
    });

    const response = await client.get('/resource', {}, {
      allowHttpStatus: [404],
      returnFullResponse: true
    });

    expect(response).toEqual({
      statusCode: 404,
      data: { code: 4040, message: 'not found' },
      header: { 'x-trace-id': 'trace-2' }
    });
  });

  test('clears auth and redirects on 401', async () => {
    const client = require('../api/client').default;

    wx.request.mockImplementation((options) => {
      options.success({
        statusCode: 401,
        data: { code: 1003, message: 'unauthorized' },
        header: {}
      });
    });

    await expect(client.get('/secure')).rejects.toMatchObject({
      statusCode: 401,
      code: 1003,
      message: 'unauthorized'
    });

    expect(wx.removeStorageSync).toHaveBeenCalledWith('token');
    expect(wx.removeStorageSync).toHaveBeenCalledWith('user_info');
    expect(wx.showToast).toHaveBeenCalledWith({
      title: '登录已过期，请重新登录',
      icon: 'none'
    });
    expect(wx.redirectTo).toHaveBeenCalledWith(expect.objectContaining({
      url: '/pages/index/index'
    }));
  });

  test('rejects on network failure and shows toast', async () => {
    const client = require('../api/client').default;

    wx.request.mockImplementation((options) => {
      options.fail({ errMsg: 'request:fail timeout' });
    });

    await expect(client.post('/broken', { a: 1 })).rejects.toMatchObject({
      statusCode: 0,
      code: 0,
      message: 'request:fail timeout'
    });

    expect(wx.showToast).toHaveBeenCalledWith({
      title: '网络开小差了，请稍后重试',
      icon: 'none'
    });
  });
});