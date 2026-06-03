const {
  DEFAULT_AVATAR_URL,
  extractBlacklistPoiIds,
  mergeBlacklistState,
  normalizeAuthUser
} = require('../utils/restaurant-state');
const {
  buildClearedAiChatState,
  buildPersistedAiChatState,
  shouldRestoreAiChatState
} = require('../utils/ai-chat-session');
const { markdownToRichText } = require('../utils/markdown-lite');
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

  test('buildPersistedAiChatState preserves assistant markdown render state', () => {
    const snapshot = buildPersistedAiChatState({
      messages: [
        { id: 'm1', role: 'assistant', rawText: '**首选** 牛肉面', hasStartedAnswer: true }
      ]
    });

    expect(snapshot.messages[0].hasStartedAnswer).toBe(true);
  });

  test('buildClearedAiChatState clears only local chat conversation fields', () => {
    const cleared = buildClearedAiChatState({
      preferenceSummary: '偏好清淡',
      profileLoading: true
    });

    expect(cleared.messages).toEqual([]);
    expect(cleared.lastQuestion).toBe('');
    expect(cleared.lastRejectedPoiIds).toEqual([]);
    expect(cleared.loading).toBe(false);
    expect(cleared.preferenceSummary).toBe('偏好清淡');
    expect(cleared.profileLoading).toBe(true);
  });

  test('shouldRestoreAiChatState only restores meaningful snapshots', () => {
    expect(shouldRestoreAiChatState(null)).toBe(false);
    expect(shouldRestoreAiChatState({})).toBe(false);
    expect(shouldRestoreAiChatState({ answerText: 'A' })).toBe(true);
    expect(shouldRestoreAiChatState({ cards: [{ poiId: 'POI-1' }] })).toBe(true);
  });

  test('markdownToRichText renders structured markdown and escapes html', () => {
    const html = markdownToRichText([
      '### 推荐',
      '',
      '1. **三沐茶苑**：适合清淡下午茶',
      '2. `小何木薯羹`：甜汤不油腻',
      '',
      '> 三家都很近',
      '',
      '<script>alert(1)</script>'
    ].join('\n'));

    expect(html).toContain('<ol');
    expect(html).toContain('<strong');
    expect(html).toContain('<code');
    expect(html).toContain('border-left');
    expect(html).not.toContain('&gt; 三家都很近');
    expect(html).toContain('&lt;script&gt;alert(1)&lt;/script&gt;');
    expect(html).not.toContain('<script>');
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
