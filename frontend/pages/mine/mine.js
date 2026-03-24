// pages/mine/mine.js
const app = getApp();

Page({
  data: {
    totalCount: 0,
    userAddedCount: 0,
    activeCount: 0,
    userAdded: []
  },

  onShow() {
    this.loadData();
  },

  // 加载数据
  loadData() {
    const restaurants = app.getRestaurants();
    const userAdded = restaurants.filter(r => r.isUserAdded);
    const actives = app.getActiveRestaurants();

    this.setData({
      totalCount: restaurants.length,
      userAddedCount: userAdded.length,
      activeCount: actives.length,
      userAdded
    });
  },

  // 显示添加表单
  showAddForm() {
    wx.navigateTo({
      url: '/pages/add-restaurant/add-restaurant'
    });
  },

  // 跳转到餐厅列表
  goToRestaurants() {
    wx.switchTab({
      url: '/pages/restaurants/restaurants'
    });
  },

  // 跳转到设置
  goToSettings() {
    wx.navigateTo({
      url: '/pages/settings/settings'
    });
  },

  // 跳转到关于
  goToAbout() {
    wx.navigateTo({
      url: '/pages/about/about'
    });
  }
})
