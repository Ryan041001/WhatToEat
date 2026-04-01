// pages/swipe/swipe.js
const app = getApp();

Page({
  data: {
    cardQueue: [],
    liked: [],
    history: [],
    showMatch: null,
    
    // Swipe states
    visibleCardsReverse: [],
    isDragging: false,
    isAnimating: false,
    topCardStyle: '',
    likeOpacity: 0,
    nopeOpacity: 0,
  },

  startX: 0,
  startY: 0,
  currentX: 0,
  startTime: 0,
  matchTimer: null,

  onLoad() {
    this.buildQueue();
  },

  onShow() {
    // If empty after loading from onShow, rebuild. Optional, but wait, usually we only build once per access
    if (this.data.cardQueue.length === 0 && this.data.history.length === 0) {
      this.buildQueue();
    }
  },

  onHide() {
    if (this.matchTimer) clearTimeout(this.matchTimer);
  },

  onUnload() {
    if (this.matchTimer) clearTimeout(this.matchTimer);
  },

  goBack() {
    if (getCurrentPages().length > 1) {
      wx.navigateBack();
      return;
    }
    wx.switchTab({ url: '/pages/home/home' });
  },

  buildQueue() {
    const actives = app.getActiveRestaurants() || [];
    // Shuffle array
    const shuffled = [...actives].sort(() => Math.random() - 0.5);
    this.setData({ 
      cardQueue: shuffled,
      liked: [],
      history: []
    }, () => {
      this.updateVisibleCards();
    });
  },

  updateVisibleCards() {
    const { cardQueue } = this.data;
    const visibleCards = cardQueue.slice(0, 3);
    
    // We reverse it to mirror React's AnimatePresence logic where top card is mapped last so it sits on top in standard DOM without z-index (but here we also use z-index)
    // Actually we map in reverse to maintain index logic
    const visibleCardsReverse = visibleCards.map((r, reverseIdx) => {
      const stackIndex = visibleCards.length - 1 - reverseIdx;
      return {
        ...r,
        priceStr: '¥'.repeat(r.priceLevel),
        tags4: r.tags ? r.tags.slice(0, 4) : [],
        isTop: stackIndex === 0,
        stackIndex,
        index: reverseIdx // original position in filtered array
      };
    }).reverse();

    this.setData({ 
      visibleCardsReverse,
      topCardStyle: 'transform: translateX(0px) rotate(0deg);',
      likeOpacity: 0,
      nopeOpacity: 0,
      isDragging: false,
      isAnimating: false
    });
  },

  // Gesture events for top card
  onTouchStart(e) {
    if (this.data.isAnimating) return;
    this.startX = e.touches[0].clientX;
    this.startY = e.touches[0].clientY;
    this.currentX = 0;
    this.startTime = Date.now();
    this.setData({ isDragging: true });
  },

  onTouchMove(e) {
    if (!this.data.isDragging || this.data.isAnimating) return;
    const x = e.touches[0].clientX;
    const deltaX = x - this.startX;
    this.currentX = deltaX;

    const rotate = (deltaX / 200) * 20; // max ~20deg rotation
    const topCardStyle = `transform: translateX(${deltaX}px) rotate(${rotate}deg);`;

    let likeOpacity = 0;
    let nopeOpacity = 0;

    if (deltaX > 30) {
      likeOpacity = Math.min((deltaX - 30) / 70, 1);
    } else if (deltaX < -30) {
      nopeOpacity = Math.min((-deltaX - 30) / 70, 1);
    }

    this.setData({
      topCardStyle,
      likeOpacity,
      nopeOpacity
    });
  },

  onTouchEnd(e) {
    if (!this.data.isDragging || this.data.isAnimating) return;
    const deltaX = this.currentX;
    const duration = Date.now() - this.startTime;
    const velocity = Math.abs(deltaX) / duration; // px per ms, crude

    const threshold = 100;
    const velThreshold = 0.6; // approx

    this.setData({ isDragging: false, isAnimating: true });

    if (deltaX > threshold || (deltaX > 20 && velocity > velThreshold)) {
      this.animateOutAndSwipe('right');
    } else if (deltaX < -threshold || (deltaX < -20 && velocity > velThreshold)) {
      this.animateOutAndSwipe('left');
    } else {
      // Snap back
      this.setData({
        topCardStyle: 'transform: translateX(0px) rotate(0deg);',
        likeOpacity: 0,
        nopeOpacity: 0
      });
      setTimeout(() => {
        this.setData({ isAnimating: false });
      }, 300);
    }
  },

  animateOutAndSwipe(dir) {
    const endX = dir === 'right' ? 500 : -500;
    const rotate = dir === 'right' ? 30 : -30;
    
    this.setData({
      topCardStyle: `transform: translateX(${endX}px) rotate(${rotate}deg); opacity: 0;`,
      likeOpacity: dir === 'right' ? 1 : 0,
      nopeOpacity: dir === 'left' ? 1 : 0
    });

    setTimeout(() => {
      this.handleSwipe(dir);
    }, 300);
  },

  btnSwipeLeft() {
    if (this.data.isAnimating || this.data.cardQueue.length === 0) return;
    this.setData({ isAnimating: true });
    this.animateOutAndSwipe('left');
  },

  btnSwipeRight() {
    if (this.data.isAnimating || this.data.cardQueue.length === 0) return;
    this.setData({ isAnimating: true });
    this.animateOutAndSwipe('right');
  },

  handleSwipe(dir) {
    const { cardQueue, history, liked } = this.data;
    if (cardQueue.length === 0) return;

    const current = cardQueue[0];
    const newCardQueue = cardQueue.slice(1);
    const newHistory = [...history, { r: current, dir }];
    let newLiked = liked;

    if (dir === 'right') {
      app.voteRestaurant(current.id, 'up');
      newLiked = [...liked, current];
      
      if (this.matchTimer) clearTimeout(this.matchTimer);
      this.setData({ showMatch: current });
      this.matchTimer = setTimeout(() => this.setData({ showMatch: null }), 2000);
    } else {
      app.voteRestaurant(current.id, 'down');
    }

    this.setData({
      cardQueue: newCardQueue,
      history: newHistory,
      liked: newLiked
    }, () => {
      this.updateVisibleCards();
    });
  },

  handleUndo() {
    const { history, cardQueue, liked } = this.data;
    if (history.length === 0 || this.data.isAnimating) return;

    const last = history[history.length - 1];
    const newHistory = history.slice(0, -1);
    const newCardQueue = [last.r, ...cardQueue];
    let newLiked = liked;

    if (last.dir === 'right') {
      newLiked = liked.filter(r => r.id !== last.r.id);
    }

    this.setData({
      history: newHistory,
      cardQueue: newCardQueue,
      liked: newLiked
    }, () => {
      this.updateVisibleCards();
    });
  },

  handleReset() {
    this.buildQueue();
  }

});