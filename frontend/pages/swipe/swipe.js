// pages/swipe/swipe.js
const app = getApp();

Page({
  data: {
    restaurants: [],
    visibleCards: [],
    currentIndex: 0,
    likedRestaurants: [],
    showResult: false,
    finalResult: null,
    swipeDirection: '',
    touchStartX: 0,
    touchStartY: 0
  },

  onShow() {
    this.loadData();
  },

  // 加载数据
  async loadData() {
    await app.bootstrapRestaurants();
    const restaurants = app.getActiveRestaurants();
    this.setData({
      restaurants,
      currentIndex: 0,
      likedRestaurants: [],
      showResult: false,
      finalResult: null
    }, () => {
      this.updateVisibleCards();
    });
  },

  // 更新可见卡片
  updateVisibleCards() {
    const { restaurants, currentIndex } = this.data;
    const visibleCards = [];
    
    for (let i = 0; i < 3; i++) {
      const index = currentIndex + i;
      if (index < restaurants.length) {
        visibleCards.push({
          ...restaurants[index],
          style: ''
        });
      }
    }
    
    this.setData({ visibleCards });
  },

  // 触摸开始
  onTouchStart(e) {
    this.setData({
      touchStartX: e.touches[0].clientX,
      touchStartY: e.touches[0].clientY,
      swipeDirection: ''
    });
  },

  // 触摸移动
  onTouchMove(e) {
    const deltaX = e.touches[0].clientX - this.data.touchStartX;
    const deltaY = e.touches[0].clientY - this.data.touchStartY;
    
    // 判断方向
    if (Math.abs(deltaX) > Math.abs(deltaY)) {
      if (deltaX > 30) {
        this.setData({ swipeDirection: 'right' });
      } else if (deltaX < -30) {
        this.setData({ swipeDirection: 'left' });
      } else {
        this.setData({ swipeDirection: '' });
      }
    }
  },

  // 触摸结束
  onTouchEnd(e) {
    const deltaX = e.changedTouches[0].clientX - this.data.touchStartX;
    
    // 滑动距离超过阈值
    if (Math.abs(deltaX) > 100) {
      if (deltaX > 0) {
        this.handleLike();
      } else {
        this.handlePass();
      }
    } else {
      this.setData({ swipeDirection: '' });
    }
  },

  // 左滑 Pass
  handlePass() {
    this.nextCard(false);
  },

  // 右滑 Like
  handleLike() {
    this.nextCard(true);
  },

  // 下一张卡片
  nextCard(isLike) {
    const { currentIndex, restaurants, likedRestaurants } = this.data;
    
    if (currentIndex >= restaurants.length) return;
    
    // 记录喜欢的餐厅
    if (isLike) {
      likedRestaurants.push(restaurants[currentIndex]);
    }
    
    const newIndex = currentIndex + 1;
    
    // 检查是否完成
    if (newIndex >= restaurants.length) {
      this.showFinalResult();
    } else {
      this.setData({
        currentIndex: newIndex,
        likedRestaurants,
        swipeDirection: ''
      }, () => {
        this.updateVisibleCards();
      });
    }
  },

  // 显示最终结果
  showFinalResult() {
    const { likedRestaurants } = this.data;
    let finalResult = null;
    
    if (likedRestaurants.length > 0) {
      // 随机选择一个喜欢的餐厅
      const randomIndex = Math.floor(Math.random() * likedRestaurants.length);
      finalResult = likedRestaurants[randomIndex];
    }
    
    this.setData({
      showResult: true,
      finalResult
    });

    // 震动反馈
    wx.vibrateShort({ type: 'medium' });
  },

  // 重置
  reset() {
    this.loadData();
  }
})
