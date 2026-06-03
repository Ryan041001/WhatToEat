import client from './client';

export const GetRandomRecommendation = async (params = {}) => {
  return await client.get('/recommendations/random', params);
};

export const GetRecommendationCards = async (params = {}) => {
  return await client.get('/recommendations/cards', params);
};
