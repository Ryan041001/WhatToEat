describe('api client with mocked wx.request', () => {
  beforeEach(() => {
    jest.resetModules();
    jest.spyOn(global, 'setTimeout').mockImplementation((callback) => {
      callback();
      return 1;
    });

    global.getApp = jest.fn(() => ({ globalData: {} }));
    global.wx = {
      getStorageSync: jest.fn((key) => {
        if (key === 'token') {
          return 'mock-token';
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

  test('should resolve payload on 200 response', async () => {
    const client = require('../api/client').default;

    wx.request.mockImplementation((options) => {
      options.success({
        statusCode: 200,
        data: { code: 0, data: { ok: true } },
        header: {}
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

  test('should return full response when returnFullResponse=true', async () => {
    const client = require('../api/client').default;

    wx.request.mockImplementation((options) => {
      options.success({
        statusCode: 404,
        data: { code: 4040, message: 'not found' },
        header: { 'x-trace-id': 'trace-1' }
      });
    });

    const response = await client.get('/resource', {}, {
      allowHttpStatus: [404],
      returnFullResponse: true
    });

    expect(response).toEqual({
      statusCode: 404,
      data: { code: 4040, message: 'not found' },
      header: { 'x-trace-id': 'trace-1' }
    });
  });

  test('should clear token and redirect on 401', async () => {
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
    expect(wx.showToast).toHaveBeenCalledWith({
      title: '登录已过期，请重新登录',
      icon: 'none'
    });
    expect(wx.redirectTo).toHaveBeenCalledWith(expect.objectContaining({
      url: '/pages/index/index'
    }));
  });

  test('should reject with backend message on non-2xx response', async () => {
    const client = require('../api/client').default;

    wx.request.mockImplementation((options) => {
      options.success({
        statusCode: 500,
        data: { code: 9000, message: '服务器错误' },
        header: {}
      });
    });

    await expect(client.post('/broken', { a: 1 })).rejects.toMatchObject({
      statusCode: 500,
      code: 9000,
      message: '服务器错误'
    });

    expect(wx.showToast).toHaveBeenCalledWith({
      title: '服务器错误',
      icon: 'none'
    });
  });

  test('should reject on network failure and show fallback toast', async () => {
    const client = require('../api/client').default;

    wx.request.mockImplementation((options) => {
      options.fail({ errMsg: 'request:fail timeout' });
    });

    await expect(client.get('/timeout')).rejects.toMatchObject({
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