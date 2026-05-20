import { getApiBaseUrl } from './base-url';
import {
  getApiTransportMode,
  getCloudbaseConfig,
  getCloudbaseServiceName
} from './cloudbase-config';

const API_PREFIX = '/api/v1';

function ensureLeadingSlash(path) {
  if (!path) {
    return '/';
  }
  return path.startsWith('/') ? path : `/${path}`;
}

function toCloudPath(path) {
  const normalized = ensureLeadingSlash(path);
  if (/^\/api\/v\d+\//.test(normalized) || /^\/api\/v\d+$/.test(normalized)) {
    return normalized;
  }
  return `${API_PREFIX}${normalized}`;
}

function getHeaderValue(header, name) {
  if (!header) {
    return '';
  }
  return header[name] || header[name.toLowerCase()] || '';
}

function normalizeResponse(res = {}) {
  const header = res.header || res.headers || {};
  const upstreamStatus = Number(getHeaderValue(header, 'X-Cloudbase-Upstream-Status-Code'));
  const statusCode = Number.isFinite(upstreamStatus) && upstreamStatus > 0
    ? upstreamStatus
    : Number(res.statusCode || 200);

  return {
    statusCode,
    data: res.data,
    header
  };
}

function buildCloudHeader(header = {}) {
  return {
    ...header,
    'X-WX-SERVICE': getCloudbaseServiceName()
  };
}

function callCloudContainer(url, method, data, header) {
  if (!wx.cloud || typeof wx.cloud.callContainer !== 'function') {
    return Promise.reject({ errMsg: 'wx.cloud.callContainer unavailable' });
  }

  return new Promise((resolve, reject) => {
    const options = {
      config: getCloudbaseConfig(),
      path: toCloudPath(url),
      method,
      data,
      header: buildCloudHeader(header),
      success: resolve,
      fail: reject
    };
    const task = wx.cloud.callContainer(options);
    if (task && typeof task.then === 'function') {
      task.then(resolve).catch(reject);
    }
  }).then(normalizeResponse);
}

function callWxRequest(url, method, data, header) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${getApiBaseUrl()}${url}`,
      method,
      data,
      header,
      success: (res) => resolve(normalizeResponse(res)),
      fail: reject
    });
  });
}

export function sendApiRequest({ url, method, data, header }) {
  if (getApiTransportMode() === 'cloudbase') {
    return callCloudContainer(url, method, data, header);
  }

  return callWxRequest(url, method, data, header);
}

export function startApiStream({ url, method, data, header, success, fail }) {
  const streamOptions = {
    method,
    data,
    header,
    enableChunked: true,
    dataType: 'text',
    responseType: 'arraybuffer',
    success,
    fail
  };

  if (getApiTransportMode() === 'cloudbase') {
    if (!wx.cloud || typeof wx.cloud.callContainer !== 'function') {
      if (typeof fail === 'function') {
        fail({ errMsg: 'wx.cloud.callContainer unavailable' });
      }
      return null;
    }

    const task = wx.cloud.callContainer({
      ...streamOptions,
      config: getCloudbaseConfig(),
      path: toCloudPath(url),
      header: buildCloudHeader(header)
    });

    if (task && typeof task.then === 'function') {
      task.then(success).catch(fail);
    }
    return task;
  }

  return wx.request({
    ...streamOptions,
    url: `${getApiBaseUrl()}${url}`
  });
}
