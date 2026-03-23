// pages/home/home.js
const app = getApp();

Page({
  data: {
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
  loadData() {
    const restaurants = app.getRestaurants();
    const actives = app.getActiveRestaurants();
    const topRated = [...actives]
      .sort((a, b) => b.rating - a.rating)
      .slice(0, 3)
      .map(r => ({
        ...r,
        priceText: '¥'.repeat(r.priceLevel)
      }));

    this.setData({
      totalCount: restaurants.length,
      activeCount: actives.length,
      blacklistedCount: restaurants.length - actives.length,
      topRated
    });
  },

  // 跳转到大转盘
  goToSpin() {
    wx.switchTab({
      url: '/pages/spin/spin'
    });
  },

  // 跳转到卡片滑选
  goToSwipe() {
    wx.switchTab({
      url: '/pages/swipe/swipe'
    });
  },

  // 跳转到餐厅列表
  goToRestaurants() {
    wx.switchTab({
      url: '/pages/restaurants/restaurants'
    });
  },

  // 跳转到我的
  goToMine() {
    wx.switchTab({
      url: '/pages/mine/mine'
    });
  },

  // 摇一摇
  handleShake() {
    if (this.data.shaking) return;

    const actives = app.getActiveRestaurants();
    if (actives.length === 0) {
      wx.showToast({
        title: '没有可选餐厅',
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
