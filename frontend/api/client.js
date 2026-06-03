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
 * 判断是否为网络层面的错误（可重试）
 */
function isNetworkError(err) {
  if (!err) return false;
  const msg = (err.errMsg || '').toLowerCase();
  return msg.includes('fail') || msg.includes('timeout') || msg.includes('network');
}

/**
 * 指数退避延迟（ms）
 */
function retryDelayMs(attempt) {
  return Math.min(1000 * Math.pow(2, attempt - 1), 8000);
}

/**
 * 执行单次 wx.request
 */
function executeRequest({ url, method, data, header, timeout, allowHttpStatus, returnFullResponse, silent, skipAuthRedirect }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url,
      method,
      data,
      header,
      timeout: timeout || 10000,
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
            message: (data && data.message) || 'Unauthorized',
            retryable: false
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
            message: (data && data.message) || '请求失败',
            retryable: false
          });
        }
      },
      fail: (err) => {
        reject({
          statusCode: 0,
          data: null,
          code: 0,
          message: (err && err.errMsg) ? err.errMsg : '网络请求异常',
          raw: err,
          retryable: isNetworkError(err)
        });
      }
    });
  });
}

/**
 * 封装微信的 wx.request，支持超时与指数退避重试
 * @param {string} url 请求地址
 * @param {string} method 请求方法 GET/POST/PUT/DELETE
 * @param {object} data 请求数据
 * @param {object} options 额外配置
 * @param {number} options.timeout 超时时间（ms），默认 10000
 * @param {number} options.retries 最大重试次数，默认 2（即最多请求 3 次）
 * @param {boolean} options.silent 是否静默错误
 * @param {number[]} options.allowHttpStatus 允许的非 2xx 状态码
 * @param {boolean} options.returnFullResponse 返回完整响应
 * @param {boolean} options.skipAuthRedirect 跳过 401 重定向
 */
const request = (url, method = 'GET', data = {}, options = {}) => {
  const {
    silent = false,
    allowHttpStatus = [],
    returnFullResponse = false,
    skipAuthRedirect = false,
    timeout = 10000,
    retries = 2
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

  const fullUrl = `${getApiBaseUrl()}${url}`;

  let attempt = 0;

  const tryRequest = () => {
    return executeRequest({
      url: fullUrl,
      method,
      data,
      header,
      timeout,
      allowHttpStatus,
      returnFullResponse,
      silent,
      skipAuthRedirect
    }).catch((err) => {
      attempt += 1;
      // 仅对网络层面错误进行重试，业务错误直接抛出
      if (err && err.retryable && attempt <= retries) {
        console.warn(`请求失败，${retryDelayMs(attempt)}ms 后第 ${attempt} 次重试: ${fullUrl}`);
        return new Promise((resolve) => {
          setTimeout(() => {
            resolve(tryRequest());
          }, retryDelayMs(attempt));
        });
      }

      // 最终失败时提示（仅非静默模式）
      if (!silent && attempt > 0) {
        wx.showToast({
          title: '网络开小差了，请稍后重试',
          icon: 'none'
        });
      }

      throw err;
    });
  };

  return tryRequest();
};

export default {
  get: (url, data, options) => request(url, 'GET', data, options),
  post: (url, data, options) => request(url, 'POST', data, options),
  put: (url, data, options) => request(url, 'PUT', data, options),
  delete: (url, data, options) => request(url, 'DELETE', data, options)
};
