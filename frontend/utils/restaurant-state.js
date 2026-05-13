export const DEFAULT_AVATAR_URL = 'https://placehold.co/200x200/F97316/FFFFFF?text=%E5%90%83';

export function unwrapData(payload) {
  if (!payload) {
    return null;
  }

  if (typeof payload === 'object' && payload.data !== undefined && !Array.isArray(payload)) {
    return payload.data;
  }

  return payload;
}

export function extractRestaurantList(payload) {
  if (Array.isArray(payload)) {
    return payload;
  }

  const data = unwrapData(payload);
  if (!data) {
    return [];
  }

  if (Array.isArray(data.items)) {
    return data.items;
  }

  if (Array.isArray(data.records)) {
    return data.records;
  }

  if (Array.isArray(data.restaurants)) {
    return data.restaurants;
  }

  if (Array.isArray(data)) {
    return data;
  }

  return [];
}

export function extractBlacklistPoiIds(payload) {
  const items = extractRestaurantList(payload);
  return items
    .map((item) => (item && item.poiId ? String(item.poiId) : ''))
    .filter(Boolean);
}

export function mergeBlacklistState(restaurants = [], blacklistPoiIds = []) {
  const poiIdSet = new Set((blacklistPoiIds || []).map((item) => String(item)));
  return (restaurants || []).map((restaurant) => {
    const poiId = restaurant && (restaurant.poiId || restaurant.id) ? String(restaurant.poiId || restaurant.id) : '';
    return {
      ...restaurant,
      isBlacklisted: poiId ? poiIdSet.has(poiId) : false
    };
  });
}

export function normalizeAuthUser(user) {
  if (!user || typeof user !== 'object') {
    return null;
  }

  const rawNickname = typeof user.nickname === 'string' && user.nickname.trim()
    ? user.nickname.trim()
    : typeof user.nickName === 'string' && user.nickName.trim()
      ? user.nickName.trim()
      : '微信用户';
  const rawAvatarUrl = typeof user.avatarUrl === 'string' && user.avatarUrl.trim()
    ? user.avatarUrl.trim()
    : typeof user.avatar === 'string' && user.avatar.trim()
      ? user.avatar.trim()
      : DEFAULT_AVATAR_URL;

  return {
    ...user,
    nickname: rawNickname,
    nickName: user.nickName || rawNickname,
    avatarUrl: rawAvatarUrl,
    avatar: user.avatar || rawAvatarUrl
  };
}
