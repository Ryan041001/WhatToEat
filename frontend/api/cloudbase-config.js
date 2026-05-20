const API_TRANSPORT_MODE_STORAGE_KEY = 'apiTransportMode';
const DEFAULT_API_TRANSPORT_MODE = 'cloudbase';
const VALID_TRANSPORT_MODES = new Set(['cloudbase', 'request']);

const CLOUDBASE_ENV_ID = 'cloud1-d0gendp5i219d4f5f';
const CLOUDBASE_BACKEND_SERVICE = 'whattoeat-backend';

function normalizeTransportMode(value) {
  if (typeof value !== 'string') {
    return '';
  }

  const normalized = value.trim().toLowerCase();
  return VALID_TRANSPORT_MODES.has(normalized) ? normalized : '';
}

function readAppGlobal(key) {
  try {
    const app = getApp();
    return app && app.globalData ? app.globalData[key] : '';
  } catch (error) {
    return '';
  }
}

function normalizeEnvId(value) {
  return typeof value === 'string' ? value.trim() : '';
}

export function getApiTransportMode() {
  const appLevel = normalizeTransportMode(readAppGlobal('apiTransportMode'));
  if (appLevel) {
    return appLevel;
  }

  const fromStorage = normalizeTransportMode(wx.getStorageSync(API_TRANSPORT_MODE_STORAGE_KEY));
  return fromStorage || DEFAULT_API_TRANSPORT_MODE;
}

export function setApiTransportMode(mode) {
  const normalized = normalizeTransportMode(mode);
  if (!normalized) {
    return '';
  }

  wx.setStorageSync(API_TRANSPORT_MODE_STORAGE_KEY, normalized);
  try {
    const app = getApp();
    if (app && app.globalData) {
      app.globalData.apiTransportMode = normalized;
    }
  } catch (error) {
    // ignore; app might be unavailable during startup
  }

  return normalized;
}

export function getCloudbaseEnvId() {
  return normalizeEnvId(readAppGlobal('cloudbaseEnvId')) || normalizeEnvId(CLOUDBASE_ENV_ID);
}

export function getCloudbaseServiceName() {
  return normalizeEnvId(readAppGlobal('cloudbaseBackendService')) || CLOUDBASE_BACKEND_SERVICE;
}

export function getCloudbaseConfig() {
  const env = getCloudbaseEnvId();
  return env ? { env } : {};
}

export function initCloudbase() {
  if (!wx.cloud || typeof wx.cloud.init !== 'function') {
    return false;
  }

  wx.cloud.init(getCloudbaseConfig());
  return true;
}
