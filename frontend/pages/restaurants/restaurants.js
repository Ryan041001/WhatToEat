// pages/restaurants/restaurants.js
const app = getApp();

Page({
  data: {
    loading: false,
    error: '',
    restaurants: [],
    filteredRestaurants: [],
    categories: ['川菜', '日料', '快餐', '烧烤', '米线', '面食', '韩餐', '西餐', '火锅', '小吃'],
    selectedCategory: '',
    selectedPrice: 0,
    selectedSort: 'distance',
    sortOptions: {
      distance: '按距离',
      avgRating: '按评分',
      reviewCount: '按评论数',
      avgPriceAsc: '人均从低到高',
      avgPriceDesc: '人均从高到低',
      smart: '智能推荐'
    },
    showSortModal: false,
    heroCollapsed: false
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

  // 加载数据
  async loadData() {
    this.setData({ loading: true, error: '' });
    try {
      await app.bootstrapRestaurants({
        force: true,
        sort: this.data.selectedSort
      });
      const restaurants = app.getRestaurants().map((r) => ({
        ...r,
        displayRating: r.avgRating !== null && r.avgRating !== undefined ? Number(r.avgRating).toFixed(1) : '暂无评分',
        displayReviewCount: Number.isFinite(Number(r.reviewCount)) ? Number(r.reviewCount) : 0,
        displayAvgPerCapitaPrice: r.avgPerCapitaPrice !== null && r.avgPerCapitaPrice !== undefined
          ? `¥${r.avgPerCapitaPrice}`
          : '人均待补充',
        priceText: r.priceLevel > 0 ? '¥'.repeat(r.priceLevel) : '--',
        distanceValue: Number.isFinite(Number(r.distanceValue)) ? Number(r.distanceValue) : this.parseDistance(r.distance)
      }));

      this.setData({ restaurants }, () => {
        this.filterRestaurants();
      });
    } catch (error) {
      this.setData({ error: '餐厅列表暂时没加载出来，请稍后重试' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 解析距离（用于排序）
  parseDistance(distanceStr) {
    const match = distanceStr.match(/(\d+)/);
    return match ? parseInt(match[0]) : 999999;
  },

  // 筛选餐厅
  filterRestaurants() {
    let filtered = [...this.data.restaurants];

    // 按分类筛选
    if (this.data.selectedCategory) {
      filtered = filtered.filter(r => r.category === this.data.selectedCategory);
    }

    // 按价格筛选
    if (this.data.selectedPrice > 0) {
      filtered = filtered.filter(r => r.priceLevel === this.data.selectedPrice);
    }

    // 后端已经按 selectedSort 返回，此处仅保留 distance 兜底
    if (this.data.selectedSort === 'distance') {
      filtered.sort((a, b) => a.distanceValue - b.distanceValue);
    }

    this.setData({ filteredRestaurants: filtered });
  },

  // 选择分类
  selectCategory(e) {
    const category = e.currentTarget.dataset.category;
    this.setData({ selectedCategory: category }, () => {
      this.filterRestaurants();
    });
  },

  // 选择价格
  selectPrice(e) {
    const price = parseInt(e.currentTarget.dataset.price);
    this.setData({ selectedPrice: price }, () => {
      this.filterRestaurants();
    });
  },

  // 切换排序模态框
  toggleSort() {
    this.setData({ showSortModal: !this.data.showSortModal });
  },

  // 点击空白区域关闭排序菜单
  closeSortMenu() {
    if (!this.data.showSortModal) {
      return;
    }
    this.setData({ showSortModal: false });
  },

  // 阻止冒泡
  stopPropagation() {},

  openDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      return;
    }
    wx.navigateTo({
      url: `/pages/detail/detail?id=${id}`
    });
  },

  // 改变排序
  changeSort(e) {
    const sort = e.currentTarget.dataset.sort;
    this.setData({ 
      selectedSort: sort,
      showSortModal: false
    }, () => {
      this.filterRestaurants();
    });
  },

  // 切换拉黑状态
  toggleBlacklist(e) {
    const id = e.currentTarget.dataset.id;
    const restaurant = this.data.restaurants.find(r => String(r.id) === String(id));
    
    if (!restaurant) return;

    wx.showModal({
      title: '确认操作',
      content: restaurant.isBlacklisted ? '要把这家餐厅移出不感兴趣吗？' : '要暂时不看这家餐厅吗？',
      success: (res) => {
        if (res.confirm) {
          app.toggleBlacklist(id);
          this.loadData();
          
          wx.showToast({
            title: restaurant.isBlacklisted ? '已恢复展示' : '已设为不感兴趣',
            icon: 'success'
          });
        }
      }
    });
  }
})
