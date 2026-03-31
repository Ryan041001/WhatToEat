// frontend/api/restaurants.js

import client from './client';

export const GetNearbyRestaurants = async (params) => {
  return await client.get('/restaurants/nearby', params);
};

export const SearchRestaurants = async (params) => {
  return await client.get('/restaurants/search', params);
};

export const mapApiRestaurantToCard = (item = {}) => {
  const name = item.name || '未命名餐厅';
  const distanceMeters = Number(item.distance || 0);
  const distanceText = distanceMeters >= 1000
    ? `${(distanceMeters / 1000).toFixed(1)}km`
    : `${Math.round(distanceMeters)}m`;

  return {
    id: item.poiId || `${Date.now()}-${Math.random()}`,
    name,
    category: (item.category || '其他').split(';')[0] || '其他',
    tags: [],
    rating: 4.0,
    priceLevel: 2,
    image: 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800',
    isBlacklisted: false,
    distance: distanceText,
    description: item.address || '暂无简介',
    address: item.address || '暂无地址',
    votes: { up: 0, down: 0 },
    isUserAdded: false,
    poiId: item.poiId || ''
  };
};
