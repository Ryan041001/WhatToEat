const {
  DEFAULT_AVATAR_URL,
  extractBlacklistPoiIds,
  mergeBlacklistState,
  normalizeAuthUser
} = require('../utils/restaurant-state');
const {
  buildPersistedAiChatState,
  shouldRestoreAiChatState
} = require('../utils/ai-chat-session');
const { buildCategoryOptions } = require('../utils/restaurant-filters');
const { buildRatingStars } = require('../utils/rating-stars');

describe('utility helpers', () => {
  test('mergeBlacklistState marks matched poiIds as blacklisted', () => {
    const restaurants = [
      { id: 'a', poiId: 'POI-1', name: 'A' },
      { id: 'b', poiId: 'POI-2', name: 'B' }
    ];

    const merged = mergeBlacklistState(restaurants, ['POI-2']);

    expect(merged[0].isBlacklisted).toBe(false);
    expect(merged[1].isBlacklisted).toBe(true);
  });

  test('extractBlacklistPoiIds reads paginated api response items', () => {
    const payload = {
      data: {
        items: [
          { poiId: 'POI-1' },
          { poiId: 'POI-2' },
          { poiId: '' }
        ]
      }
    };

    expect(extractBlacklistPoiIds(payload)).toEqual(['POI-1', 'POI-2']);
  });

  test('normalizeAuthUser fills a display nickname when backend nickname is empty', () => {
    expect(normalizeAuthUser({ id: 7, nickname: '', openid: 'openid-1' })).toEqual({
      id: 7,
      nickname: '微信用户',
      nickName: '微信用户',
      openid: 'openid-1',
      avatarUrl: DEFAULT_AVATAR_URL,
      avatar: DEFAULT_AVATAR_URL
    });
  });

  test('buildPersistedAiChatState freezes an in-flight stream as interrupted snapshot', () => {
    const snapshot = buildPersistedAiChatState({
      question: '想吃面',
      answerText: '推荐牛肉面',
      status: 'streaming',
      statusText: 'AI 正在组织推荐理由...',
      cards: [{ poiId: 'POI-1', name: '牛肉面' }],
      loading: true
    });

    expect(snapshot.status).toBe('interrupted');
    expect(snapshot.loading).toBe(false);
    expect(snapshot.statusText).toBe('已恢复上次对话记录');
  });

  test('shouldRestoreAiChatState only restores meaningful snapshots', () => {
    expect(shouldRestoreAiChatState(null)).toBe(false);
    expect(shouldRestoreAiChatState({})).toBe(false);
    expect(shouldRestoreAiChatState({ answerText: 'A' })).toBe(true);
    expect(shouldRestoreAiChatState({ cards: [{ poiId: 'POI-1' }] })).toBe(true);
  });

  test('buildRatingStars returns full and half star fills for 3.5 rating', () => {
    expect(buildRatingStars(3.5).map((item) => item.fill)).toEqual([
      'full',
      'full',
      'full',
      'half',
      'empty'
    ]);
  });

  test('buildRatingStars returns one full star for 1.0 rating', () => {
    expect(buildRatingStars(1).map((item) => item.fill)).toEqual([
      'full',
      'empty',
      'empty',
      'empty',
      'empty'
    ]);
  });

  test('buildCategoryOptions keeps previous categories visible after selecting one category', () => {
    const previousCategories = ['面馆', '快餐', '烧烤'];
    const filteredRestaurants = [
      { id: 'a', category: '面馆' },
      { id: 'b', category: '面馆' }
    ];

    expect(buildCategoryOptions(filteredRestaurants, previousCategories, '面馆')).toEqual([
      '面馆',
      '快餐',
      '烧烤'
    ]);
  });
});