// frontend/api/restaurants.js

import client from './client';
import { resolveRestaurantImage } from '../utils/restaurant-images';

function getCategoryLabel(category) {
  if (typeof category !== 'string' || !category.trim()) {
    return '其他';
  }

  const segments = category
    .split(';')
    .map((item) => item.trim())
    .filter(Boolean);

  return segments[segments.length - 1] || category.trim();
}

export const GetNearbyRestaurants = async (params) => {
  return await client.get('/restaurants/nearby', params);
};

export const SearchRestaurants = async (params) => {
  return await client.get('/restaurants/search', params);
};

export const mapApiRestaurantToCard = (item = {}) => {
  const name = item.name || '未命名餐厅';
  const category = getCategoryLabel(item.category);
  const distanceMeters = Number(item.distance || 0);
  const distanceText = distanceMeters >= 1000
    ? `${(distanceMeters / 1000).toFixed(1)}km`
    : `${Math.round(distanceMeters)}m`;
  const avgRating = Number.isFinite(Number(item.avgRating)) ? Number(item.avgRating) : null;
  const avgPerCapitaPrice = Number.isFinite(Number(item.avgPerCapitaPrice))
    ? Number(item.avgPerCapitaPrice)
    : null;
  const reviewCount = Number.isFinite(Number(item.reviewCount)) ? Number(item.reviewCount) : 0;
  const aiTags = Array.isArray(item.aiTags) ? item.aiTags.filter((tag) => typeof tag === 'string' && tag.trim()) : [];

  let priceLevel = 0;
  if (avgPerCapitaPrice !== null) {
    if (avgPerCapitaPrice <= 20) {
      priceLevel = 1;
    } else if (avgPerCapitaPrice <= 40) {
      priceLevel = 2;
    } else {
      priceLevel = 3;
    }
  }

  return {
    id: item.poiId || item.id || `${Date.now()}-${Math.random()}`,
    poiId: item.poiId || '',
    name,
    category,
    rawCategory: item.category || '其他',
    distance: distanceText,
    distanceValue: distanceMeters,
    avgRating,
    reviewCount,
    avgPerCapitaPrice,
    aiTags,
    rating: avgRating,
    tags: aiTags.slice(0, 3),
    priceLevel,
    image: resolveRestaurantImage({ name, category: item.category || category }),
    isBlacklisted: false,
    description: item.address || '暂无简介',
    address: item.address || '暂无地址',
    votes: { up: 0, down: 0 },
    isUserAdded: false
  };
};
