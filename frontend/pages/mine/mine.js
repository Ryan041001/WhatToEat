// pages/mine/mine.js
import { Logout } from '../../api/auth';

const app = getApp();

Page({
  data: {
    userName: '游客',
    userAvatarUrl: '',
    userBio: '登录后可同步你的偏好设置',
    restaurantLoadError: '',
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
    const user = app.getCurrentUser();

    this.setData({
      userName: (user && (user.nickname || user.nickName)) || '游客',
      userAvatarUrl: (user && user.avatarUrl) || '',
      userBio: user ? '账号和餐厅信息已同步完成' : '登录后可同步你的偏好设置'
    });

    try {
      await app.bootstrapRestaurants();
      const restaurants = app.getRestaurants();
      const userAdded = restaurants.filter(r => r.isUserAdded);
      const actives = app.getActiveRestaurants();

      this.setData({
        restaurantLoadError: '',
        totalCount: restaurants.length,
        userAddedCount: userAdded.length,
        activeCount: actives.length,
        userAdded
      });
    } catch (error) {
      this.setData({
        restaurantLoadError: (error && error.message) || '餐厅数据暂时没加载出来',
        totalCount: 0,
        userAddedCount: 0,
        activeCount: 0,
        userAdded: []
      });
    }
  },

  // 显示添加表单
  showAddForm() {
    wx.showToast({
      title: '去餐厅列表即可添加和管理',
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
  async goToSettings() {
    try {
      await Logout();
    } catch (error) {}

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
      content: '今天吃什么 v1.0.0\n帮你更轻松地决定每一餐',
      showCancel: false
    });
  }
})
