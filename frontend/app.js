// app.js
import { GetNearbyRestaurants, mapApiRestaurantToCard } from './api/restaurants';
import { getMockRestaurants, getMockUser } from './mock/restaurants';

const RESTAURANT_CACHE_KEY = 'restaurants_cache';
const BLACKLIST_KEY = 'restaurant_blacklist_keys';
const TOKEN_KEY = 'token';
const USER_KEY = 'user_info';
const BROKEN_IMAGE_URL = 'https://images.unsplash.com/photo-1604908554167-6ca8f7c3f2f6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800';
const FIXED_IMAGE_URL = 'https://images.unsplash.com/photo-1529193591184-b1d58069ecdd?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800';

function extractRestaurantList(payload) {
	if (Array.isArray(payload)) {
		return payload;
	}

	const data = payload && payload.data ? payload.data : payload;
  if (data && Array.isArray(data.items)) {
	  return data.items;
  }

	if (Array.isArray(data)) {
		return data;
	}

	if (data && Array.isArray(data.records)) {
		return data.records;
	}

	if (data && Array.isArray(data.restaurants)) {
		return data.restaurants;
	}

	return [];
}

App({
	globalData: {
		restaurants: [],
		user: null,
		token: '',
		useMockData: false
	},

	onLaunch() {
		this.globalData.token = wx.getStorageSync(TOKEN_KEY) || '';
		this.globalData.user = wx.getStorageSync(USER_KEY) || null;

		const cachedRestaurants = (wx.getStorageSync(RESTAURANT_CACHE_KEY) || []).map((item) => {
			if (item && item.image === BROKEN_IMAGE_URL) {
				return {
					...item,
					image: FIXED_IMAGE_URL
				};
			}
			return item;
		});
		if (Array.isArray(cachedRestaurants) && cachedRestaurants.length > 0) {
			this.globalData.restaurants = this.applyBlacklistState(cachedRestaurants);
			wx.setStorageSync(RESTAURANT_CACHE_KEY, this.globalData.restaurants);
		}
	},

	getCurrentUser() {
		return this.globalData.user;
	},

	setAuth(token, user) {
		this.globalData.token = token || '';
		this.globalData.user = user || null;

		if (token) {
			wx.setStorageSync(TOKEN_KEY, token);
		} else {
			wx.removeStorageSync(TOKEN_KEY);
		}

		if (user) {
			wx.setStorageSync(USER_KEY, user);
		} else {
			wx.removeStorageSync(USER_KEY);
		}
	},

	clearAuth() {
		this.setAuth('', null);
	},

	async bootstrapRestaurants(options = {}) {
		  const { force = false, sort = 'distance' } = options;
		  if (!force && sort === 'distance' && this.globalData.restaurants.length >= 12) {
			return this.globalData.restaurants;
		}

		const params = {
			longitude: 120.1551,
			latitude: 30.2741,
			radius: 3000,
			page: 1,
			  size: 30,
			  sort
		};

		try {
			const response = await GetNearbyRestaurants(params);
			  const list = extractRestaurantList(response).map(mapApiRestaurantToCard);
			if (list.length === 0) {
				throw new Error('餐厅列表为空');
			}

			this.globalData.useMockData = false;
			this.globalData.restaurants = this.applyBlacklistState(list);
			wx.setStorageSync(RESTAURANT_CACHE_KEY, this.globalData.restaurants);
			return this.globalData.restaurants;
		} catch (error) {
			const fallback = this.applyBlacklistState(getMockRestaurants());
			this.globalData.useMockData = true;
			this.globalData.restaurants = fallback;
			wx.setStorageSync(RESTAURANT_CACHE_KEY, fallback);
			return fallback;
		}
	},

	getRestaurants() {
		return this.globalData.restaurants || [];
	},

	getActiveRestaurants() {
		return this.getRestaurants().filter((item) => !item.isBlacklisted);
	},

	getRestaurantById(id) {
		return this.getRestaurants().find((item) => String(item.id) === String(id));
	},

	applyBlacklistState(list) {
		const blacklistKeys = new Set(wx.getStorageSync(BLACKLIST_KEY) || []);
		return (list || []).map((item) => {
			const key = item.poiId || `local:${item.id}`;
			return {
				...item,
				isBlacklisted: blacklistKeys.has(key)
			};
		});
	},

	toggleBlacklist(id) {
		const restaurants = this.getRestaurants();
		const target = restaurants.find((item) => String(item.id) === String(id));
		if (!target) {
			return;
		}

		const targetKey = target.poiId || `local:${target.id}`;
		const blacklistKeys = new Set(wx.getStorageSync(BLACKLIST_KEY) || []);

		if (blacklistKeys.has(targetKey)) {
			blacklistKeys.delete(targetKey);
		} else {
			blacklistKeys.add(targetKey);
		}

		wx.setStorageSync(BLACKLIST_KEY, Array.from(blacklistKeys));
		this.globalData.restaurants = restaurants.map((item) => {
			if (String(item.id) !== String(id)) {
				return item;
			}
			return {
				...item,
				isBlacklisted: !item.isBlacklisted
			};
		});
		wx.setStorageSync(RESTAURANT_CACHE_KEY, this.globalData.restaurants);
	},

	createMockSession() {
		const mockToken = `mock-token-${Date.now()}`;
		const mockUser = getMockUser();
		this.setAuth(mockToken, mockUser);
		return { mockToken, mockUser };
	}
});
