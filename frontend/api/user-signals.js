import client from './client';

export const CreateChoiceHistory = async (userId, payload) => {
  return await client.post(`/users/${userId}/choice-history`, payload);
};

export const ListChoiceHistory = async (userId, params = {}) => {
  return await client.get(`/users/${userId}/choice-history`, params, { silent: true });
};

export const CreateRecommendationFeedback = async (userId, payload) => {
  return await client.post(`/users/${userId}/recommendation-feedback`, payload);
};

export const ListRecommendationFeedback = async (userId, params = {}) => {
  return await client.get(`/users/${userId}/recommendation-feedback`, params, { silent: true });
};

export const GetPreferenceProfile = async (userId) => {
  return await client.get(`/users/${userId}/preference-profile`, {}, {
    silent: true,
    skipAuthRedirect: true
  });
};
