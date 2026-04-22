// frontend/api/auth.js

import client from './client';

export const WechatLogin = async (code, nickname = '微信用户', avatarUrl = '') => {
  return await client.post('/auth/wechat-login', { code, nickname, avatarUrl });
};

export const Logout = async () => {
  return await client.post('/auth/logout');
};

export const GetMe = async () => {
  return await client.get('/auth/me');
};
