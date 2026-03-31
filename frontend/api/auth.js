// frontend/api/auth.js

import client from './client';

export const WechatLogin = async (code) => {
  return await client.post('/auth/wechat-login', { code });
};

export const Logout = async () => {
  return await client.post('/auth/logout');
};

export const GetMe = async () => {
  return await client.get('/auth/me');
};
