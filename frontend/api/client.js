// frontend/api/client.js

import { getApiBaseUrl } from './base-url';

let isHandlingUnauthorized = false;
const TOKEN_KEY = 'token';
const USER_KEY = 'user_info';

function clearStoredAuth() {
  wx.removeStorageSync(TOKEN_KEY);
  wx.removeStorageSync(USER_KEY);

  if (typeof getApp === 'function') {
    try {
      const app = getApp();
      if (app && app.globalData) {
        app.globalData.token = '';
        app.globalData.user = null;
        app.globalData.blacklistPoiIds = [];
      }
    } catch (error) {}
  }
}

/**
 * 封装微信的 wx.request
 * @param {string} url 请求地址
 * @param {string} method 请求方法 GET/POST/PUT/DELETE
 * @param {object} data 请求数据
 * @param {object} options 额外配置
 */
const request = (url, method = 'GET', data = {}, options = {}) => {
  return new Promise((resolve, reject) => {
    const {
      silent = false,
      allowHttpStatus = [],
      returnFullResponse = false,
      skipAuthRedirect = false
    } = options;

    // 从本地缓存获取 token
    const token = wx.getStorageSync(TOKEN_KEY);
    const header = {
      'Content-Type': 'application/json',
      ...options.header
    };

    if (token) {
      header['Authorization'] = `Bearer ${token}`;
    }

    wx.request({
      url: `${getApiBaseUrl()}${url}`,
      method,
      data,
      header,
      success: (res) => {
        const { statusCode, data } = res;
        const is2xx = statusCode >= 200 && statusCode < 300;
        const isAllowedStatus = Array.isArray(allowHttpStatus) && allowHttpStatus.includes(statusCode);
        if (is2xx || isAllowedStatus) {
          if (returnFullResponse) {
            resolve({
              statusCode,
              data,
              header: res.header || {}
            });
            return;
          }
          resolve(data);
        } else if (statusCode === 401) {
          clearStoredAuth();

          if (!skipAuthRedirect && !isHandlingUnauthorized) {
            isHandlingUnauthorized = true;
            if (!silent) {
              wx.showToast({
                title: '登录已过期，请重新登录',
                icon: 'none'
              });
            }
            wx.redirectTo({
              url: '/pages/index/index',
              complete: () => {
                setTimeout(() => {
                  isHandlingUnauthorized = false;
                }, 600);
              }
            });
          }

          reject({
            statusCode,
            data,
            code: data && data.code,
            message: (data && data.message) || 'Unauthorized'
          });
        } else {
          if (!silent) {
            wx.showToast({
              title: (data && data.message) || '请求失败',
              icon: 'none'
            });
          }
          reject({
            statusCode,
            data,
            code: data && data.code,
            message: (data && data.message) || '请求失败'
          });
        }
      },
      fail: (err) => {
        const message = (err && err.errMsg) ? err.errMsg : '网络请求异常';
        const requestUrl = `${getApiBaseUrl()}${url}`;
        if (!silent) {
          wx.showToast({
            title: '网络开小差了，请稍后重试',
            icon: 'none'
          });
        }
        console.error('请求失败详情:', requestUrl, message, err);
        reject({
          statusCode: 0,
          data: null,
          code: 0,
          message,
          raw: err
        });
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
