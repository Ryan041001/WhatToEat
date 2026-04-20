import {
  GetMyRestaurantReview,
  GetRestaurantReviews,
  GetReviewSummary,
  UpsertMyRestaurantReview,
  DeleteMyRestaurantReview
} from '../../api/reviews';

const app = getApp();

const RATING_OPTIONS = [
  { value: 0.5, label: '0.5 星' },
  { value: 1.0, label: '1.0 星' },
  { value: 1.5, label: '1.5 星' },
  { value: 2.0, label: '2.0 星' },
  { value: 2.5, label: '2.5 星' },
  { value: 3.0, label: '3.0 星' },
  { value: 3.5, label: '3.5 星' },
  { value: 4.0, label: '4.0 星' },
  { value: 4.5, label: '4.5 星' },
  { value: 5.0, label: '5.0 星' }
];

function unwrapData(payload) {
  if (!payload) {
    return null;
  }

  if (typeof payload === 'object' && payload.data !== undefined && !Array.isArray(payload)) {
    return payload.data;
  }

  return payload;
}

function extractItems(payload) {
  const data = unwrapData(payload);
  if (!data) {
    return [];
  }

  if (Array.isArray(data.items)) {
    return data.items;
  }

  if (Array.isArray(data.records)) {
    return data.records;
  }

  if (Array.isArray(data)) {
    return data;
  }

  return [];
}

function normalizeSummary(payload) {
  const raw = unwrapData(payload) || {};
  return {
    reviewCount: Number.isFinite(Number(raw.reviewCount)) ? Number(raw.reviewCount) : 0,
    avgRating: Number.isFinite(Number(raw.avgRating)) ? Number(raw.avgRating) : null,
    avgPerCapitaPrice: Number.isFinite(Number(raw.avgPerCapitaPrice)) ? Number(raw.avgPerCapitaPrice) : null,
    aiTags: Array.isArray(raw.aiTags) ? raw.aiTags : [],
    aiSummary: raw.aiSummary || '',
    recommendedScenarios: Array.isArray(raw.recommendedScenarios) ? raw.recommendedScenarios : []
  };
}

function normalizePublicReview(item = {}) {
  return {
    id: item.id || item.reviewId || `${item.userNickname || 'user'}_${item.updatedAt || Date.now()}`,
    author: item.userNickname || item.nickname || '匿名用户',
    ratingScore: Number.isFinite(Number(item.ratingScore)) ? Number(item.ratingScore) : null,
    perCapitaPrice: Number.isFinite(Number(item.perCapitaPrice)) ? Number(item.perCapitaPrice) : null,
    content: item.content || '',
    updatedAt: item.updatedAt || item.createdAt || ''
  };
}

function normalizeMyReview(payload) {
  const raw = unwrapData(payload) || {};
  return {
    ratingScore: Number.isFinite(Number(raw.ratingScore)) ? Number(raw.ratingScore) : null,
    perCapitaPrice: Number.isFinite(Number(raw.perCapitaPrice)) ? String(Number(raw.perCapitaPrice)) : '',
    content: raw.content || ''
  };
}

Page({
  data: {
    id: '',
    poiId: '',
    restaurant: null,
    loading: true,
    error: '',
    summaryLoading: false,
    reviewListLoading: false,
    myReviewLoading: false,
    submittingReview: false,
    deletingReview: false,
    summary: {
      reviewCount: 0,
      avgRating: null,
      avgPerCapitaPrice: null,
      aiTags: [],
      aiSummary: '',
      recommendedScenarios: []
    },
    publicReviews: [],
    hasReviewed: false,
    reviewForm: {
      ratingScore: null,
      perCapitaPrice: '',
      content: ''
    },
    ratingOptions: RATING_OPTIONS
  },

  onLoad(options) {
    const id = options && options.id ? options.id : '';
    this.setData({ id });
    this.loadDetail();
  },

  onPullDownRefresh() {
    this.loadDetail().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  getCurrentUserId() {
    const user = app.getCurrentUser();
    if (!user) {
      return null;
    }
    const userId = user.id || user.userId;
    return Number.isFinite(Number(userId)) ? Number(userId) : null;
  },

  formatDateTime(value) {
    if (!value) {
      return '';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return String(value);
    }
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    return `${month}-${day} ${hour}:${minute}`;
  },

  onRatingSelect(event) {
    const value = Number(event.currentTarget.dataset.value);
    if (!Number.isFinite(value)) {
      return;
    }
    this.setData({
      'reviewForm.ratingScore': value
    });
  },

  onPerCapitaInput(event) {
    const value = (event.detail && event.detail.value ? event.detail.value : '').replace(/\D+/g, '');
    this.setData({
      'reviewForm.perCapitaPrice': value
    });
  },

  onReviewContentInput(event) {
    const value = (event.detail && event.detail.value ? event.detail.value : '').slice(0, 1000);
    this.setData({
      'reviewForm.content': value
    });
  },

  validateReviewForm() {
    const { ratingScore, perCapitaPrice, content } = this.data.reviewForm;
    if (!Number.isFinite(Number(ratingScore))) {
      return '请先选择评分';
    }
    const rating = Number(ratingScore);
    if (rating < 0.5 || rating > 5 || (rating * 10) % 5 !== 0) {
      return '评分需在 0.5 到 5.0 之间，且步进 0.5';
    }

    if (!/^\d+$/.test(String(perCapitaPrice || ''))) {
      return '请输入有效的人均价格';
    }
    if (Number(perCapitaPrice) <= 0) {
      return '人均价格必须大于 0';
    }

    const normalizedContent = String(content || '').trim();
    if (!normalizedContent) {
      return '评论内容不能为空';
    }
    if (normalizedContent.length > 1000) {
      return '评论不能超过 1000 字';
    }

    return '';
  },

  async submitReview() {
    const userId = this.getCurrentUserId();
    if (!userId) {
      wx.showToast({ title: '请先登录后再评论', icon: 'none' });
      return;
    }

    const poiId = this.data.poiId;
    if (!poiId) {
      wx.showToast({ title: '餐厅信息异常，请重试', icon: 'none' });
      return;
    }

    const validationMessage = this.validateReviewForm();
    if (validationMessage) {
      wx.showToast({ title: validationMessage, icon: 'none' });
      return;
    }

    const payload = {
      poiNameSnapshot: this.data.restaurant && this.data.restaurant.name ? this.data.restaurant.name : '',
      ratingScore: Number(this.data.reviewForm.ratingScore),
      perCapitaPrice: Number(this.data.reviewForm.perCapitaPrice),
      content: String(this.data.reviewForm.content || '').trim()
    };

    this.setData({ submittingReview: true });
    try {
      await UpsertMyRestaurantReview(userId, poiId, payload);
      wx.showToast({
        title: this.data.hasReviewed ? '评论已更新' : '评论已发布',
        icon: 'success'
      });
      await Promise.all([
        this.loadMyReview(),
        this.loadPublicReviews(),
        this.loadSummary()
      ]);
    } catch (error) {
      wx.showToast({ title: error.message || '评论提交失败', icon: 'none' });
    } finally {
      this.setData({ submittingReview: false });
    }
  },

  async deleteMyReview() {
    const userId = this.getCurrentUserId();
    if (!userId || !this.data.poiId) {
      return;
    }

    wx.showModal({
      title: '删除评论',
      content: '确认删除你对这家店的评论吗？',
      success: async (res) => {
        if (!res.confirm) {
          return;
        }

        this.setData({ deletingReview: true });
        try {
          await DeleteMyRestaurantReview(userId, this.data.poiId);
          wx.showToast({ title: '评论已删除', icon: 'success' });
          await Promise.all([
            this.loadMyReview(),
            this.loadPublicReviews(),
            this.loadSummary()
          ]);
        } catch (error) {
          wx.showToast({ title: error.message || '删除失败，请稍后重试', icon: 'none' });
        } finally {
          this.setData({ deletingReview: false });
        }
      }
    });
  },

  async loadSummary() {
    if (!this.data.poiId) {
      return;
    }
    this.setData({ summaryLoading: true });
    try {
      const payload = await GetReviewSummary(this.data.poiId);
      this.setData({ summary: normalizeSummary(payload) });
    } catch (error) {
      this.setData({
        summary: {
          reviewCount: 0,
          avgRating: null,
          avgPerCapitaPrice: null,
          aiTags: [],
          aiSummary: '',
          recommendedScenarios: []
        }
      });
    } finally {
      this.setData({ summaryLoading: false });
    }
  },

  async loadPublicReviews() {
    if (!this.data.poiId) {
      return;
    }
    this.setData({ reviewListLoading: true });
    try {
      const payload = await GetRestaurantReviews(this.data.poiId, { page: 1, size: 10 });
      const items = extractItems(payload).map((item) => ({
        ...normalizePublicReview(item),
        displayTime: this.formatDateTime(item.updatedAt || item.createdAt)
      }));
      this.setData({ publicReviews: items });
    } catch (error) {
      this.setData({ publicReviews: [] });
    } finally {
      this.setData({ reviewListLoading: false });
    }
  },

  async loadMyReview() {
    const userId = this.getCurrentUserId();
    if (!userId || !this.data.poiId) {
      this.setData({
        hasReviewed: false,
        reviewForm: {
          ratingScore: null,
          perCapitaPrice: '',
          content: ''
        }
      });
      return;
    }

    this.setData({ myReviewLoading: true });
    try {
      const response = await GetMyRestaurantReview(userId, this.data.poiId);
      const statusCode = response && response.statusCode;
      const body = response && response.data ? response.data : {};
      const bizCode = body && body.code;

      if (statusCode === 404 && bizCode === 2104) {
        this.setData({
          hasReviewed: false,
          reviewForm: {
            ratingScore: null,
            perCapitaPrice: '',
            content: ''
          }
        });
        return;
      }

      const myReview = normalizeMyReview(body);
      this.setData({
        hasReviewed: true,
        reviewForm: myReview
      });
    } catch (error) {
      this.setData({
        hasReviewed: false,
        reviewForm: {
          ratingScore: null,
          perCapitaPrice: '',
          content: ''
        }
      });
    } finally {
      this.setData({ myReviewLoading: false });
    }
  },

  async loadReviewBlocks() {
    await Promise.all([
      this.loadSummary(),
      this.loadPublicReviews(),
      this.loadMyReview()
    ]);
  },

  async loadDetail() {
    this.setData({ loading: true, error: '' });
    try {
      if (!this.data.id) {
        throw new Error('没有获取到餐厅信息');
      }

      await app.bootstrapRestaurants();
      const restaurant = app.getRestaurantById(this.data.id);

      if (!restaurant) {
        throw new Error('这家餐厅暂时找不到了');
      }

      const poiId = restaurant.poiId || restaurant.id;
      this.setData({
        restaurant,
        poiId
      });

      await this.loadReviewBlocks();
    } catch (error) {
      this.setData({ error: error.message || '加载详情时出了点问题，请稍后再试' });
    } finally {
      this.setData({ loading: false });
    }
  },

  toggleBlacklist() {
    if (!this.data.restaurant) {
      return;
    }

    app.toggleBlacklist(this.data.restaurant.id);
    const next = app.getRestaurantById(this.data.restaurant.id);
    this.setData({ restaurant: next });

    wx.showToast({
      title: next && next.isBlacklisted ? '已设为不感兴趣' : '已恢复展示',
      icon: 'none'
    });
  },

  goBack() {
    wx.navigateBack({
      fail: () => {
        wx.redirectTo({ url: '/pages/home/home' });
      }
    });
  }
});
