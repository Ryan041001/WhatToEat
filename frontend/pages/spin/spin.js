// pages/spin/spin.js
const app = getApp();

Page({
  data: {
    restaurants: [],
    isSpinning: false,
    showResult: false,
    result: null,
    currentRotation: 0
  },

  ctx: null,
  canvasWidth: 0,
  canvasHeight: 0,

  onLoad() {
    this.loadData();
    this.initCanvas();
  },

  onShow() {
    this.loadData();
    if (this.ctx) {
      this.drawWheel();
    }
  },

  // 加载数据
  async loadData() {
    await app.bootstrapRestaurants();
    const restaurants = app.getActiveRestaurants();
    this.setData({ restaurants });
    if (this.ctx) {
      this.drawWheel();
    }
  },

  // 初始化Canvas
  initCanvas() {
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
          
          this.drawWheel();
        }
      });
  },

  // 绘制转盘
  drawWheel() {
    if (!this.ctx || this.data.restaurants.length === 0) return;

    const ctx = this.ctx;
    const width = this.canvasWidth;
    const height = this.canvasHeight;
    const centerX = width / 2;
    const centerY = height / 2;
    const radius = Math.min(width, height) / 2 - 10;
    const restaurants = this.data.restaurants;
    const anglePerSlice = (Math.PI * 2) / restaurants.length;

    // 清空画布
    ctx.clearRect(0, 0, width, height);

    // 绘制扇形
    restaurants.forEach((restaurant, index) => {
      const startAngle = anglePerSlice * index + this.data.currentRotation;
      const endAngle = startAngle + anglePerSlice;

      // 扇形颜色
      const colors = ['#F97316', '#4361EE', '#84A98C', '#FFB84D', '#E63946', '#457B9D'];
      ctx.fillStyle = colors[index % colors.length];

      // 绘制扇形
      ctx.beginPath();
      ctx.moveTo(centerX, centerY);
      ctx.arc(centerX, centerY, radius, startAngle, endAngle);
      ctx.closePath();
      ctx.fill();

      // 绘制边框
      ctx.strokeStyle = '#fff';
      ctx.lineWidth = 3;
      ctx.stroke();

      // 绘制文字
      ctx.save();
      ctx.translate(centerX, centerY);
      ctx.rotate(startAngle + anglePerSlice / 2);
      ctx.textAlign = 'center';
      ctx.fillStyle = '#fff';
      ctx.font = 'bold 14px sans-serif';
      
      const text = restaurant.name.length > 6 ? restaurant.name.substring(0, 6) + '...' : restaurant.name;
      ctx.fillText(text, radius * 0.65, 0);
      ctx.restore();
    });

    // 绘制中心圆
    ctx.beginPath();
    ctx.arc(centerX, centerY, 30, 0, Math.PI * 2);
    ctx.fillStyle = '#fff';
    ctx.fill();
    ctx.strokeStyle = '#F97316';
    ctx.lineWidth = 3;
    ctx.stroke();
  },

  // 开始转动
  handleSpin() {
    if (this.data.isSpinning || this.data.restaurants.length === 0) return;

    this.setData({ isSpinning: true });

    // 随机选择一个餐厅
    const restaurants = this.data.restaurants;
    const winnerIndex = Math.floor(Math.random() * restaurants.length);
    const anglePerSlice = (Math.PI * 2) / restaurants.length;
    
    // 计算目标角度（多转几圈 + 随机角度）
    const extraRotations = 5;
    const targetAngle = extraRotations * Math.PI * 2 + (Math.PI * 2 - anglePerSlice * winnerIndex - anglePerSlice / 2);

    // 动画参数
    const duration = 3000;
    const startTime = Date.now();
    const startRotation = this.data.currentRotation;

    // 缓动函数
    const easeOutQuint = (t) => 1 - Math.pow(1 - t, 5);

    // 动画循环
    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const eased = easeOutQuint(progress);
      
      const currentRotation = startRotation + targetAngle * eased;
      this.setData({ currentRotation });
      this.drawWheel();

      if (progress < 1) {
        if (typeof requestAnimationFrame === 'function') {
          requestAnimationFrame(animate);
        } else {
          setTimeout(animate, 16);
        }
      } else {
        // 动画结束，显示结果
        this.setData({
          isSpinning: false,
          showResult: true,
          result: restaurants[winnerIndex]
        });

        // 震动反馈
        wx.vibrateShort({ type: 'heavy' });
      }
    };

    animate();
  },

  // 再转一次
  handleSpinAgain() {
    this.setData({ showResult: false });
    setTimeout(() => {
      this.handleSpin();
    }, 300);
  },

  // 关闭结果
  closeResult() {
    this.setData({ showResult: false });
  }
})
