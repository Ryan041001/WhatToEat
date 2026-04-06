const app = getApp();

Page({
  data: {
    id: '',
    restaurant: null,
    loading: true,
    error: ''
  },

  onLoad(options) {
    const id = options && options.id ? options.id : '';
    this.setData({ id });
    this.loadDetail();
  },

  async loadDetail() {
    this.setData({ loading: true, error: '' });
    try {
      if (!this.data.id) {
        throw new Error('缺少餐厅ID');
      }

      await app.bootstrapRestaurants();
      const restaurant = app.getRestaurantById(this.data.id);

      if (!restaurant) {
        throw new Error('未找到该餐厅');
      }

      this.setData({ restaurant });
    } catch (error) {
      this.setData({ error: error.message || '加载失败' });
    } finally {
      this.setData({ loading: false });
    }
  },

  toggleBlacklist() {
    if (!this.data.restaurant) {
      return;
    }

    app.toggleBlacklist(this.data.restaurant.id);
    const next = app.getRestaurantById(this.data.restaurant.id);
    this.setData({ restaurant: next });

    wx.showToast({
      title: next && next.isBlacklisted ? '已拉黑' : '已取消拉黑',
      icon: 'none'
    });
  },

  goBack() {
    wx.navigateBack({
      fail: () => {
        wx.redirectTo({ url: '/pages/home/home' });
      }
    });
  }
});
