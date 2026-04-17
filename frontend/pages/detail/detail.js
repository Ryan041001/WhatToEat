const app = getApp();

Page({
  data: {
    id: '',
    restaurant: null,
    loading: true,
    error: '',
    comments: [],
    visibleComments: [],
    commentsCollapsed: true,
    collapseThreshold: 3,
    commentInput: '',
    canSubmitComment: false
  },

  onLoad(options) {
    const id = options && options.id ? options.id : '';
    this.setData({ id });
    this.loadComments();
    this.loadDetail();
  },

  getCommentStorageKey() {
    return `restaurant_comments_${this.data.id || 'unknown'}`;
  },

  loadComments() {
    const comments = wx.getStorageSync(this.getCommentStorageKey()) || [];
    const normalizedComments = Array.isArray(comments) ? comments : [];
    const initialCollapsed = normalizedComments.length > this.data.collapseThreshold;
    this.setData({
      comments: normalizedComments,
      commentsCollapsed: initialCollapsed,
      visibleComments: this.getVisibleComments(normalizedComments, initialCollapsed)
    });
  },

  getVisibleComments(comments = [], isCollapsed = true) {
    if (!isCollapsed || comments.length <= this.data.collapseThreshold) {
      return comments;
    }
    return comments.slice(0, this.data.collapseThreshold);
  },

  saveComments(comments) {
    wx.setStorageSync(this.getCommentStorageKey(), comments);
  },

  formatCommentTime(timestamp) {
    const date = new Date(timestamp);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    return `${month}-${day} ${hour}:${minute}`;
  },

  onCommentInput(event) {
    const value = (event.detail && event.detail.value ? event.detail.value : '').slice(0, 120);
    this.setData({
      commentInput: value,
      canSubmitComment: value.trim().length > 0
    });
  },

  submitComment() {
    if (!this.data.restaurant) {
      return;
    }

    const content = (this.data.commentInput || '').trim();
    if (!content) {
      wx.showToast({
        title: '先写点内容再发布吧',
        icon: 'none'
      });
      return;
    }

    const user = app.getCurrentUser();
    const timestamp = Date.now();
    const nextComment = {
      id: `comment_${timestamp}`,
      author: user && user.nickname ? user.nickname : '匿名用户',
      content,
      createdAt: this.formatCommentTime(timestamp)
    };

    const nextComments = [nextComment, ...(this.data.comments || [])].slice(0, 50);
    const nextCollapsed = this.data.commentsCollapsed && nextComments.length > this.data.collapseThreshold;
    this.saveComments(nextComments);
    this.setData({
      comments: nextComments,
      visibleComments: this.getVisibleComments(nextComments, nextCollapsed),
      commentsCollapsed: nextCollapsed,
      commentInput: '',
      canSubmitComment: false
    });

    wx.showToast({
      title: '评论已发布',
      icon: 'success'
    });
  },

  toggleCommentFold() {
    if ((this.data.comments || []).length <= this.data.collapseThreshold) {
      return;
    }

    const nextCollapsed = !this.data.commentsCollapsed;
    this.setData({
      commentsCollapsed: nextCollapsed,
      visibleComments: this.getVisibleComments(this.data.comments, nextCollapsed)
    });
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

      this.setData({ restaurant });
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
