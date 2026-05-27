// pages/home/home.js
import { AI_CHAT_SESSION_KEY, shouldRestoreAiChatState } from '../../utils/ai-chat-session';
import { GetRandomRecommendation } from '../../api/recommendations';
import { mapApiRestaurantToCard } from '../../api/restaurants';
import { CreateChoiceHistory } from '../../api/user-signals';
import { extractRestaurantList } from '../../utils/restaurant-state';

const app = getApp();

Page({
  data: {
    loading: false,
    error: '',
    totalCount: 0,
    activeCount: 0,
    blacklistedCount: 0,
    topRated: [],
    shakeResult: null,
    shaking: false
  },

  onLoad() {
    this.loadData();
  },


  onShow() {
    this.loadData();
    // 仅首页启用摇一摇监听
    this._lastShakeTs = 0;
    this._shakeStartTs = Date.now();
    this._shakeSampleCount = 0;
    this._shakeSpikeCount = 0;
    this._lastAccel = null;
    this._onShakeAccelerometerChange = this._onShakeAccelerometerChange || this._onAccelerometerChange.bind(this);
    this.enableShakeSensor();
  },

  // 加载数据
  async loadData() {
    this.setData({ loading: true, error: '' });
    try {
      await app.bootstrapRestaurants({
        force: true,
        forceLocationRefresh: true
      });
      const restaurants = app.getRestaurants();
      const actives = app.getActiveRestaurants();
      const topRated = [...actives]
        .sort((a, b) => b.rating - a.rating)
        .slice(0, 3)
        .map((r) => ({
          ...r,
          priceText: '¥'.repeat(r.priceLevel)
        }));

      this.setData({
        totalCount: restaurants.length,
        activeCount: actives.length,
        blacklistedCount: restaurants.length - actives.length,
        topRated
      });
    } catch (error) {
      this.setData({
        error: '餐厅数据暂时没加载出来，请稍后再试'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 跳转到大转盘
  goToSpin() {
    wx.navigateTo({
      url: '/pages/spin/spin'
    });
  },

  onHide() {
    // 离开首页解绑监听
    this.disableShakeSensor();
  },

  onUnload() {
    // 离开首页解绑监听
    this.disableShakeSensor();
  },

  enableShakeSensor() {
    try {
      const attachListener = () => {
        wx.offAccelerometerChange && wx.offAccelerometerChange(this._onShakeAccelerometerChange);
        wx.onAccelerometerChange && wx.onAccelerometerChange(this._onShakeAccelerometerChange);
      };

      if (wx.stopAccelerometer) {
        wx.stopAccelerometer({
          complete: () => {
            wx.startAccelerometer && wx.startAccelerometer({
              interval: 'game',
              success: attachListener,
              fail: () => {}
            });
          }
        });
        return;
      }

      wx.startAccelerometer && wx.startAccelerometer({
        interval: 'game',
        success: attachListener,
        fail: () => {}
      });
    } catch (e) {}
  },

  disableShakeSensor() {
    try {
      wx.offAccelerometerChange && wx.offAccelerometerChange(this._onShakeAccelerometerChange);
      wx.stopAccelerometer && wx.stopAccelerometer();
    } catch (e) {}
  },

  // 摇一摇判定逻辑
  _onAccelerometerChange(res) {
    // 只在未展示结果时响应
    if (this.data.shakeResult || this.data.shaking) return;

    const now = Date.now();
    if (now - (this._shakeStartTs || 0) < 800) return; // 启动后先稳定一小段时间
    if (now - (this._lastShakeTs || 0) < 1600) return; // 触发后冷却更久，避免连续误判

    const { x, y, z } = res;
    const magnitude = Math.sqrt((x * x) + (y * y) + (z * z));
    const accelDelta = Math.abs(magnitude - 1);
    const prev = this._lastAccel;
    const stepDelta = prev
      ? Math.abs(x - prev.x) + Math.abs(y - prev.y) + Math.abs(z - prev.z)
      : 0;

    this._lastAccel = { x, y, z };
    this._shakeSampleCount += 1;

    // 需要连续多次明显波动才认为是在摇手机
    if (accelDelta > 0.75 || stepDelta > 2.2) {
      this._shakeSpikeCount += 1;
    } else {
      this._shakeSpikeCount = 0;
    }

    if (this._shakeSampleCount >= 4 && this._shakeSpikeCount >= 2) {
      this._lastShakeTs = now;
      this._shakeSampleCount = 0;
      this._shakeSpikeCount = 0;
      this.handleShake();
    }
  },

  // 跳转到卡片滑选
  goToSwipe() {
    wx.navigateTo({
      url: '/pages/swipe/swipe'
    });
  },

  // 跳转到餐厅列表
  goToRestaurants() {
    wx.navigateTo({
      url: '/pages/restaurants/restaurants'
    });
  },

  goToAiChat() {
    if (!app.globalData) {
      app.globalData = {};
    }

    const cachedSession = wx.getStorageSync(AI_CHAT_SESSION_KEY);
    const hasSessionSnapshot = shouldRestoreAiChatState(cachedSession);
    app.globalData.aiShouldPreheat = !hasSessionSnapshot;
    app.globalData.aiPreheatQuestion = hasSessionSnapshot ? '' : '你好，先帮我看看现在适合吃什么';

    wx.navigateTo({
      url: '/pages/ai-chat/ai-chat'
    });
  },

  // 跳转到我的
  goToMine() {
    wx.navigateTo({
      url: '/pages/mine/mine'
    });
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      return;
    }
    wx.navigateTo({
      url: `/pages/detail/detail?id=${id}`
    });
  },

  // 摇一摇
  async handleShake() {
    if (this.data.shaking) return;

    this.setData({ shaking: true });
    wx.vibrateShort({ type: 'medium' });

    try {
      const userId = app.getCurrentUserId();
      let pick = null;

      if (userId) {
        try {
          const location = await app.resolveCurrentLocation({ forceRefresh: true });
          const response = await GetRandomRecommendation({
            userId,
            longitude: location.longitude,
            latitude: location.latitude,
            radius: 3000
          });
          const list = extractRestaurantList(response).map(mapApiRestaurantToCard);
          if (list.length > 0) {
            pick = list[0];
          }
        } catch (e) {
          console.warn('随机推荐接口失败，回退到本地随机', e);
        }
      }

      if (!pick) {
        const actives = app.getActiveRestaurants();
        if (actives.length === 0) {
          this.setData({ shaking: false });
          wx.showToast({ title: '暂时没有可选餐厅', icon: 'none' });
          return;
        }
        pick = actives[Math.floor(Math.random() * actives.length)];
      }

      setTimeout(() => {
        this.setData({
          shaking: false,
          shakeResult: pick
        });
      }, 800);
    } catch (err) {
      this.setData({ shaking: false });
      wx.showToast({ title: '摇一摇失败，请重试', icon: 'none' });
    }
  },

  // 就吃这家
  async chooseShakeResult() {
    const pick = this.data.shakeResult;
    if (!pick) return;

    const userId = app.getCurrentUserId();
    if (userId && pick.poiId) {
      try {
        await CreateChoiceHistory(userId, {
          poiId: pick.poiId,
          poiName: pick.name || ''
        });
      } catch (e) {}
    }

    this.closeShakeResult();
    wx.navigateTo({
      url: `/pages/detail/detail?id=${pick.id}`
    });
  },

  goToShakeDetail() {
    const pick = this.data.shakeResult;
    if (!pick || !pick.id) {
      return;
    }

    wx.navigateTo({
      url: `/pages/detail/detail?id=${pick.id}`
    });
  },

  noop() {},

  // 关闭摇一摇结果
  closeShakeResult() {
    this.setData({ shakeResult: null });
  }
})
