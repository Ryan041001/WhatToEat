// pages/restaurants/restaurants.js
import { GetNearbyRestaurants, SearchRestaurants, mapApiRestaurantToCard } from '../../api/restaurants.js'
const app = getApp()

const CATEGORIES = ['全部', '川菜', '日料', '快餐', '烧烤', '米线', '面食', '韩餐', '西餐', '北方面食', '其他'];
const PRICE_FILTERS = ['全部', '¥', '¥¥', '¥¥¥'];

Page({
  data: {
    restaurants: [],
    filtered: [],
    search: '',
    activeCategory: '全部',
    activePriceFilter: '全部',
    showBlacklisted: false,
    activeCount: 0,
    blacklistCount: 0,
    page: 1,
    size: 20,
    total: 0,
    hasMore: false,
    isLoading: false,
    usingRemoteData: false,
    CATEGORIES,
    PRICE_FILTERS,
    
    // UI states per restaurant
    menuStates: {}, // id -> boolean
    voteStates: {}  // id -> 'up' | 'down' | null
  },

  onShow() {
    this.reloadFirstPage();
  },

  reloadFirstPage() {
    this.loadData({ page: 1, append: false });
  },

  enrichRestaurant(r) {
    return {
      ...r,
      priceText: '¥'.repeat(r.priceLevel || 1),
      votes: r.votes || { up: 0, down: 0 }
    };
  },

  async loadData(options = {}) {
    if (this.data.isLoading) return;

    const { size, search } = this.data;
    const page = options.page || this.data.page || 1;
    const append = !!options.append;

    this.setData({ isLoading: true });

    let usingRemoteData = false;
    let allRestaurants = [];
    let total = 0;
    let hasMore = false;

    try {
      const location = await this.getLocationOrDefault();
      const params = {
        longitude: location.longitude,
        latitude: location.latitude,
        radius: 1500,
        page,
        size
      };

      const response = search
        ? await SearchRestaurants({ ...params, keyword: search })
        : await GetNearbyRestaurants(params);

      const items = (response?.data?.items || []).map(mapApiRestaurantToCard);
      const mapped = items.map(r => this.enrichRestaurant(r));
      total = Number(response?.data?.total || mapped.length);

      allRestaurants = append
        ? this.mergeById(this.data.restaurants, mapped)
        : mapped;

      usingRemoteData = true;
      hasMore = allRestaurants.length < total && mapped.length > 0;

      if (!append && mapped.length > 0) {
        app.globalData.restaurants = mapped;
      }
    } catch (err) {
      if (err && err.code === 3003) {
        // 远端明确无结果，显示空列表，不回退本地数据
        allRestaurants = [];
        total = 0;
        usingRemoteData = true;
        hasMore = false;
      } else {
        console.warn('餐厅页拉取远端数据失败，使用本地数据兜底', err);
      }
    }

    if (!usingRemoteData && allRestaurants.length === 0) {
      allRestaurants = (app.getRestaurants() || []).map(r => this.enrichRestaurant(r));
      total = allRestaurants.length;
      hasMore = false;
    }

    this.setData({
      page,
      total,
      hasMore,
      isLoading: false,
      restaurants: allRestaurants,
      usingRemoteData
    }, () => {
      this.filterData();
    });
  },

  mergeById(prev = [], next = []) {
    const map = new Map();
    [...prev, ...next].forEach((item) => {
      map.set(String(item.id), item);
    });
    return Array.from(map.values());
  },

  loadMore() {
    if (!this.data.usingRemoteData || !this.data.hasMore || this.data.isLoading) return;
    this.loadData({ page: this.data.page + 1, append: true });
  },

  onSearchInput(e) {
    this.setData({ search: e.detail.value }, () => {
      this.reloadFirstPage()
    })
  },

  getLocationOrDefault() {
    return new Promise((resolve) => {
      wx.getLocation({
        type: 'gcj02',
        success: (loc) => {
          resolve({ longitude: loc.longitude, latitude: loc.latitude });
        },
        fail: () => {
          resolve({ longitude: 120.3502, latitude: 30.3154 });
        }
      });
    });
  },

  setCategory(e) {
    this.setData({ activeCategory: e.currentTarget.dataset.val }, () => {
      this.filterData()
    })
  },

  setPriceFilter(e) {
    this.setData({ activePriceFilter: e.currentTarget.dataset.val }, () => {
      this.filterData()
    })
  },

  toggleShowBlacklisted() {
    this.setData({ showBlacklisted: !this.data.showBlacklisted }, () => {
      this.filterData()
    })
  },

  filterData() {
    const { restaurants, search, activeCategory, activePriceFilter, showBlacklisted } = this.data;
    
    const filtered = restaurants.filter(r => {
      if (!showBlacklisted && r.isBlacklisted) return false;
      if (activeCategory !== '全部' && r.category !== activeCategory) return false;
      
      if (activePriceFilter !== '全部') {
        const level = activePriceFilter.length; // '¥' -> 1, '¥¥' -> 2
        if (r.priceLevel !== level) return false;
      }
      
      if (search) {
        const q = search.toLowerCase();
        return (
          r.name.toLowerCase().includes(q) ||
          r.category.toLowerCase().includes(q) ||
          (r.tags && r.tags.some(t => t.toLowerCase().includes(q))) ||
          (r.description && r.description.toLowerCase().includes(q))
        );
      }
      return true;
    });

    const activeCount = restaurants.filter(r => !r.isBlacklisted).length;
    const blacklistCount = restaurants.filter(r => r.isBlacklisted).length;

    this.setData({
      filtered,
      activeCount,
      blacklistCount
    });
  },

  goToHome() {
    if (getCurrentPages().length > 1) {
      wx.navigateBack();
      return;
    }
    wx.redirectTo({ url: '/pages/home/home' });
  },

  toggleMenu(e) {
    const id = e.currentTarget.dataset.id;
    const { menuStates } = this.data;
    this.setData({
      [`menuStates.${id}`]: !menuStates[id]
    });
  },

  handleVote(e) {
    const { id, type } = e.currentTarget.dataset;
    const { voteStates } = this.data;
    if (voteStates[id]) return; // already voted

    // Mock voting:
    // In real app, call app.voteRestaurant(id, type)
    let upAdded = type === 'up' ? 1 : 0;
    let downAdded = type === 'down' ? 1 : 0;

    const newFiltered = this.data.filtered.map(r => {
      if (r.id === id) {
        return {
          ...r,
          votes: {
            up: (r.votes?.up || 0) + upAdded,
            down: (r.votes?.down || 0) + downAdded
          }
        }
      }
      return r;
    });
    
    // Also update main list
    const newRestaurants = this.data.restaurants.map(r => {
      if (r.id === id) {
        return {
          ...r,
          votes: {
            up: (r.votes?.up || 0) + upAdded,
            down: (r.votes?.down || 0) + downAdded
          }
        }
      }
      return r;
    });
    
    // Actually you should persist to app.js
    if (app.voteRestaurant) {
      app.voteRestaurant(id, type);
    }
    
    this.setData({
      [`voteStates.${id}`]: type,
      filtered: newFiltered,
      restaurants: newRestaurants
    });
  },

  toggleBlacklist(e) {
    const id = e.currentTarget.dataset.id;
    
    if (app.toggleBlacklist) {
      app.toggleBlacklist(id);
      this.reloadFirstPage();
    } else {
      // Mock toggle
      const newRestaurants = this.data.restaurants.map(r => {
        if (r.id === id) return { ...r, isBlacklisted: !r.isBlacklisted }
        return r;
      });
      this.setData({ restaurants: newRestaurants }, () => this.filterData());
    }
  },

  deleteRestaurant(e) {
    const id = e.currentTarget.dataset.id;
    if (app.deleteRestaurant) {
      app.deleteRestaurant(id);
      this.reloadFirstPage();
    } else {
      const newRestaurants = this.data.restaurants.filter(r => r.id !== id);
      this.setData({ restaurants: newRestaurants }, () => this.filterData());
    }
  }
})
