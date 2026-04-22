import client from './client';

export const ListBlacklist = async (userId, params = {}) => {
  return await client.get(`/users/${userId}/blacklist`, {
    page: 1,
    size: 100,
    ...params
  }, {
    silent: true
  });
};

export const AddBlacklist = async (userId, payload) => {
  return await client.post(`/users/${userId}/blacklist`, payload);
};

export const RemoveBlacklist = async (userId, poiId) => {
  return await client.delete(`/users/${userId}/blacklist/${encodeURIComponent(poiId)}`);
};
