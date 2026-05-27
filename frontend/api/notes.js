import client from './client';

export const CreateNote = async (userId, payload) => {
  return await client.post(`/users/${userId}/notes`, payload);
};

export const ListNotes = async (userId, params = {}) => {
  return await client.get(`/users/${userId}/notes`, params, { silent: true });
};

export const GetNote = async (userId, noteId) => {
  return await client.get(`/users/${userId}/notes/${noteId}`, {}, { silent: true });
};

export const UpdateNote = async (userId, noteId, payload) => {
  return await client.put(`/users/${userId}/notes/${noteId}`, payload);
};

export const DeleteNote = async (userId, noteId) => {
  return await client.delete(`/users/${userId}/notes/${noteId}`);
};
