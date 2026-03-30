// pages/home/home.js
import { GetNearbyRestaurants, mapApiRestaurantToCard } from '../../api/restaurants.js'
const app = getApp()

Page({
  data: {
    totalCount: 0,
    activeCount: 0,
    topRated: [],
    shaking: false,
    shakeResult: null,
    usingRemoteData: false,
  },

  onShow() {
    this.updateData()
  },

  async updateData() {
    let restaurants = app.getRestaurants()
    let usingRemoteData = false

    try {
      const location = await this.getLocationOrDefault()
      const res = await GetNearbyRestaurants({
        longitude: location.longitude,
        latitude: location.latitude,
        radius: 1500,
        page: 1,
        size: 20
      })

      const remoteItems = (res?.data?.items || []).map(mapApiRestaurantToCard)
      if (remoteItems.length > 0) {
        app.globalData.restaurants = remoteItems
        restaurants = remoteItems
        usingRemoteData = true
      }
    } catch (err) {
      console.warn('首页拉取附近餐厅失败，使用本地数据兜底', err)
    }

    const actives = restaurants.filter(item => !item.isBlacklisted)
    const topRated = [...actives].sort((a, b) => b.rating - a.rating).slice(0, 3)
    const topRatedFormatted = topRated.map(item => ({
      ...item,
      priceText: '¥'.repeat(item.priceLevel || 1)
    }))

    this.setData({
      totalCount: restaurants.length,
      activeCount: actives.length,
      topRated: topRatedFormatted,
      usingRemoteData
    })
  },

  getLocationOrDefault() {
    return new Promise((resolve) => {
      wx.getLocation({
        type: 'gcj02',
        success: (loc) => {
          resolve({ longitude: loc.longitude, latitude: loc.latitude })
        },
        fail: () => {
          // 默认回退到学校附近坐标，保证 API 可用
          resolve({ longitude: 120.3502, latitude: 30.3154 })
        }
      })
    })
  },

  goToSpin() {
    wx.navigateTo({ url: '/pages/spin/spin' })
  },

  goToSwipe() {
    wx.navigateTo({ url: '/pages/swipe/swipe' })
  },
  
  goToMine() {
    wx.navigateTo({ url: '/pages/mine/mine' })
  },

  goToRestaurants() {
    wx.navigateTo({ url: '/pages/restaurants/restaurants' })
  },

  handleShake() {
    if (this.data.shaking) return

    const actives = app.getActiveRestaurants()
    if (actives.length === 0) {
      wx.showToast({
        title: '没有可选的餐厅',
        icon: 'none'
      })
      return
    }

    this.setData({ shaking: true })
    
    // Vibrate device
    wx.vibrateShort({ type: 'medium' })

    const pick = actives[Math.floor(Math.random() * actives.length)]
    
    setTimeout(() => {
      this.setData({
        shaking: false,
        shakeResult: pick.name
      })
    }, 800)
  },

  closeShakeResult() {
    this.setData({
      shakeResult: null
    })
  },
  
  noop() {
    // Prevent event bubbling
  },

  handleImageError(e) {
    const { index } = e.currentTarget.dataset
    const { topRated } = this.data
    if (topRated[index]) {
      // Fallback image handling could go here
    }
  }
})
