// pages/spin/spin.js
const app = getApp();

const SEGMENT_COLORS = [
  '#FF6B35', '#FF8C42', '#FFA502', '#FFBE0B',
  '#F77F00', '#FCBF49', '#E09F3E', '#F4A261',
  '#FF9F1C', '#E76F51', '#D62828', '#F28482',
];

Page({
  data: {
    restaurants: [],
    spinning: false,
    result: null,
    currentAngle: 0
  },

  canvas: null,
  ctx: null,
  animId: null,
  
  onLoad() {
    this.loadData();
  },

  onShow() {
    this.loadData();
    if (!this.ctx && this.data.restaurants.length > 0) {
      this.initCanvas();
    } else if (this.ctx) {
      this.draw(this.data.currentAngle);
    }
  },

  onHide() {
    if (this.animId) {
      this.canvas.cancelAnimationFrame(this.animId);
      this.animId = null;
    }
  },

  onUnload() {
    if (this.animId) {
      this.canvas.cancelAnimationFrame(this.animId);
      this.animId = null;
    }
  },

  goBack() {
    if (getCurrentPages().length > 1) {
      wx.navigateBack();
      return;
    }
    wx.redirectTo({ url: '/pages/home/home' });
  },

  loadData() {
    const active = app.getActiveRestaurants() || [];
    this.setData({ restaurants: active });
  },

  initCanvas() {
    const query = wx.createSelectorQuery();
    query.select('#spinCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0]) return;
        const canvas = res[0].node;
        const ctx = canvas.getContext('2d');
        const dpr = wx.getWindowInfo().pixelRatio;

        canvas.width = res[0].width * dpr;
        canvas.height = res[0].height * dpr;
        ctx.scale(dpr, dpr);

        this.canvas = canvas;
        this.ctx = ctx;
        
        this.draw(this.data.currentAngle);
      });
  },

  draw(angle, highlightIdx = -1) {
    const ctx = this.ctx;
    const canvas = this.canvas;
    const sysInfo = wx.getSystemInfoSync();
    // Simulate color-mix with fixed rgba fallbacks for wxss variables 
    // Wait, we can't use var(--x) in canvas. We must hardcode the hex/rgba equivalent.
    // var(--glass-surface-strong) -> rgba(255, 255, 255, 0.6)
    // var(--foreground) -> #0f172a
    // var(--warning) -> #eab308

    if (!ctx || !canvas) return;

    const W = 290;
    const H = 290;
    const cx = W / 2;
    const cy = H / 2;
    const radius = cx - 12;
    const names = this.data.restaurants.map(r => r.name);
    const n = names.length;
    if (n === 0) return;

    ctx.clearRect(0, 0, W, H);

    // Outer ring glow
    ctx.save();
    ctx.beginPath();
    ctx.arc(cx, cy, radius + 6, 0, 2 * Math.PI);
    ctx.fillStyle = 'rgba(255, 255, 255, 0.85)'; // approx color-mix(in srgb, var(--glass-surface-strong) 90%, white)
    ctx.shadowBlur = 16;
    ctx.shadowColor = 'rgba(15, 23, 42, 0.18)'; 
    ctx.fill();
    ctx.restore();

    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(angle);

    const arcSize = (2 * Math.PI) / n;
    for (let i = 0; i < n; i++) {
      const startAngle = i * arcSize - Math.PI / 2;
      const endAngle = startAngle + arcSize;
      const color = SEGMENT_COLORS[i % SEGMENT_COLORS.length];

      ctx.beginPath();
      ctx.moveTo(0, 0);
      ctx.arc(0, 0, radius, startAngle, endAngle);
      ctx.closePath();
      
      // highlight approx
      ctx.fillStyle = highlightIdx === i ? '#fcd34d' : color; 
      ctx.fill();
      
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.85)';
      ctx.lineWidth = 2.5;
      ctx.stroke();

      ctx.save();
      ctx.rotate(startAngle + arcSize / 2);
      ctx.textAlign = 'right';
      ctx.textBaseline = 'middle';
      const fontSize = n <= 6 ? 13 : n <= 9 ? 11 : 9;
      ctx.font = `bold ${fontSize}px sans-serif`;
      ctx.fillStyle = highlightIdx === i ? color : 'rgba(255, 255, 255, 0.95)';
      ctx.strokeStyle = 'rgba(0, 0, 0, 0.4)';
      ctx.lineWidth = n <= 8 ? 3 : 2.4;
      
      const maxLen = n <= 6 ? 6 : n <= 9 ? 5 : 4;
      const label = names[i].length > maxLen ? names[i].slice(0, maxLen) + '…' : names[i];
      const labelOffset = n <= 6 ? radius - 12 : n <= 9 ? radius - 16 : radius - 20;
      
      ctx.strokeText(label, labelOffset, 1);
      ctx.fillText(label, labelOffset, 1);
      ctx.restore();
    }

    ctx.restore();

    // Center circle
    ctx.save();
    ctx.beginPath();
    ctx.arc(cx, cy, 28, 0, 2 * Math.PI);
    ctx.fillStyle = 'rgba(255, 255, 255, 0.6)';
    ctx.shadowBlur = 6;
    ctx.shadowColor = 'rgba(15, 23, 42, 0.12)';
    ctx.fill();
    ctx.restore();

    // Center emoji
    ctx.font = '20px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillStyle = '#000';
    ctx.fillText('🍚', cx, cy);
  },

  spin() {
    if (this.data.spinning || this.data.restaurants.length === 0) return;
    
    this.setData({
      result: null,
      spinning: true
    });

    const n = this.data.restaurants.length;
    const pickedIdx = Math.floor(Math.random() * n);
    const arcSize = (2 * Math.PI) / n;

    const segCenter = pickedIdx * arcSize + arcSize / 2;
    const targetBase = -segCenter;

    const fullSpins = (5 + Math.floor(Math.random() * 3)) * 2 * Math.PI;
    const currentAngle = this.data.currentAngle;
    const normalizedCurrent = ((currentAngle % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
    const normalizedTarget = ((targetBase % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
    const diff = ((normalizedTarget - normalizedCurrent) + 2 * Math.PI) % (2 * Math.PI);
    const totalSpin = fullSpins + diff;
    const endAngle = currentAngle + totalSpin;

    const duration = 4500;
    const startTime = Date.now();
    const startAngle = currentAngle;

    const animate = () => {
      const now = Date.now();
      const elapsed = Math.min(now - startTime, duration);
      const t = elapsed / duration;
      const eased = 1 - Math.pow(1 - t, 5); // Ease out quint
      const angle = startAngle + (endAngle - startAngle) * eased;
      
      this.setData({ currentAngle: angle });
      this.draw(angle);

      if (elapsed < duration) {
        this.animId = this.canvas.requestAnimationFrame(animate);
      } else {
        this.setData({ currentAngle: endAngle });
        this.setData({
          spinning: false,
          result: this.data.restaurants[pickedIdx].name
        });
        this.draw(endAngle, pickedIdx);
      }
    };

    if (this.canvas) {
      this.animId = this.canvas.requestAnimationFrame(animate);
    }
  },

  spinAgain() {
    this.setData({ result: null });
    setTimeout(() => {
      this.spin();
    }, 200);
  }
});