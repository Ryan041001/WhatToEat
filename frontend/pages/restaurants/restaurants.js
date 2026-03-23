// pages/restaurants/restaurants.js
const app = getApp();

Page({
  data: {
    restaurants: [],
    filteredRestaurants: [],
    categories: ['川菜', '日料', '快餐', '烧烤', '米线', '面食', '韩餐', '西餐', '火锅', '小吃'],
    selectedCategory: '',
    selectedPrice: 0,
    selectedSort: 'rating',
    sortOptions: {
      rating: '按评分',
      distance: '按距离'
    },
    showSortModal: false
  },

  onShow() {
    this.loadData();
  },

  // 加载数据
  loadData() {
    const restaurants = app.getRestaurants().map(r => ({
      ...r,
      priceText: '¥'.repeat(r.priceLevel),
      distanceValue: this.parseDistance(r.distance)
    }));
    
    this.setData({ restaurants }, () => {
      this.filterRestaurants();
    });
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

    // 排序
    if (this.data.selectedSort === 'rating') {
      filtered.sort((a, b) => b.rating - a.rating);
    } else if (this.data.selectedSort === 'distance') {
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

  // 阻止冒泡
  stopPropagation() {},

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
    const restaurant = this.data.restaurants.find(r => r.id === id);
    
    if (!restaurant) return;

    wx.showModal({
      title: '提示',
      content: restaurant.isBlacklisted ? '确定要取消拉黑吗？' : '确定要拉黑这家餐厅吗？',
      success: (res) => {
        if (res.confirm) {
          app.toggleBlacklist(id);
          this.loadData();
          
          wx.showToast({
            title: restaurant.isBlacklisted ? '已取消拉黑' : '已拉黑',
            icon: 'success'
          });
        }
      }
    });
  }
})
