const API_BASE_URL_STORAGE_KEY = 'apiBaseUrl';
const DEVTOOLS_API_BASE_URL = 'http://127.0.0.1:8080/api/v1';
// Update this when your laptop joins a different LAN.
const REAL_DEVICE_API_BASE_URL = 'http://192.168.1.176:8080/api/v1';

function normalizeApiBaseUrl(input) {
  if (typeof input !== 'string') {
    return '';
  }

  const trimmed = input.trim();
  if (!trimmed) {
    return '';
  }

  const normalized = trimmed.replace(/\/+$/, '');
  if (/\/api\/v\d+$/i.test(normalized)) {
    return normalized;
  }

  return `${normalized}/api/v1`;
}

function readAppLevelApiBaseUrl() {
  try {
    const app = getApp();
    return app && app.globalData ? app.globalData.apiBaseUrl : '';
  } catch (error) {
    return '';
  }
}

function isWechatDevtools() {
  try {
    const info = wx.getSystemInfoSync();
    return info && info.platform === 'devtools';
  } catch (error) {
    return false;
  }
}

function getDefaultApiBaseUrl() {
  return isWechatDevtools() ? DEVTOOLS_API_BASE_URL : REAL_DEVICE_API_BASE_URL;
}

export function getApiBaseUrl() {
  const appLevel = normalizeApiBaseUrl(readAppLevelApiBaseUrl());
  if (appLevel) {
    return appLevel;
  }

  const fromStorage = normalizeApiBaseUrl(wx.getStorageSync(API_BASE_URL_STORAGE_KEY));
  if (fromStorage) {
    return fromStorage;
  }

  return getDefaultApiBaseUrl();
}

export function setApiBaseUrl(nextBaseUrl) {
  const normalized = normalizeApiBaseUrl(nextBaseUrl);
  if (!normalized) {
    return '';
  }

  wx.setStorageSync(API_BASE_URL_STORAGE_KEY, normalized);
  try {
    const app = getApp();
    if (app && app.globalData) {
      app.globalData.apiBaseUrl = normalized;
    }
  } catch (error) {
    // ignore; app might be unavailable during startup
  }

  return normalized;
}

export function resetApiBaseUrl() {
  wx.removeStorageSync(API_BASE_URL_STORAGE_KEY);
  try {
    const app = getApp();
    if (app && app.globalData && app.globalData.apiBaseUrl) {
      delete app.globalData.apiBaseUrl;
    }
  } catch (error) {
    // ignore; app might be unavailable during startup
  }
}

export function useDevtoolsApiBaseUrl() {
  return setApiBaseUrl(DEVTOOLS_API_BASE_URL);
}

export function useRealDeviceApiBaseUrl() {
  return setApiBaseUrl(REAL_DEVICE_API_BASE_URL);
}
