// pages/home/home.js
const app = getApp();

Page({
  data: {
    loading: false,
    error: '',
    totalCount: 0,
    activeCount: 0,
    blacklistedCount: 0,
    topRated: [],
    shakeResult: '',
    shaking: false
  },

  onLoad() {
    this.loadData();
  },

  onShow() {
    this.loadData();
  },

  // 加载数据
  async loadData() {
    this.setData({ loading: true, error: '' });
    try {
      await app.bootstrapRestaurants();
      const restaurants = app.getRestaurants();
      const actives = app.getActiveRestaurants();
      const topRated = [...actives]
        .sort((a, b) => b.rating - a.rating)
        .slice(0, 3)
        .map((r) => ({
          ...r,
          priceText: '¥'.repeat(r.priceLevel)
        }));

      this.setData({
        totalCount: restaurants.length,
        activeCount: actives.length,
        blacklistedCount: restaurants.length - actives.length,
        topRated
      });
    } catch (error) {
      this.setData({
        error: '餐厅数据暂时没加载出来，请稍后再试'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 跳转到大转盘
  goToSpin() {
    wx.navigateTo({
      url: '/pages/spin/spin'
    });
  },

  // 跳转到卡片滑选
  goToSwipe() {
    wx.navigateTo({
      url: '/pages/swipe/swipe'
    });
  },

  // 跳转到餐厅列表
  goToRestaurants() {
    wx.navigateTo({
      url: '/pages/restaurants/restaurants'
    });
  },

  // 跳转到我的
  goToMine() {
    wx.navigateTo({
      url: '/pages/mine/mine'
    });
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      return;
    }
    wx.navigateTo({
      url: `/pages/detail/detail?id=${id}`
    });
  },

  // 摇一摇
  handleShake() {
    if (this.data.shaking) return;

    const actives = app.getActiveRestaurants();
    if (actives.length === 0) {
      wx.showToast({
        title: '暂时没有可选餐厅',
        icon: 'none'
      });
      return;
    }

    this.setData({ shaking: true });

    // 触发震动反馈
    wx.vibrateShort({
      type: 'medium'
    });

    setTimeout(() => {
      const pick = actives[Math.floor(Math.random() * actives.length)];
      this.setData({
        shaking: false,
        shakeResult: pick.name
      });

      // 3秒后自动关闭
      setTimeout(() => {
        this.setData({ shakeResult: '' });
      }, 3000);
    }, 800);
  },

  // 关闭摇一摇结果
  closeShakeResult() {
    this.setData({ shakeResult: '' });
  }
})
