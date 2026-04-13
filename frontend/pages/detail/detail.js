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
        throw new Error('没有获取到餐厅信息');
      }

      await app.bootstrapRestaurants();
      const restaurant = app.getRestaurantById(this.data.id);

      if (!restaurant) {
        throw new Error('这家餐厅暂时找不到了');
      }

      this.setData({ restaurant });
    } catch (error) {
      this.setData({ error: error.message || '加载详情时出了点问题，请稍后再试' });
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
      title: next && next.isBlacklisted ? '已设为不感兴趣' : '已恢复展示',
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
