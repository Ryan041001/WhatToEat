// pages/swipe/swipe.js
const app = getApp();
const SWIPE_RESULT_CACHE_KEY = 'swipe_result_snapshot';

Page({
  shouldPreserveOnShow: false,

  data: {
    restaurants: [],
    visibleCards: [],
    currentIndex: 0,
    likedRestaurants: [],
    showResult: false,
    
    // 手势滑动的状态
    swipeDirection: '',
    touchStartX: 0,
    touchStartY: 0,
    translateX: 0,
    translateY: 0,
    rotation: 0,
    absX: 0,
    isSwiping: false
  },

  onShow() {
    const restored = this.consumeResultSnapshot();
    if (restored) {
      this.setData(restored);
      this.shouldPreserveOnShow = false;
      return;
    }

    if (this.shouldPreserveOnShow) {
      this.shouldPreserveOnShow = false;
      return;
    }
    this.loadData();
  },

  // 加载数据
  async loadData() {
    try {
      wx.removeStorageSync(SWIPE_RESULT_CACHE_KEY);
      await app.bootstrapRestaurants({
        force: true,
        forceLocationRefresh: true
      });
      const restaurants = app.getActiveRestaurants();
      this.setData({
        restaurants,
        currentIndex: 0,
        likedRestaurants: [],
        showResult: false
      }, () => {
        this.updateVisibleCards();
      });
    } catch (err) {
      console.error(err);
    }
  },

  // 结算按钮：直接展示结果
  handleSettle() {
    this.setData({ showResult: true });
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
    
    this.setData({ 
      visibleCards,
      translateX: 0,
      translateY: 0,
      rotation: 0,
      absX: 0,
      swipeDirection: ''
    });
  },

  // 触摸开始
  onTouchStart(e) {
    this.setData({
      touchStartX: e.touches[0].clientX,
      touchStartY: e.touches[0].clientY,
      swipeDirection: '',
      translateX: 0,
      translateY: 0,
      rotation: 0,
      absX: 0,
      isSwiping: true
    });
  },

  // 触摸移动
  onTouchMove(e) {
    const deltaX = e.touches[0].clientX - this.data.touchStartX;
    const deltaY = e.touches[0].clientY - this.data.touchStartY;
    
    const absX = Math.abs(deltaX);
    const rotation = deltaX * 0.05; // 生成一定的旋转角度跟手
    
    let swipeDirection = '';
    if (absX > Math.abs(deltaY)) {
      if (deltaX > 20) {
        swipeDirection = 'right';
      } else if (deltaX < -20) {
        swipeDirection = 'left';
      }
    }

    this.setData({
      translateX: deltaX,
      translateY: deltaY,
      rotation: rotation,
      absX: absX,
      swipeDirection
    });
  },

  // 触摸结束
  onTouchEnd(e) {
    const deltaX = e.changedTouches[0].clientX - this.data.touchStartX;
    
    this.setData({ isSwiping: false });

    // 滑动距离超过阈值 (设置100px为阈值)
    if (Math.abs(deltaX) > 100) {
      if (deltaX > 0) {
        this.handleLike();
      } else {
        this.handlePass();
      }
    } else {
      // 没有超过阈值，回弹
      this.setData({ 
        translateX: 0, 
        translateY: 0, 
        rotation: 0, 
        absX: 0,
        swipeDirection: '' 
      });
    }
  },

  // 左滑 Pass
  handlePass() {
    this.animateCard(-300, false);
  },

  // 右滑 Like
  handleLike() {
    this.animateCard(300, true);
  },

  // 卡片飞出动画后进入下一张
  animateCard(moveX, isLike) {
    this.setData({
      translateX: moveX,
      translateY: moveX * 0.2, // 飞出时附带一些往下的分量
      rotation: moveX * 0.1,
      absX: Math.abs(moveX),
      swipeDirection: isLike ? 'right' : 'left'
    });

    // 等待飞出动画完成，由于 wxss 中的 transition 一般是 0.3s，我们这里给 300ms
    setTimeout(() => {
      this.nextCard(isLike);
    }, 280);
  },

  // 下一张卡片
  nextCard(isLike) {
    const { currentIndex, restaurants, likedRestaurants } = this.data;
    
    if (currentIndex >= restaurants.length) return;
    
    // 记录喜欢的餐厅
    const nextLikedRestaurants = isLike
      ? [...likedRestaurants, restaurants[currentIndex]]
      : likedRestaurants;
    
    const newIndex = currentIndex + 1;
    
    // 检查是否完成
    if (newIndex >= restaurants.length) {
      this.showFinalResult(nextLikedRestaurants);
    } else {
      this.setData({
        currentIndex: newIndex,
        likedRestaurants: nextLikedRestaurants,
      }, () => {
        this.updateVisibleCards();
      });
    }
  },

  // 显示最终结果
  showFinalResult(likedRestaurants = []) {
    this.setData({
      showResult: true,
      likedRestaurants
    });

    // 震动反馈
    wx.vibrateShort({ type: 'medium' });
  },

  saveResultSnapshot() {
    if (!this.data.showResult) {
      return;
    }

    wx.setStorageSync(SWIPE_RESULT_CACHE_KEY, {
      showResult: true,
      likedRestaurants: this.data.likedRestaurants || [],
      restaurants: this.data.restaurants || [],
      visibleCards: [],
      currentIndex: this.data.currentIndex,
      translateX: 0,
      translateY: 0,
      rotation: 0,
      absX: 0,
      swipeDirection: '',
      isSwiping: false
    });
  },

  consumeResultSnapshot() {
    const snapshot = wx.getStorageSync(SWIPE_RESULT_CACHE_KEY);
    if (!snapshot || !snapshot.showResult) {
      return null;
    }

    wx.removeStorageSync(SWIPE_RESULT_CACHE_KEY);
    return snapshot;
  },

  openDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      return;
    }

    this.saveResultSnapshot();
    this.shouldPreserveOnShow = true;

    wx.navigateTo({
      url: `/pages/detail/detail?id=${id}`
    });
  },

  // 重置
  reset() {
    this.loadData();
  }
})
