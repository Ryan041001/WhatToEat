// app.js
import { GetNearbyRestaurants, mapApiRestaurantToCard } from './api/restaurants';
import { getMockRestaurants, getMockUser } from './mock/restaurants';

const RESTAURANT_CACHE_KEY = 'restaurants_cache';
const BLACKLIST_KEY = 'restaurant_blacklist_keys';
const TOKEN_KEY = 'token';
const USER_KEY = 'user_info';

function extractRestaurantList(payload) {
	if (Array.isArray(payload)) {
		return payload;
	}

	const data = payload && payload.data ? payload.data : payload;
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

function mergeWithMockRestaurants(list = [], minCount = 12) {
	if (!Array.isArray(list)) {
		return [];
	}

	if (list.length >= minCount) {
		return list;
	}

	const existedKeys = new Set(list.map((item) => item.poiId || item.id));
	const mockExtras = getMockRestaurants().filter((item) => {
		const key = item.poiId || item.id;
		return !existedKeys.has(key);
	});

	return [...list, ...mockExtras].slice(0, minCount);
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

		const cachedRestaurants = wx.getStorageSync(RESTAURANT_CACHE_KEY) || [];
		if (Array.isArray(cachedRestaurants) && cachedRestaurants.length > 0) {
			this.globalData.restaurants = this.applyBlacklistState(cachedRestaurants);
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
		const { force = false } = options;
		if (!force && this.globalData.restaurants.length >= 12) {
			return this.globalData.restaurants;
		}

		const params = {
			longitude: 120.1551,
			latitude: 30.2741,
			radius: 3000,
			page: 1,
			pageSize: 30
		};

		try {
			const response = await GetNearbyRestaurants(params);
			const list = mergeWithMockRestaurants(
				extractRestaurantList(response).map(mapApiRestaurantToCard),
				12
			);
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
