import client from './client';

export const GetReviewSummary = async (poiId) => {
  return await client.get(`/restaurants/${encodeURIComponent(poiId)}/review-summary`);
};

export const GetRestaurantReviews = async (poiId, params = {}) => {
  return await client.get(`/restaurants/${encodeURIComponent(poiId)}/reviews`, params);
};

export const GetMyRestaurantReview = async (userId, poiId) => {
  return await client.get(
    `/users/${userId}/restaurant-reviews/${encodeURIComponent(poiId)}`,
    {},
    {
      allowHttpStatus: [404],
      returnFullResponse: true,
      silent: true,
      skipAuthRedirect: true
    }
  );
};

export const UpsertMyRestaurantReview = async (userId, poiId, payload) => {
  return await client.put(`/users/${userId}/restaurant-reviews/${encodeURIComponent(poiId)}`, payload);
};

export const DeleteMyRestaurantReview = async (userId, poiId) => {
  return await client.delete(`/users/${userId}/restaurant-reviews/${encodeURIComponent(poiId)}`);
};
