import { startRecommendationStream } from '../../api/recommendation-chat';
import { mapApiRestaurantToCard } from '../../api/restaurants';
import {
  CreateChoiceHistory,
  CreateRecommendationFeedback,
  GetPreferenceProfile
} from '../../api/user-signals';
import {
  AI_CHAT_SESSION_KEY,
  buildPersistedAiChatState,
  shouldRestoreAiChatState
} from '../../utils/ai-chat-session';
import { markdownToRichText } from '../../utils/markdown-lite';

const app = getApp();
const INITIAL_QUESTION = '你好，先帮我看看现在适合吃什么';

function getUserId() {
  const user = app.getCurrentUser();
  if (!user) {
    return null;
  }
  const userId = user.id || user.userId;
  return Number.isFinite(Number(userId)) ? Number(userId) : null;
}

function buildMessageId(prefix) {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function upsertCard(list, nextCard) {
  const index = list.findIndex((item) => item.poiId === nextCard.poiId);
  if (index === -1) {
    return [...list, nextCard].sort((a, b) => (a.rank || 999) - (b.rank || 999));
  }

  const merged = [...list];
  merged[index] = { ...merged[index], ...nextCard, animateIn: false, enterStyle: '' };
  return merged.sort((a, b) => (a.rank || 999) - (b.rank || 999));
}

function createAiCardViewModel(payload = {}, options = {}) {
  const { animateIn = true } = options;
  const baseCard = mapApiRestaurantToCard({
    poiId: payload.poiId,
    name: payload.name,
    address: payload.address,
    category: payload.category,
    distance: payload.distance,
    avgRating: payload.avgRating,
    reviewCount: payload.reviewCount,
    avgPerCapitaPrice: payload.avgPerCapitaPrice,
    aiTags: payload.aiTags
  });

  const rank = payload.rank ? Number(payload.rank) : 999;
  return {
    ...baseCard,
    rank,
    matchReason: payload.matchReason || '',
    displayRating: baseCard.avgRating !== null && baseCard.avgRating !== undefined
      ? Number(baseCard.avgRating).toFixed(1)
      : '暂无评分',
    displayReviewCount: Number.isFinite(Number(baseCard.reviewCount)) ? Number(baseCard.reviewCount) : 0,
    displayAvgPerCapitaPrice: baseCard.avgPerCapitaPrice !== null && baseCard.avgPerCapitaPrice !== undefined
      ? `¥${baseCard.avgPerCapitaPrice}`
      : '人均待补充',
    distanceText: baseCard.distance,
    description: payload.matchReason || baseCard.description || 'AI 推荐餐厅',
    animateIn,
    enterStyle: animateIn ? `animation-delay:${Math.max(rank - 1, 0) * 90}ms;` : ''
  };
}

function createMessage(payload = {}) {
  const role = payload.role || 'assistant';
  const rawText = String(payload.rawText || '');
  const cards = Array.isArray(payload.cards) ? payload.cards : [];
  const hasStartedAnswer = Boolean(payload.hasStartedAnswer);

  return {
    id: payload.id || buildMessageId(role),
    role,
    rawText,
    contentHtml: role === 'assistant' && hasStartedAnswer ? markdownToRichText(rawText) : '',
    cards,
    isStreaming: Boolean(payload.isStreaming),
    progressText: String(payload.progressText || ''),
    hasStartedAnswer
  };
}

function hydrateMessage(message = {}) {
  const cards = Array.isArray(message.cards)
    ? message.cards.map((card) => ({
        ...card,
        animateIn: false,
        enterStyle: ''
      }))
    : [];

  return createMessage({
    id: message.id,
    role: message.role,
    rawText: message.rawText,
    cards,
    isStreaming: false,
    progressText: message.progressText || '',
    hasStartedAnswer: Boolean(message.hasStartedAnswer)
  });
}

function buildLegacyMessages(snapshot = {}) {
  const messages = [];
  const lastQuestion = String(snapshot.lastQuestion || snapshot.question || '').trim();
  const answerText = String(snapshot.answerText || '').trim();
  const cards = Array.isArray(snapshot.cards)
    ? snapshot.cards.map((card) => ({
        ...card,
        animateIn: false,
        enterStyle: ''
      }))
    : [];

  if (lastQuestion) {
    messages.push(createMessage({
      id: buildMessageId('legacy-user'),
      role: 'user',
      rawText: lastQuestion
    }));
  }

  if (answerText || cards.length > 0) {
    messages.push(createMessage({
      id: buildMessageId('legacy-assistant'),
      role: 'assistant',
      rawText: answerText,
      cards,
      isStreaming: false,
      progressText: '',
      hasStartedAnswer: Boolean(answerText)
    }));
  }

  return messages;
}

let activeStreamState = null;
let activeStreamTask = null;
let activeStreamPage = null;
let persistTimer = null;

function cloneCards(cards = []) {
  return Array.isArray(cards)
    ? cards.map((card) => ({
        ...card,
        animateIn: false,
        enterStyle: ''
      }))
    : [];
}

function cloneMessages(messages = []) {
  return Array.isArray(messages)
    ? messages.map((message) => ({
        id: message.id,
        role: message.role,
        rawText: String(message.rawText || ''),
        cards: cloneCards(message.cards),
        isStreaming: Boolean(message.isStreaming),
        progressText: String(message.progressText || ''),
        hasStartedAnswer: Boolean(message.hasStartedAnswer)
      }))
    : [];
}

function buildConversationSnapshot(data = {}) {
  return {
    question: String(data.question || ''),
    presetQuestion: String(data.presetQuestion || INITIAL_QUESTION),
    messages: cloneMessages(data.messages),
    loading: Boolean(data.loading),
    requestId: data.requestId || '',
    messageId: data.messageId || '',
    lastQuestion: data.lastQuestion || '',
    lastRejectedPoiIds: Array.isArray(data.lastRejectedPoiIds) ? [...data.lastRejectedPoiIds] : [],
    preferenceSummary: data.preferenceSummary || '',
    activeAssistantMessageId: data.activeAssistantMessageId || '',
    composerFocused: Boolean(data.composerFocused),
    scrollAnchorId: data.scrollAnchorId || 'chat-bottom'
  };
}

function updateMessageList(messages, messageId, updater) {
  const nextMessages = cloneMessages(messages);
  const index = nextMessages.findIndex((message) => message.id === messageId);
  if (index < 0) {
    return nextMessages;
  }

  nextMessages[index] = createMessage(updater(nextMessages[index]));
  return nextMessages;
}

function persistSnapshotNow(data) {
  const snapshot = buildPersistedAiChatState({
    ...data,
    status: data.loading ? 'streaming' : 'idle'
  });

  if (!shouldRestoreAiChatState(snapshot)) {
    wx.removeStorageSync(AI_CHAT_SESSION_KEY);
    return;
  }

  wx.setStorageSync(AI_CHAT_SESSION_KEY, snapshot);
}

function schedulePersistSnapshot(data) {
  clearTimeout(persistTimer);
  const snapshot = buildConversationSnapshot(data);
  persistTimer = setTimeout(() => {
    persistSnapshotNow(snapshot);
  }, 80);
}

function bindActiveStreamPage(page) {
  activeStreamPage = page;
}

function unbindActiveStreamPage(page) {
  if (activeStreamPage === page) {
    activeStreamPage = null;
  }
}

function emitActiveStreamState(options = {}) {
  if (!activeStreamPage || typeof activeStreamPage.applyConversationState !== 'function' || !activeStreamState) {
    return;
  }

  activeStreamPage.applyConversationState(activeStreamState, options);
}

function commitActiveStreamState(updater, options = {}) {
  const baseState = activeStreamState ? buildConversationSnapshot(activeStreamState) : null;
  const nextState = typeof updater === 'function' ? updater(baseState) : updater;
  if (!nextState) {
    return;
  }

  activeStreamState = buildConversationSnapshot(nextState);
  schedulePersistSnapshot(activeStreamState);
  emitActiveStreamState(options);
}

function setActiveStreamState(state, options = {}) {
  activeStreamState = buildConversationSnapshot(state);
  schedulePersistSnapshot(activeStreamState);
  emitActiveStreamState(options);
}

function clearActiveStreamTask() {
  activeStreamTask = null;
}

function abortActiveStream(options = {}) {
  const { keepState = false } = options;
  if (activeStreamTask && typeof activeStreamTask.abort === 'function') {
    activeStreamTask.abort();
  }
  activeStreamTask = null;

  if (!keepState) {
    activeStreamState = null;
  }
}

Page({
  data: {
    question: '',
    presetQuestion: INITIAL_QUESTION,
    messages: [],
    loading: false,
    requestId: '',
    messageId: '',
    lastQuestion: '',
    lastRejectedPoiIds: [],
    preferenceSummary: '',
    profileLoading: false,
    activeAssistantMessageId: '',
    composerFocused: false,
    scrollIntoView: '',
    scrollAnchorId: 'chat-bottom'
  },

  onLoad() {
    bindActiveStreamPage(this);
    this.initializePage();
  },

  onShow() {
    bindActiveStreamPage(this);
    if (activeStreamState) {
      this.applyConversationState(activeStreamState, { shouldScroll: false });
    }
  },

  onHide() {
    this.persistConversation();
    unbindActiveStreamPage(this);
  },

  onUnload() {
    this.persistConversation();
    unbindActiveStreamPage(this);
  },

  onQuestionInput(event) {
    const value = event && event.detail ? event.detail.value : '';
    this.setData({ question: value });
  },

  onQuestionFocus() {
    this.setData({ composerFocused: true });
  },

  onQuestionBlur() {
    this.setData({ composerFocused: false });
  },

  getEffectiveQuestion() {
    const manualQuestion = String(this.data.question || '').trim();
    if (manualQuestion) {
      return manualQuestion;
    }
    return String(this.data.presetQuestion || '').trim();
  },

  scheduleScrollToAnchor() {
    clearTimeout(this.scrollTimer);
    this.scrollTimer = setTimeout(() => {
      const targetId = this.data.scrollAnchorId || 'chat-bottom';
      this.setData({ scrollIntoView: '' }, () => {
        this.setData({ scrollIntoView: targetId });
      });
    }, 24);
  },

  setMessages(messages, extraData = {}, options = {}) {
    const { shouldScroll = true } = options;
    this.setData({
      messages,
      ...extraData
    }, () => {
      if (shouldScroll) {
        this.scheduleScrollToAnchor();
      }
    });
  },

  applyConversationState(state = {}, options = {}) {
    const {
      shouldScroll = false
    } = options;

    const nextData = buildConversationSnapshot({
      ...this.data,
      ...state
    });

    this.setData({
      question: nextData.question,
      presetQuestion: nextData.presetQuestion,
      messages: nextData.messages,
      loading: nextData.loading,
      requestId: nextData.requestId,
      messageId: nextData.messageId,
      lastQuestion: nextData.lastQuestion,
      lastRejectedPoiIds: nextData.lastRejectedPoiIds,
      preferenceSummary: nextData.preferenceSummary,
      activeAssistantMessageId: nextData.activeAssistantMessageId,
      composerFocused: nextData.composerFocused,
      scrollAnchorId: nextData.scrollAnchorId
    }, () => {
      if (shouldScroll) {
        this.scheduleScrollToAnchor();
      }
    });
  },

  restoreConversation() {
    if (activeStreamState) {
      this.applyConversationState(activeStreamState, { shouldScroll: false });
      return true;
    }

    const snapshot = wx.getStorageSync(AI_CHAT_SESSION_KEY);
    if (!shouldRestoreAiChatState(snapshot)) {
      return false;
    }

    const persisted = buildPersistedAiChatState(snapshot);
    const messages = Array.isArray(persisted.messages) && persisted.messages.length > 0
      ? persisted.messages.map(hydrateMessage)
      : buildLegacyMessages(persisted);

    this.setMessages(messages, {
      question: persisted.question || '',
      presetQuestion: persisted.question || INITIAL_QUESTION,
      loading: false,
      requestId: persisted.requestId || '',
      messageId: persisted.messageId || '',
      lastQuestion: persisted.lastQuestion || '',
      lastRejectedPoiIds: Array.isArray(persisted.lastRejectedPoiIds) ? persisted.lastRejectedPoiIds : [],
      activeAssistantMessageId: '',
      composerFocused: false,
      scrollAnchorId: 'chat-bottom'
    });
    return true;
  },

  persistConversation() {
    const source = activeStreamState || this.data;
    persistSnapshotNow(source);
  },

  async initializePage() {
    const restored = this.restoreConversation();
    await this.loadPreferenceProfile();

    const shouldAutoAsk = app.globalData && app.globalData.aiShouldPreheat;
    if (restored) {
      if (app.globalData) {
        app.globalData.aiShouldPreheat = false;
      }
      return;
    }

    const initialQuestion = app.globalData && app.globalData.aiPreheatQuestion
      ? app.globalData.aiPreheatQuestion
      : INITIAL_QUESTION;

    if (shouldAutoAsk || this.data.messages.length === 0) {
      this.setData({
        question: '',
        presetQuestion: initialQuestion
      });
      if (app.globalData) {
        app.globalData.aiShouldPreheat = false;
        app.globalData.aiPreheatQuestion = '';
      }
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
    return await app.resolveCurrentLocation();
  },

  buildPayload(location, question) {
    const userId = getUserId();
    const payload = {
      question: String(question || '').trim(),
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
    app.upsertRestaurant(card);
  },

  abortCurrentStream() {
    if (activeStreamTask && typeof activeStreamTask.abort === 'function') {
      activeStreamTask.abort();
    }
    clearActiveStreamTask();
  },

  updateAssistantMessage(messageId, updater, extraData = {}, options = {}) {
    const messages = updateMessageList(this.data.messages, messageId, updater);
    this.setMessages(messages, extraData, options);
  },

  appendAssistantText(messageId, delta) {
    this.updateAssistantMessage(messageId, (message) => ({
      ...message,
      rawText: message.hasStartedAnswer ? `${message.rawText || ''}${delta || ''}` : (delta || ''),
      cards: message.cards,
      isStreaming: true,
      progressText: '正在生成回复',
      hasStartedAnswer: true
    }), {}, { shouldScroll: false });
  },

  setAssistantText(messageId, text) {
    this.updateAssistantMessage(messageId, (message) => ({
      ...message,
      rawText: text,
      cards: message.cards,
      isStreaming: true,
      progressText: '正在整理答案',
      hasStartedAnswer: true
    }), {}, { shouldScroll: false });
  },

  finalizeAssistantMessage(messageId, extraText = '') {
    this.updateAssistantMessage(messageId, (message) => ({
      ...message,
      rawText: extraText ? extraText : message.rawText,
      cards: message.cards,
      isStreaming: false,
      progressText: '',
      hasStartedAnswer: Boolean(extraText ? extraText : message.rawText)
    }), {
      loading: false,
      activeAssistantMessageId: ''
    }, { shouldScroll: false });
  },

  upsertAssistantCard(messageId, nextCard) {
    this.updateAssistantMessage(messageId, (message) => ({
      ...message,
      rawText: message.rawText,
      cards: upsertCard(message.cards || [], nextCard),
      isStreaming: message.isStreaming,
      progressText: '正在补充推荐卡片',
      hasStartedAnswer: message.hasStartedAnswer
    }), {}, { shouldScroll: false });
  },

  findCardByPoiId(poiId) {
    for (let messageIndex = this.data.messages.length - 1; messageIndex >= 0; messageIndex -= 1) {
      const message = this.data.messages[messageIndex];
      const card = (message.cards || []).find((item) => item.poiId === poiId);
      if (card) {
        return card;
      }
    }
    return null;
  },

  async askAi(options = {}) {
    const { silent = false, fromPreheat = false } = options;
    if (this.data.loading) {
      return;
    }

    const question = this.getEffectiveQuestion();
    if (!question) {
      if (!silent) {
        wx.showToast({ title: '先输入你想吃什么', icon: 'none' });
      }
      return;
    }

    this.abortCurrentStream();

    const userMessage = createMessage({
      id: buildMessageId('user'),
      role: 'user',
      rawText: question
    });
    const assistantMessageId = buildMessageId('assistant');
    const assistantMessage = createMessage({
      id: assistantMessageId,
      role: 'assistant',
      rawText: fromPreheat ? '我先看看现在适合吃什么，再给你几家更贴近的选择。' : '我先看看附近有什么更适合你的选择。',
      cards: [],
      isStreaming: true,
      progressText: fromPreheat ? '正在准备今天的推荐' : '正在理解你的要求',
      hasStartedAnswer: false
    });

    const nextState = buildConversationSnapshot({
      ...this.data,
      messages: [...this.data.messages, userMessage, assistantMessage],
      question: '',
      presetQuestion: this.data.presetQuestion || INITIAL_QUESTION,
      loading: true,
      requestId: '',
      messageId: '',
      activeAssistantMessageId: assistantMessageId,
      composerFocused: false,
      scrollAnchorId: `message-${assistantMessageId}`
    });

    setActiveStreamState(nextState, { shouldScroll: true });

    let location;
    try {
      location = await this.getCurrentLocation();
    } catch (error) {
      const fallbackText = error && error.message
        ? error.message
        : '未获取到当前位置，请检查定位权限后重试';
      clearActiveStreamTask();
      commitActiveStreamState((state) => ({
        ...state,
        loading: false,
        activeAssistantMessageId: '',
        messages: updateMessageList(state.messages, assistantMessageId, (message) => ({
          ...message,
          rawText: fallbackText,
          cards: message.cards,
          isStreaming: false,
          progressText: '',
          hasStartedAnswer: true
        }))
      }), { shouldScroll: false });
      persistSnapshotNow(activeStreamState);
      return;
    }
    const payload = this.buildPayload(location, question);

    activeStreamTask = startRecommendationStream(payload, {
      onEvent: ({ event, data }) => {
        if (event === 'session.created') {
          commitActiveStreamState((state) => ({
            ...state,
            requestId: data && data.requestId ? data.requestId : '',
            messageId: data && data.messageId ? data.messageId : '',
            messages: updateMessageList(state.messages, assistantMessageId, (message) => ({
              ...message,
              progressText: '正在建立会话'
            }))
          }));
          return;
        }

        if (event === 'retrieval.started') {
          commitActiveStreamState((state) => ({
            ...state,
            messages: updateMessageList(state.messages, assistantMessageId, (message) => ({
              ...message,
              progressText: '正在看附近有什么可选'
            }))
          }));
          return;
        }

        if (event === 'retrieval.completed') {
          commitActiveStreamState((state) => ({
            ...state,
            messages: updateMessageList(state.messages, assistantMessageId, (message) => ({
              ...message,
              progressText: '正在挑更适合你的几家'
            }))
          }));
          return;
        }

        if (event === 'recommendation.card') {
          const normalizedCard = createAiCardViewModel({
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
          }, {
            animateIn: true
          });

          this.appendCardToGlobal(normalizedCard);
          commitActiveStreamState((state) => ({
            ...state,
            messages: updateMessageList(state.messages, assistantMessageId, (message) => ({
              ...message,
              rawText: message.rawText,
              cards: upsertCard(message.cards || [], normalizedCard),
              isStreaming: message.isStreaming,
              progressText: '正在补充推荐卡片',
              hasStartedAnswer: message.hasStartedAnswer
            }))
          }));
          return;
        }

        if (event === 'answer.delta') {
          commitActiveStreamState((state) => ({
            ...state,
            messages: updateMessageList(state.messages, assistantMessageId, (message) => ({
              ...message,
              rawText: message.hasStartedAnswer ? `${message.rawText || ''}${(data && data.delta) || ''}` : ((data && data.delta) || ''),
              cards: message.cards,
              isStreaming: true,
              progressText: '正在生成回复',
              hasStartedAnswer: true
            }))
          }), { shouldScroll: true });
          return;
        }

        if (event === 'answer.done') {
          commitActiveStreamState((state) => ({
            ...state,
            messages: updateMessageList(state.messages, assistantMessageId, (message) => ({
              ...message,
              rawText: (data && data.answer) || '',
              cards: message.cards,
              isStreaming: true,
              progressText: '正在整理答案',
              hasStartedAnswer: true
            }))
          }), { shouldScroll: true });
          return;
        }

        if (event === 'done') {
          clearActiveStreamTask();
          commitActiveStreamState((state) => ({
            ...state,
            loading: false,
            activeAssistantMessageId: '',
            lastQuestion: payload.question,
            messages: updateMessageList(state.messages, assistantMessageId, (message) => ({
              ...message,
              rawText: message.rawText,
              cards: message.cards,
              isStreaming: false,
              progressText: '',
              hasStartedAnswer: Boolean(message.rawText)
            }))
          }), { shouldScroll: false });
          persistSnapshotNow(activeStreamState);
          return;
        }

        if (event === 'error') {
          const fallbackText = (data && data.message)
            ? `抱歉，这次连接断了。${data.message}`
            : '抱歉，这次连接断了，你重新发一句我继续帮你找。';
          clearActiveStreamTask();
          commitActiveStreamState((state) => ({
            ...state,
            loading: false,
            activeAssistantMessageId: '',
            messages: updateMessageList(state.messages, assistantMessageId, (message) => ({
              ...message,
              rawText: fallbackText,
              cards: message.cards,
              isStreaming: false,
              progressText: '',
              hasStartedAnswer: true
            }))
          }), { shouldScroll: false });
          persistSnapshotNow(activeStreamState);
        }
      },
      onError: (err) => {
        const fallbackText = err && err.message
          ? `抱歉，这次连接断了。${err.message}`
          : '抱歉，这次请求失败了，你再发一句我继续。';
        clearActiveStreamTask();
        commitActiveStreamState((state) => ({
          ...state,
          loading: false,
          activeAssistantMessageId: '',
          messages: updateMessageList(state.messages, assistantMessageId, (message) => ({
            ...message,
            rawText: fallbackText,
            cards: message.cards,
            isStreaming: false,
            progressText: '',
            hasStartedAnswer: true
          }))
        }), { shouldScroll: false });
        persistSnapshotNow(activeStreamState);
      },
      onComplete: () => {
        clearActiveStreamTask();
        const assistant = (activeStreamState && activeStreamState.messages || []).find((message) => message.id === assistantMessageId);
        if (assistant && assistant.isStreaming) {
          const fallbackText = assistant.rawText
            ? `${assistant.rawText}\n\n- 这次回复提前结束了，你可以再追问我一次。`
            : '这次回复提前结束了，你可以再发一句我继续。';
          commitActiveStreamState((state) => ({
            ...state,
            loading: false,
            activeAssistantMessageId: '',
            messages: updateMessageList(state.messages, assistantMessageId, (message) => ({
              ...message,
              rawText: fallbackText,
              cards: message.cards,
              isStreaming: false,
              progressText: '',
              hasStartedAnswer: Boolean(fallbackText)
            }))
          }), { shouldScroll: false });
          persistSnapshotNow(activeStreamState);
        }
      }
    });
  },

  rejectCard(event) {
    const poiId = event.currentTarget.dataset.poiid;
    if (!poiId) {
      return;
    }

    const nextRejected = Array.from(new Set([...(this.data.lastRejectedPoiIds || []), poiId]));
    this.setData({ lastRejectedPoiIds: nextRejected });
    this.persistConversation();

    const userId = getUserId();
    const target = this.findCardByPoiId(poiId);
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

    const target = this.findCardByPoiId(poiId);
    const userId = getUserId();
    if (userId) {
      try {
        await CreateChoiceHistory(userId, {
          poiId,
          poiName: target && target.name ? target.name : ''
        });
      } catch (error) {}
    }

    this.persistConversation();
    wx.navigateTo({
      url: `/pages/detail/detail?id=${poiId}`
    });
  },

  openDetail(event) {
    const poiId = event.currentTarget.dataset.poiid;
    if (!poiId) {
      return;
    }

    this.persistConversation();
    wx.navigateTo({
      url: `/pages/detail/detail?id=${poiId}`
    });
  }
});
