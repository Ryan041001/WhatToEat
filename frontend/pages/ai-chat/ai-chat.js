import { startRecommendationStream } from '../../api/recommendation-chat';
import {
  CreateChoiceHistory,
  CreateRecommendationFeedback,
  GetPreferenceProfile
} from '../../api/user-signals';
const app = getApp();

function getUserId() {
  const user = app.getCurrentUser();
  if (!user) {
    return null;
  }
  const userId = user.id || user.userId;
  return Number.isFinite(Number(userId)) ? Number(userId) : null;
}

function formatDistance(distanceValue) {
  const meters = Number(distanceValue || 0);
  if (meters >= 1000) {
    return `${(meters / 1000).toFixed(1)}km`;
  }
  return `${Math.round(meters)}m`;
}

function upsertCard(list, nextCard) {
  const index = list.findIndex((item) => item.poiId === nextCard.poiId);
  if (index === -1) {
    return [...list, nextCard].sort((a, b) => (a.rank || 999) - (b.rank || 999));
  }

  const merged = [...list];
  merged[index] = { ...merged[index], ...nextCard };
  return merged.sort((a, b) => (a.rank || 999) - (b.rank || 999));
}

Page({
  data: {
    question: '你好，先帮我看看现在适合吃什么',
    answerText: '',
    status: 'idle',
    statusText: '输入问题后开始推荐',
    cards: [],
    loading: false,
    requestId: '',
    messageId: '',
    lastQuestion: '',
    lastRejectedPoiIds: [],
    preferenceSummary: '',
    profileLoading: false
  },

  onLoad() {
    this.initializePage();
  },

  onUnload() {
    this.abortCurrentStream();
  },

  onQuestionInput(event) {
    const value = event && event.detail ? event.detail.value : '';
    this.setData({ question: value });
  },

  async initializePage() {
    await this.loadPreferenceProfile();

    const shouldAutoAsk = app.globalData && app.globalData.aiShouldPreheat;
    if (shouldAutoAsk) {
      if (app.globalData.aiPreheatQuestion) {
        this.setData({ question: app.globalData.aiPreheatQuestion });
      }
      app.globalData.aiShouldPreheat = false;
      await this.askAi({ silent: true, fromPreheat: true });
    }
  },

  async loadPreferenceProfile() {
    const userId = getUserId();
    if (!userId) {
      this.setData({ preferenceSummary: '' });
      return;
    }

    this.setData({ profileLoading: true });
    try {
      const payload = await GetPreferenceProfile(userId);
      const data = payload && payload.data ? payload.data : payload;
      this.setData({
        preferenceSummary: data && data.summary ? data.summary : ''
      });
    } catch (error) {
      this.setData({ preferenceSummary: '' });
    } finally {
      this.setData({ profileLoading: false });
    }
  },

  async getCurrentLocation() {
    return await new Promise((resolve) => {
      wx.getLocation({
        type: 'gcj02',
        success: (res) => {
          resolve({
            longitude: res.longitude,
            latitude: res.latitude
          });
        },
        fail: () => {
          resolve({
            longitude: 120.1551,
            latitude: 30.2741
          });
        }
      });
    });
  },

  buildPayload(location) {
    const userId = getUserId();
    const payload = {
      question: String(this.data.question || '').trim(),
      longitude: location.longitude,
      latitude: location.latitude,
      radius: 3000,
      size: 3
    };

    if (userId) {
      payload.userId = userId;
    }

    if (this.data.lastQuestion || (this.data.lastRejectedPoiIds || []).length > 0 || this.data.preferenceSummary) {
      payload.context = {
        previousQuestion: this.data.lastQuestion || undefined,
        rejectedPoiIds: this.data.lastRejectedPoiIds || [],
        selectedPoiIds: [],
        userSignals: this.data.preferenceSummary ? [this.data.preferenceSummary] : []
      };
    }

    return payload;
  },

  appendCardToGlobal(card) {
    const poiId = card.poiId;
    if (!poiId) {
      return;
    }

    const current = app.getRestaurants();
    if (current.find((item) => item.id === poiId || item.poiId === poiId)) {
      return;
    }

    const avgPerCapitaPrice = Number.isFinite(Number(card.avgPerCapitaPrice)) ? Number(card.avgPerCapitaPrice) : null;
    let priceLevel = 0;
    if (avgPerCapitaPrice !== null) {
      if (avgPerCapitaPrice <= 20) {
        priceLevel = 1;
      } else if (avgPerCapitaPrice <= 40) {
        priceLevel = 2;
      } else {
        priceLevel = 3;
      }
    }

    const mapped = {
      id: poiId,
      poiId,
      name: card.name || '推荐餐厅',
      category: card.category || '其他',
      distance: formatDistance(card.distance),
      distanceValue: Number(card.distance || 0),
      avgRating: Number.isFinite(Number(card.avgRating)) ? Number(card.avgRating) : null,
      reviewCount: Number.isFinite(Number(card.reviewCount)) ? Number(card.reviewCount) : 0,
      avgPerCapitaPrice,
      aiTags: Array.isArray(card.aiTags) ? card.aiTags : [],
      rating: Number.isFinite(Number(card.avgRating)) ? Number(card.avgRating) : null,
      priceLevel,
      tags: Array.isArray(card.aiTags) ? card.aiTags.slice(0, 3) : [],
      image: 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800',
      description: card.address || 'AI 推荐餐厅',
      address: card.address || '地址待补充',
      isBlacklisted: false,
      isUserAdded: false
    };

    app.globalData.restaurants = [...current, mapped];
    wx.setStorageSync('restaurants_cache', app.globalData.restaurants);
  },

  abortCurrentStream() {
    if (this.requestTask && typeof this.requestTask.abort === 'function') {
      this.requestTask.abort();
      this.requestTask = null;
    }
  },

  async askAi(options = {}) {
    const { silent = false, fromPreheat = false } = options;
    const question = String(this.data.question || '').trim();
    if (!question) {
      if (!silent) {
        wx.showToast({ title: '先输入你想吃什么', icon: 'none' });
      }
      return;
    }

    this.abortCurrentStream();
    const location = await this.getCurrentLocation();
    const payload = this.buildPayload(location);

    this.setData({
      loading: true,
      status: 'streaming',
      statusText: fromPreheat ? '正在预热今日推荐...' : '正在连接 AI 推荐流...',
      answerText: '',
      cards: [],
      requestId: '',
      messageId: ''
    });

    this.requestTask = startRecommendationStream(payload, {
      onEvent: ({ event, data }) => {
        if (event === 'session.created') {
          this.setData({
            messageId: data && data.messageId ? data.messageId : '',
            requestId: data && data.requestId ? data.requestId : '',
            statusText: '会话已创建，准备检索候选餐厅...'
          });
          return;
        }

        if (event === 'retrieval.started') {
          this.setData({ statusText: '正在筛选候选餐厅...' });
          return;
        }

        if (event === 'retrieval.completed') {
          this.setData({ statusText: '候选餐厅筛选完成，正在生成推荐...' });
          return;
        }

        if (event === 'recommendation.card') {
          const normalizedCard = {
            rank: data && data.rank ? Number(data.rank) : 999,
            poiId: data && data.poiId ? data.poiId : '',
            name: data && data.name ? data.name : '推荐餐厅',
            address: data && data.address ? data.address : '地址待补充',
            category: data && data.category ? data.category : '其他',
            distance: data && data.distance ? Number(data.distance) : 0,
            avgRating: data && Number.isFinite(Number(data.avgRating)) ? Number(data.avgRating) : null,
            reviewCount: data && Number.isFinite(Number(data.reviewCount)) ? Number(data.reviewCount) : 0,
            avgPerCapitaPrice: data && Number.isFinite(Number(data.avgPerCapitaPrice)) ? Number(data.avgPerCapitaPrice) : null,
            aiTags: data && Array.isArray(data.aiTags) ? data.aiTags : [],
            matchReason: data && data.matchReason ? data.matchReason : ''
          };

          this.appendCardToGlobal(normalizedCard);
          this.setData({
            cards: upsertCard(this.data.cards, normalizedCard),
            statusText: '已收到推荐卡片，正在生成解释...'
          });
          return;
        }

        if (event === 'answer.delta') {
          this.setData({
            answerText: `${this.data.answerText}${(data && data.delta) || ''}`,
            statusText: 'AI 正在组织推荐理由...'
          });
          return;
        }

        if (event === 'answer.done') {
          this.setData({
            answerText: (data && data.answer) || this.data.answerText,
            statusText: '推荐理由生成完成'
          });
          return;
        }

        if (event === 'done') {
          this.setData({
            loading: false,
            status: 'final',
            statusText: '推荐完成',
            lastQuestion: payload.question
          });
          return;
        }

        if (event === 'error') {
          this.setData({
            loading: false,
            status: 'interrupted',
            statusText: (data && data.message) || '推荐中断，可重试'
          });
        }
      },
      onError: (err) => {
        this.setData({
          loading: false,
          status: 'error',
          statusText: err && err.message ? err.message : '请求失败，请稍后重试'
        });
      },
      onComplete: () => {
        if (this.data.status === 'streaming') {
          this.setData({
            loading: false,
            status: 'interrupted',
            statusText: this.data.cards.length > 0 ? '流已结束，推荐理由可能不完整' : '流已结束，请重试'
          });
        }
      }
    });
  },

  retry() {
    this.askAi();
  },

  rejectCard(event) {
    const poiId = event.currentTarget.dataset.poiid;
    if (!poiId) {
      return;
    }
    const nextRejected = Array.from(new Set([...(this.data.lastRejectedPoiIds || []), poiId]));
    this.setData({ lastRejectedPoiIds: nextRejected });

    const userId = getUserId();
    const target = (this.data.cards || []).find((card) => card.poiId === poiId);
    if (userId && target) {
      CreateRecommendationFeedback(userId, {
        poiId,
        poiNameSnapshot: target.name,
        feedbackType: 'DONT_WANT_THIS_TODAY',
        detail: '',
        requestQuestion: this.data.lastQuestion || this.data.question || ''
      }).catch(() => {});
    }

    wx.showToast({ title: '已记录偏好，下轮会避开', icon: 'none' });
  },

  async chooseThisRestaurant(event) {
    const poiId = event.currentTarget.dataset.poiid;
    if (!poiId) {
      return;
    }

    const target = (this.data.cards || []).find((card) => card.poiId === poiId);
    const userId = getUserId();
    if (userId) {
      try {
        await CreateChoiceHistory(userId, {
          poiId,
          poiName: target && target.name ? target.name : ''
        });
      } catch (error) {}
    }

    wx.navigateTo({
      url: `/pages/detail/detail?id=${poiId}`
    });
  },

  openDetail(event) {
    const poiId = event.currentTarget.dataset.poiid;
    if (!poiId) {
      return;
    }
    wx.navigateTo({
      url: `/pages/detail/detail?id=${poiId}`
    });
  }
});
