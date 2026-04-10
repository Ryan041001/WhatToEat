// pages/mine/mine.js
const app = getApp();

Page({
  data: {
    userName: '游客',
    userBio: '登录后可同步你的偏好设置',
    totalCount: 0,
    userAddedCount: 0,
    activeCount: 0,
    userAdded: []
  },

  onShow() {
    this.loadData();
  },

  // 加载数据
  async loadData() {
    await app.bootstrapRestaurants();
    const restaurants = app.getRestaurants();
    const userAdded = restaurants.filter(r => r.isUserAdded);
    const actives = app.getActiveRestaurants();
    const user = app.getCurrentUser();

    this.setData({
      userName: (user && (user.nickname || user.nickName)) || '游客',
      userBio: app.globalData.useMockData ? '当前使用 Mock 数据模式' : '已连接后端接口',
      totalCount: restaurants.length,
      userAddedCount: userAdded.length,
      activeCount: actives.length,
      userAdded
    });
  },

  // 显示添加表单
  showAddForm() {
    wx.showToast({
      title: '可在列表页维护餐厅',
      icon: 'none'
    });
  },

  // 跳转到餐厅列表
  goToRestaurants() {
    wx.navigateTo({
      url: '/pages/restaurants/restaurants'
    });
  },

  // 跳转到设置
  goToSettings() {
    app.clearAuth();
    wx.showToast({
      title: '已退出登录',
      icon: 'success'
    });
    wx.redirectTo({ url: '/pages/index/index' });
  },

  // 跳转到关于
  goToAbout() {
    wx.showModal({
      title: '关于项目',
      content: '今天吃什么 v1.0.0\n微信小程序前端实践作业版本',
      showCancel: false
    });
  }
})
