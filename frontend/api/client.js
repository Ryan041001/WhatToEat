// frontend/api/client.js

// 根据环境配置 baseURL，微信开发者工具本地调试默认使用 127.0.0.1
const baseURL = 'http://127.0.0.1:8080/api/v1';

/**
 * 封装微信的 wx.request
 * @param {string} url 请求地址
 * @param {string} method 请求方法 GET/POST/PUT/DELETE
 * @param {object} data 请求数据
 * @param {object} options 额外配置
 */
const request = (url, method = 'GET', data = {}, options = {}) => {
  return new Promise((resolve, reject) => {
    // 从本地缓存获取 token
    const token = wx.getStorageSync('token');
    const header = {
      'Content-Type': 'application/json',
      ...options.header
    };

    if (token) {
      header['Authorization'] = `Bearer ${token}`;
    }

    wx.request({
      url: `${baseURL}${url}`,
      method,
      data,
      header,
      success: (res) => {
        const { statusCode, data } = res;
        // 简单处理状态码，通常 2xx 表示成功
        if (statusCode >= 200 && statusCode < 300) {
          resolve(data);
        } else if (statusCode === 401) {
          // 未授权，处理 token 过期的情况
          wx.removeStorageSync('token');
          wx.showToast({
            title: '登录已过期，请重新登录',
            icon: 'none'
          });
          wx.redirectTo({
            url: '/pages/index/index'
          });
          reject(new Error('Unauthorized'));
        } else {
          // 其他服务器错误
          wx.showToast({
            title: data.message || '请求失败',
            icon: 'none'
          });
          reject(data);
        }
      },
      fail: (err) => {
        const message = (err && err.errMsg) ? err.errMsg : '网络请求异常';
        wx.showToast({
          title: '请求失败，请看控制台',
          icon: 'none'
        });
        console.error('请求失败详情:', message, err);
        reject(err);
      }
    });
  });
};

export default {
  get: (url, data, options) => request(url, 'GET', data, options),
  post: (url, data, options) => request(url, 'POST', data, options),
  put: (url, data, options) => request(url, 'PUT', data, options),
  delete: (url, data, options) => request(url, 'DELETE', data, options)
};
