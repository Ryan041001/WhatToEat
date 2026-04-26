import { AddBlacklist, RemoveBlacklist } from '../../api/blacklist';
import { GetNearbyRestaurants, mapApiRestaurantToCard } from '../../api/restaurants';
import { buildCategoryOptions } from '../../utils/restaurant-filters';
import { extractRestaurantList } from '../../utils/restaurant-state';

const app = getApp();

const PRICE_PRESETS = [
  { key: 'all', label: '全部价位', min: null, max: null },
  { key: '0-20', label: '0-20', min: 0, max: 20 },
  { key: '21-40', label: '21-40', min: 21, max: 40 },
  { key: '41-60', label: '41-60', min: 41, max: 60 },
  { key: '60+', label: '60+', min: 60, max: null }
];

const SORT_OPTIONS = {
  distance: '按距离',
  avgRating: '按评分',
  reviewCount: '按评论数',
  avgPriceAsc: '人均从低到高',
  avgPriceDesc: '人均从高到低',
  smart: '智能推荐'
};

function enrichRestaurant(restaurant = {}) {
  return {
    ...restaurant,
    displayRating: restaurant.avgRating !== null && restaurant.avgRating !== undefined
      ? Number(restaurant.avgRating).toFixed(1)
      : '暂无评分',
    displayReviewCount: Number.isFinite(Number(restaurant.reviewCount)) ? Number(restaurant.reviewCount) : 0,
    displayAvgPerCapitaPrice: restaurant.avgPerCapitaPrice !== null && restaurant.avgPerCapitaPrice !== undefined
      ? `¥${restaurant.avgPerCapitaPrice}`
      : '人均待补充',
    priceText: restaurant.priceLevel > 0 ? '¥'.repeat(restaurant.priceLevel) : '--',
    distanceValue: Number.isFinite(Number(restaurant.distanceValue))
      ? Number(restaurant.distanceValue)
      : parseDistance(restaurant.distance)
  };
}

function parseDistance(distanceText) {
  const match = typeof distanceText === 'string' ? distanceText.match(/(\d+)/) : null;
  return match ? parseInt(match[0], 10) : 999999;
}

function parsePriceInput(value) {
  if (value === null || value === undefined || value === '') {
    return null;
  }

  const normalized = String(value).trim();
  if (!/^\d+$/.test(normalized)) {
    return NaN;
  }

  return Number(normalized);
}

Page({
  data: {
    loading: false,
    error: '',
    restaurants: [],
    categories: [],
    selectedCategory: '',
    selectedPricePreset: 'all',
    customPriceMin: '',
    customPriceMax: '',
    selectedSort: 'distance',
    sortOptions: SORT_OPTIONS,
    showSortModal: false,
    heroCollapsed: false,
    showAdvancedFilters: false,
    filteredSummaryText: '按真实分类、人均和排序筛选'
  },

  onPageScroll(e) {
    const shouldCollapse = (e && e.scrollTop ? e.scrollTop : 0) > 72;
    if (shouldCollapse !== this.data.heroCollapsed) {
      this.setData({ heroCollapsed: shouldCollapse });
    }
  },

  onShow() {
    this.loadData();
  },

  async loadData(options = {}) {
    const { scrollToTop = false } = options;
    this.setData({ loading: true, error: '', showSortModal: false });

    try {
      const location = await app.resolveCurrentLocation({
        forceRefresh: true
      });
      await app.loadBlacklistPoiIds();

      const params = {
        longitude: location.longitude,
        latitude: location.latitude,
        radius: 3000,
        page: 1,
        size: 30,
        sort: this.data.selectedSort
      };

      const priceRange = this.getActivePriceRange();
      if (this.data.selectedCategory) {
        params.category = this.data.selectedCategory;
      }
      if (priceRange.minPrice !== null) {
        params.minAvgPerCapitaPrice = priceRange.minPrice;
      }
      if (priceRange.maxPrice !== null) {
        params.maxAvgPerCapitaPrice = priceRange.maxPrice;
      }

      const response = await GetNearbyRestaurants(params);
      const restaurants = app
        .applyBlacklistState(extractRestaurantList(response).map(mapApiRestaurantToCard))
        .map(enrichRestaurant);
      const categories = buildCategoryOptions(
        restaurants,
        this.data.categories,
        this.data.selectedCategory
      );

      this.setData({
        restaurants,
        categories,
        filteredSummaryText: this.buildSummaryText()
      });

      if (scrollToTop && typeof wx.pageScrollTo === 'function') {
        wx.pageScrollTo({
          scrollTop: 0,
          duration: 180
        });
      }
    } catch (error) {
      this.setData({
        restaurants: [],
        categories: [],
        error: error && error.message ? error.message : '餐厅列表暂时没加载出来，请稍后重试'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  getActivePriceRange() {
    if (this.data.selectedPricePreset && this.data.selectedPricePreset !== 'custom') {
      const preset = PRICE_PRESETS.find((item) => item.key === this.data.selectedPricePreset) || PRICE_PRESETS[0];
      return {
        minPrice: preset.min,
        maxPrice: preset.max
      };
    }

    return {
      minPrice: parsePriceInput(this.data.customPriceMin),
      maxPrice: parsePriceInput(this.data.customPriceMax)
    };
  },

  buildSummaryText() {
    const parts = [];

    if (this.data.selectedCategory) {
      parts.push(this.data.selectedCategory);
    }

    if (this.data.selectedPricePreset && this.data.selectedPricePreset !== 'all') {
      const preset = PRICE_PRESETS.find((item) => item.key === this.data.selectedPricePreset);
      if (preset) {
        parts.push(`人均 ${preset.label}`);
      }
    } else if (this.data.selectedPricePreset === 'custom') {
      const minText = this.data.customPriceMin ? `¥${this.data.customPriceMin}` : '不限';
      const maxText = this.data.customPriceMax ? `¥${this.data.customPriceMax}` : '不限';
      parts.push(`人均 ${minText}-${maxText}`);
    }

    parts.push(SORT_OPTIONS[this.data.selectedSort] || '按距离');
    return parts.join(' · ');
  },

  selectCategory(e) {
    const category = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.category || '' : '';
    this.setData({ selectedCategory: category }, () => {
      this.loadData({ scrollToTop: true });
    });
  },

  selectPricePreset(e) {
    const preset = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.preset : 'all';
    if (!preset) {
      return;
    }

    this.setData({
      selectedPricePreset: preset,
      customPriceMin: '',
      customPriceMax: '',
      showAdvancedFilters: false
    }, () => {
      this.loadData({ scrollToTop: true });
    });
  },

  toggleAdvancedFilters() {
    this.setData({
      showAdvancedFilters: !this.data.showAdvancedFilters
    });
  },

  onCustomPriceMinInput(e) {
    this.setData({
      customPriceMin: (e && e.detail ? e.detail.value : '') || ''
    });
  },

  onCustomPriceMaxInput(e) {
    this.setData({
      customPriceMax: (e && e.detail ? e.detail.value : '') || ''
    });
  },

  applyCustomPrice() {
    const minPrice = parsePriceInput(this.data.customPriceMin);
    const maxPrice = parsePriceInput(this.data.customPriceMax);

    if (Number.isNaN(minPrice) || Number.isNaN(maxPrice)) {
      wx.showToast({
        title: '价格请输入非负整数',
        icon: 'none'
      });
      return;
    }

    if (minPrice !== null && maxPrice !== null && minPrice > maxPrice) {
      wx.showToast({
        title: '最低人均不能大于最高人均',
        icon: 'none'
      });
      return;
    }

    this.setData({
      selectedPricePreset: 'custom',
      showAdvancedFilters: true
    }, () => {
      this.loadData({ scrollToTop: true });
    });
  },

  resetCustomPrice() {
    this.setData({
      selectedPricePreset: 'all',
      customPriceMin: '',
      customPriceMax: '',
      showAdvancedFilters: false
    }, () => {
      this.loadData({ scrollToTop: true });
    });
  },

  toggleSort() {
    this.setData({ showSortModal: !this.data.showSortModal });
  },

  closeSortMenu() {
    if (!this.data.showSortModal) {
      return;
    }
    this.setData({ showSortModal: false });
  },

  stopPropagation() {},

  openDetail(e) {
    const id = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.id : '';
    if (!id) {
      return;
    }
    wx.navigateTo({
      url: `/pages/detail/detail?id=${id}`
    });
  },

  changeSort(e) {
    const sort = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.sort : '';
    if (!sort || sort === this.data.selectedSort) {
      this.setData({ showSortModal: false });
      return;
    }

    this.setData({
      selectedSort: sort,
      showSortModal: false
    }, () => {
      this.loadData({ scrollToTop: true });
    });
  },

  async toggleBlacklist(e) {
    const id = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.id : '';
    const restaurant = this.data.restaurants.find((item) => String(item.id) === String(id));

    if (!restaurant) {
      return;
    }

    const userId = app.getCurrentUserId();
    if (!userId) {
      wx.showToast({
        title: '请先登录后再操作',
        icon: 'none'
      });
      return;
    }

    const poiId = restaurant.poiId || restaurant.id;
    if (!poiId) {
      wx.showToast({
        title: '餐厅主键缺失，请刷新后重试',
        icon: 'none'
      });
      return;
    }

    wx.showModal({
      title: '确认操作',
      content: restaurant.isBlacklisted ? '要把这家餐厅移出不感兴趣吗？' : '要暂时不看这家餐厅吗？',
      success: async (res) => {
        if (!res.confirm) {
          return;
        }

        try {
          if (restaurant.isBlacklisted) {
            await RemoveBlacklist(userId, poiId);
            app.setBlacklistPoiIds(app.globalData.blacklistPoiIds.filter((item) => item !== String(poiId)));
          } else {
            await AddBlacklist(userId, {
              poiId,
              reason: ''
            });
            app.setBlacklistPoiIds([...app.globalData.blacklistPoiIds, String(poiId)]);
          }

          this.setData({
            restaurants: app.applyBlacklistState(this.data.restaurants).map(enrichRestaurant)
          });

          wx.showToast({
            title: restaurant.isBlacklisted ? '已恢复展示' : '已设为不感兴趣',
            icon: 'success'
          });
        } catch (error) {
          wx.showToast({
            title: error && error.message ? error.message : '操作失败，请稍后重试',
            icon: 'none'
          });
        }
      }
    });
  }
});
