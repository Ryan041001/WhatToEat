// app.js
import { GetNearbyRestaurants, mapApiRestaurantToCard } from './api/restaurants';
import { AddBlacklist, ListBlacklist, RemoveBlacklist } from './api/blacklist';
import {
	extractBlacklistPoiIds,
	extractRestaurantList,
	mergeBlacklistState,
	normalizeAuthUser
} from './utils/restaurant-state';

const RESTAURANT_CACHE_KEY = 'restaurants_cache';
const TOKEN_KEY = 'token';
const USER_KEY = 'user_info';
const LOCATION_CACHE_KEY = 'current_location_cache';
const BROKEN_IMAGE_URL = 'https://images.unsplash.com/photo-1604908554167-6ca8f7c3f2f6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800';
const FIXED_IMAGE_URL = 'https://images.unsplash.com/photo-1529193591184-b1d58069ecdd?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800';
const DEVTOOLS_FALLBACK_LOCATION = {
	longitude: 120.1551,
	latitude: 30.2741
};

function isWechatDevtools() {
	try {
		const info = wx.getSystemInfoSync();
		return info && info.platform === 'devtools';
	} catch (error) {
		return false;
	}
}

App({
	globalData: {
		restaurants: [],
		user: null,
		token: '',
		blacklistPoiIds: [],
		location: null
	},

	onLaunch() {
		this.globalData.token = wx.getStorageSync(TOKEN_KEY) || '';
		this.globalData.user = normalizeAuthUser(wx.getStorageSync(USER_KEY) || null);

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
			this.globalData.restaurants = cachedRestaurants;
			wx.setStorageSync(RESTAURANT_CACHE_KEY, this.globalData.restaurants);
		}

		const cachedLocation = wx.getStorageSync(LOCATION_CACHE_KEY) || null;
		if (cachedLocation && Number.isFinite(Number(cachedLocation.longitude)) && Number.isFinite(Number(cachedLocation.latitude))) {
			this.globalData.location = {
				longitude: Number(cachedLocation.longitude),
				latitude: Number(cachedLocation.latitude)
			};
		}
	},

	getCurrentUser() {
		return this.globalData.user;
	},

	getCurrentUserId() {
		const user = this.getCurrentUser();
		if (!user) {
			return null;
		}

		const userId = user.id || user.userId;
		return Number.isFinite(Number(userId)) ? Number(userId) : null;
	},

	setAuth(token, user) {
		const normalizedUser = normalizeAuthUser(user);
		this.globalData.token = token || '';
		this.globalData.user = normalizedUser;

		if (token) {
			wx.setStorageSync(TOKEN_KEY, token);
		} else {
			wx.removeStorageSync(TOKEN_KEY);
		}

		if (normalizedUser) {
			wx.setStorageSync(USER_KEY, normalizedUser);
		} else {
			wx.removeStorageSync(USER_KEY);
		}
	},

	clearAuth() {
		this.setAuth('', null);
		this.globalData.blacklistPoiIds = [];
	},

	cacheLocation(location) {
		if (!location) {
			return;
		}

		const normalized = {
			longitude: Number(location.longitude),
			latitude: Number(location.latitude)
		};
		this.globalData.location = normalized;
		wx.setStorageSync(LOCATION_CACHE_KEY, normalized);
	},

	async ensureLocationPermission() {
		const setting = await new Promise((resolve, reject) => {
			wx.getSetting({
				success: resolve,
				fail: reject
			});
		});

		const authSetting = setting && setting.authSetting ? setting.authSetting : {};
		const locationAuthorized = authSetting['scope.userLocation'];

		if (locationAuthorized === true) {
			return true;
		}

		if (locationAuthorized === false) {
			const modalResult = await new Promise((resolve) => {
				wx.showModal({
					title: '需要位置权限',
					content: '开启位置权限后，才能按你附近的位置推荐餐厅。',
					confirmText: '去开启',
					cancelText: '暂不',
					success: resolve,
					fail: () => resolve({ confirm: false, cancel: true })
				});
			});

			if (!modalResult || !modalResult.confirm) {
				throw new Error('未开启位置权限，请授权后再试');
			}

			const openResult = await new Promise((resolve, reject) => {
				wx.openSetting({
					success: resolve,
					fail: reject
				});
			});

			const nextAuthSetting = openResult && openResult.authSetting ? openResult.authSetting : {};
			if (nextAuthSetting['scope.userLocation']) {
				return true;
			}

			throw new Error('未开启位置权限，请授权后再试');
		}

		await new Promise((resolve, reject) => {
			wx.authorize({
				scope: 'scope.userLocation',
				success: resolve,
				fail: reject
			});
		});

		return true;
	},

	async resolveCurrentLocation(options = {}) {
		const {
			forceRefresh = false,
			allowDevtoolsFallback = true
		} = options;

		if (!forceRefresh && this.globalData.location) {
			return this.globalData.location;
		}

		try {
			await this.ensureLocationPermission();
			const location = await new Promise((resolve, reject) => {
				wx.getLocation({
					type: 'gcj02',
					isHighAccuracy: true,
					success: resolve,
					fail: reject
				});
			});

			const normalized = {
				longitude: Number(location.longitude),
				latitude: Number(location.latitude)
			};
			this.cacheLocation(normalized);
			return normalized;
		} catch (error) {
			if (this.globalData.location) {
				return this.globalData.location;
			}

			const cachedLocation = wx.getStorageSync(LOCATION_CACHE_KEY) || null;
			if (cachedLocation && Number.isFinite(Number(cachedLocation.longitude)) && Number.isFinite(Number(cachedLocation.latitude))) {
				const normalized = {
					longitude: Number(cachedLocation.longitude),
					latitude: Number(cachedLocation.latitude)
				};
				this.globalData.location = normalized;
				return normalized;
			}

			if (allowDevtoolsFallback && isWechatDevtools()) {
				return DEVTOOLS_FALLBACK_LOCATION;
			}

			throw new Error('未获取到当前位置，请打开定位权限后重试');
		}
	},

	cacheRestaurants(restaurants) {
		this.globalData.restaurants = restaurants || [];
		wx.setStorageSync(RESTAURANT_CACHE_KEY, this.globalData.restaurants);
	},

	setBlacklistPoiIds(poiIds = []) {
		this.globalData.blacklistPoiIds = Array.from(new Set((poiIds || []).map((item) => String(item))));
		if (this.globalData.restaurants.length > 0) {
			this.cacheRestaurants(mergeBlacklistState(this.globalData.restaurants, this.globalData.blacklistPoiIds));
		}
	},

	async loadBlacklistPoiIds() {
		const userId = this.getCurrentUserId();
		if (!userId) {
			this.setBlacklistPoiIds([]);
			return [];
		}

		const payload = await ListBlacklist(userId, { page: 1, size: 100 });
		const poiIds = extractBlacklistPoiIds(payload);
		this.setBlacklistPoiIds(poiIds);
		return poiIds;
	},

	upsertRestaurant(restaurant) {
		if (!restaurant || !restaurant.id) {
			return null;
		}

		const nextRestaurants = [...this.getRestaurants()];
		const index = nextRestaurants.findIndex((item) => {
			if (!item) {
				return false;
			}
			return String(item.id) === String(restaurant.id) || String(item.poiId || '') === String(restaurant.poiId || '');
		});

		if (index === -1) {
			nextRestaurants.push(restaurant);
		} else {
			nextRestaurants[index] = {
				...nextRestaurants[index],
				...restaurant
			};
		}

		const merged = mergeBlacklistState(nextRestaurants, this.globalData.blacklistPoiIds);
		this.cacheRestaurants(merged);
		return merged.find((item) => String(item.id) === String(restaurant.id) || String(item.poiId || '') === String(restaurant.poiId || '')) || null;
	},

	async bootstrapRestaurants(options = {}) {
		const { force = false, sort = 'distance' } = options;
		if (!force && sort === 'distance' && this.globalData.restaurants.length >= 12) {
			return this.globalData.restaurants;
		}

		const location = await this.resolveCurrentLocation();

		const params = {
			longitude: location.longitude,
			latitude: location.latitude,
			radius: 3000,
			page: 1,
			size: 30,
			sort
		};

		const [response, blacklistPoiIds] = await Promise.all([
			GetNearbyRestaurants(params),
			this.loadBlacklistPoiIds()
		]);
		const list = extractRestaurantList(response).map(mapApiRestaurantToCard);
		if (list.length === 0) {
			throw new Error('餐厅列表为空');
		}

		const merged = mergeBlacklistState(list, blacklistPoiIds);
		this.cacheRestaurants(merged);
		return this.globalData.restaurants;
	},

	getRestaurants() {
		return this.globalData.restaurants || [];
	},

	getActiveRestaurants() {
		return this.getRestaurants().filter((item) => !item.isBlacklisted);
	},

	getRestaurantById(id) {
		return this.getRestaurants().find((item) => {
			if (!item) {
				return false;
			}
			return String(item.id) === String(id) || String(item.poiId || '') === String(id);
		});
	},

	applyBlacklistState(list) {
		return mergeBlacklistState(list, this.globalData.blacklistPoiIds);
	},

	async toggleBlacklist(id) {
		const restaurants = this.getRestaurants();
		const target = restaurants.find((item) => String(item.id) === String(id));
		if (!target) {
			return null;
		}

		const userId = this.getCurrentUserId();
		if (!userId) {
			throw new Error('请先登录后再操作');
		}

		const poiId = target.poiId || target.id;
		if (!poiId) {
			throw new Error('餐厅主键缺失，请刷新后重试');
		}

		if (target.isBlacklisted) {
			await RemoveBlacklist(userId, poiId);
			this.setBlacklistPoiIds(this.globalData.blacklistPoiIds.filter((item) => item !== String(poiId)));
		} else {
			await AddBlacklist(userId, {
				poiId,
				reason: ''
			});
			this.setBlacklistPoiIds([...this.globalData.blacklistPoiIds, String(poiId)]);
		}

		return this.getRestaurantById(id);
	}
});
