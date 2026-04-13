// pages/spin/spin.js
const app = getApp();
const {
  attachDisplayNames,
  createSpinPlan,
  getAnglePerSlice,
  getSegmentColor,
  getSliceStartAngle
} = require('./spin-logic');

const WHEEL_POOL_SIZE = 10;
const REVEAL_DELAY = 180;
const SPIN_DURATION = 3200;

Page({
  data: {
    restaurants: [],
    isSpinning: false,
    showResult: false,
    result: null,
    resultAccent: '',
    currentRotation: 0,
    wheelReady: false,
    selectedIndex: -1
  },

  ctx: null,
  canvasWidth: 0,
  canvasHeight: 0,
  revealTimer: null,
  animationFrameId: null,
  animationTimer: null,

  onLoad() {
    // 页面候选池在 onShow 中刷新，确保返回时能拿到最新餐厅状态。
  },

  onShow() {
    this.loadData();
  },

  onHide() {
    this.clearRevealTimer();
    this.cancelSpinAnimation();
  },

  onUnload() {
    this.clearRevealTimer();
    this.cancelSpinAnimation();
  },

  clearRevealTimer() {
    if (this.revealTimer) {
      clearTimeout(this.revealTimer);
      this.revealTimer = null;
    }
  },

  cancelSpinAnimation() {
    if (this.animationFrameId !== null && typeof cancelAnimationFrame === 'function') {
      cancelAnimationFrame(this.animationFrameId);
    }

    if (this.animationTimer) {
      clearTimeout(this.animationTimer);
      this.animationTimer = null;
    }

    this.animationFrameId = null;
  },

  scheduleNextFrame(callback) {
    if (typeof requestAnimationFrame === 'function') {
      this.animationFrameId = requestAnimationFrame(callback);
      return;
    }

    this.animationTimer = setTimeout(callback, 16);
  },

  async loadData() {
    this.cancelSpinAnimation();
    this.clearRevealTimer();

    await app.bootstrapRestaurants();
    const allRestaurants = app.getActiveRestaurants();
    const restaurants = attachDisplayNames(this.pickRandomRestaurants(allRestaurants, WHEEL_POOL_SIZE));

    this.setData({
      restaurants,
      wheelReady: false,
      currentRotation: 0,
      result: null,
      resultAccent: '',
      showResult: false,
      isSpinning: false,
      selectedIndex: -1
    });

    if (restaurants.length === 0) {
      return;
    }

    wx.nextTick(() => {
      if (!this.ctx) {
        this.initCanvas();
      } else {
        this.refreshWheel();
      }
    });
  },

  pickRandomRestaurants(list = [], count = WHEEL_POOL_SIZE) {
    const source = Array.isArray(list) ? [...list] : [];
    if (source.length <= count) {
      return source;
    }

    for (let i = source.length - 1; i > 0; i -= 1) {
      const j = Math.floor(Math.random() * (i + 1));
      const temp = source[i];
      source[i] = source[j];
      source[j] = temp;
    }

    return source.slice(0, count);
  },

  initCanvas(onReady, retryCount = 0) {
    const query = wx.createSelectorQuery();
    query.select('.wheel-canvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (res[0]) {
          const canvas = res[0].node;
          const ctx = canvas.getContext('2d');
          const dpr = wx.getSystemInfoSync().pixelRatio;

          canvas.width = res[0].width * dpr;
          canvas.height = res[0].height * dpr;

          ctx.scale(dpr, dpr);

          this.ctx = ctx;
          this.canvasWidth = res[0].width;
          this.canvasHeight = res[0].height;

          this.refreshWheel();
          if (typeof onReady === 'function') {
            onReady();
          }
        } else if (retryCount < 3) {
          setTimeout(() => {
            this.initCanvas(onReady, retryCount + 1);
          }, 60);
        }
      });
  },

  refreshWheel() {
    if (!this.ctx || this.data.restaurants.length === 0) {
      return;
    }

    this.clearRevealTimer();
    this.drawWheel();

    if (!this.data.wheelReady) {
      this.revealTimer = setTimeout(() => {
        this.setData({ wheelReady: true });
        this.revealTimer = null;
      }, REVEAL_DELAY);
    }
  },

  drawWheel() {
    if (!this.ctx || this.data.restaurants.length === 0) {
      return;
    }

    const ctx = this.ctx;
    const width = this.canvasWidth;
    const height = this.canvasHeight;
    const centerX = width / 2;
    const centerY = height / 2;
    const radius = Math.min(width, height) / 2 - 16;
    const restaurants = this.data.restaurants;
    const anglePerSlice = getAnglePerSlice(restaurants.length);
    const rotation = this.data.currentRotation;
    const selectedIndex = this.data.selectedIndex;

    ctx.clearRect(0, 0, width, height);

    const plateGradient = ctx.createRadialGradient(centerX, centerY * 0.82, radius * 0.1, centerX, centerY, radius + 8);
    plateGradient.addColorStop(0, 'rgba(255, 251, 245, 0.98)');
    plateGradient.addColorStop(0.68, 'rgba(255, 231, 203, 0.96)');
    plateGradient.addColorStop(1, 'rgba(255, 204, 147, 0.92)');

    ctx.beginPath();
    ctx.arc(centerX, centerY, radius + 10, 0, Math.PI * 2);
    ctx.fillStyle = plateGradient;
    ctx.fill();

    restaurants.forEach((restaurant, index) => {
      const startAngle = getSliceStartAngle(restaurants.length, index, rotation);
      const endAngle = startAngle + anglePerSlice;
      const isSelected = index === selectedIndex;
      const baseColor = getSegmentColor(index);

      ctx.beginPath();
      ctx.moveTo(centerX, centerY);
      ctx.arc(centerX, centerY, radius, startAngle, endAngle);
      ctx.closePath();
      ctx.fillStyle = baseColor;
      ctx.fill();

      if (isSelected) {
        ctx.save();
        ctx.globalAlpha = 0.16;
        ctx.fillStyle = '#FFFFFF';
        ctx.fill();
        ctx.restore();
      }

      ctx.strokeStyle = isSelected ? 'rgba(255, 255, 255, 0.96)' : 'rgba(255, 255, 255, 0.78)';
      ctx.lineWidth = isSelected ? 5 : 3;
      ctx.stroke();

      ctx.save();
      ctx.translate(centerX, centerY);
      ctx.rotate(startAngle + anglePerSlice / 2);
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillStyle = '#FFFFFF';

      const fontSize = restaurants.length <= 6 ? 16 : restaurants.length <= 8 ? 14 : 12;
      const labelRadius = restaurants.length <= 6 ? radius * 0.62 : restaurants.length <= 8 ? radius * 0.66 : radius * 0.7;
      ctx.font = `700 ${fontSize}px sans-serif`;
      ctx.strokeStyle = 'rgba(120, 63, 26, 0.18)';
      ctx.lineWidth = 4;

      if (typeof ctx.strokeText === 'function') {
        ctx.strokeText(restaurant.displayName, labelRadius, 0);
      }
      ctx.fillText(restaurant.displayName, labelRadius, 0);
      ctx.restore();
    });

    ctx.beginPath();
    ctx.arc(centerX, centerY, 36, 0, Math.PI * 2);
    ctx.fillStyle = '#FFF8F1';
    ctx.fill();
    ctx.strokeStyle = '#FF8E53';
    ctx.lineWidth = 4;
    ctx.stroke();

    ctx.beginPath();
    ctx.arc(centerX, centerY, 18, 0, Math.PI * 2);
    ctx.fillStyle = '#FF8E53';
    ctx.fill();
  },

  handlePrimaryAction() {
    if (this.data.showResult) {
      this.handleSpinAgain();
      return;
    }

    this.handleSpin();
  },

  handleSpin() {
    if (this.data.isSpinning || this.data.restaurants.length === 0) {
      return;
    }

    if (!this.ctx) {
      this.initCanvas(() => {
        if (!this.data.wheelReady) {
          this.setData({ wheelReady: true }, () => {
            this.drawWheel();
            this.handleSpin();
          });
          return;
        }

        this.drawWheel();
        this.handleSpin();
      });
      return;
    }

    if (!this.data.wheelReady) {
      this.clearRevealTimer();
      this.setData({ wheelReady: true });
      this.drawWheel();
    }

    this.cancelSpinAnimation();

    const restaurants = this.data.restaurants;
    const winnerIndex = Math.floor(Math.random() * restaurants.length);
    const extraTurns = 5 + Math.floor(Math.random() * 2);
    const plan = createSpinPlan({
      count: restaurants.length,
      winnerIndex,
      currentRotation: this.data.currentRotation,
      extraTurns
    });
    const startTime = Date.now();

    this.setData({
      isSpinning: true,
      showResult: false,
      result: null,
      resultAccent: '',
      selectedIndex: -1
    });

    const easeOutQuart = (progress) => 1 - Math.pow(1 - progress, 4);

    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / SPIN_DURATION, 1);
      const eased = easeOutQuart(progress);
      const currentRotation = plan.startRotation + plan.totalRotation * eased;

      this.setData({ currentRotation });
      this.drawWheel();

      if (progress < 1) {
        this.scheduleNextFrame(animate);
        return;
      }

      this.cancelSpinAnimation();
      this.setData({
        currentRotation: plan.normalizedFinalRotation,
        isSpinning: false,
        showResult: true,
        selectedIndex: winnerIndex,
        result: restaurants[winnerIndex],
        resultAccent: getSegmentColor(winnerIndex)
      }, () => {
        this.drawWheel();
      });

      wx.vibrateShort({ type: 'heavy' });
    };

    animate();
  },

  handleSpinAgain() {
    if (this.data.isSpinning || this.data.restaurants.length === 0) {
      return;
    }

    this.setData({ showResult: false }, () => {
      this.handleSpin();
    });
  },

  closeResult() {
    this.setData({ showResult: false });
  }
});
