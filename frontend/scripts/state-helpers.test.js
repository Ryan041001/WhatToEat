import test from 'node:test';
import assert from 'node:assert/strict';

import {
  DEFAULT_AVATAR_URL,
  extractBlacklistPoiIds,
  mergeBlacklistState,
  normalizeAuthUser
} from '../utils/restaurant-state.js';
import {
  buildPersistedAiChatState,
  shouldRestoreAiChatState
} from '../utils/ai-chat-session.js';
import { buildCategoryOptions } from '../utils/restaurant-filters.js';
import { buildRatingStars } from '../utils/rating-stars.js';

test('mergeBlacklistState marks matched poiIds as blacklisted', () => {
  const restaurants = [
    { id: 'a', poiId: 'POI-1', name: 'A' },
    { id: 'b', poiId: 'POI-2', name: 'B' }
  ];

  const merged = mergeBlacklistState(restaurants, ['POI-2']);

  assert.equal(merged[0].isBlacklisted, false);
  assert.equal(merged[1].isBlacklisted, true);
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

  assert.deepEqual(extractBlacklistPoiIds(payload), ['POI-1', 'POI-2']);
});

test('normalizeAuthUser fills a display nickname when backend nickname is empty', () => {
  assert.deepEqual(
    normalizeAuthUser({ id: 7, nickname: '', openid: 'openid-1' }),
    {
      id: 7,
      nickname: '微信用户',
      nickName: '微信用户',
      openid: 'openid-1',
      avatarUrl: DEFAULT_AVATAR_URL,
      avatar: DEFAULT_AVATAR_URL
    }
  );
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

  assert.equal(snapshot.status, 'interrupted');
  assert.equal(snapshot.loading, false);
  assert.equal(snapshot.statusText, '已恢复上次对话记录');
});

test('shouldRestoreAiChatState only restores meaningful snapshots', () => {
  assert.equal(shouldRestoreAiChatState(null), false);
  assert.equal(shouldRestoreAiChatState({}), false);
  assert.equal(shouldRestoreAiChatState({ answerText: 'A' }), true);
  assert.equal(shouldRestoreAiChatState({ cards: [{ poiId: 'POI-1' }] }), true);
});

test('buildRatingStars returns full and half star fills for 3.5 rating', () => {
  assert.deepEqual(
    buildRatingStars(3.5).map((item) => item.fill),
    ['full', 'full', 'full', 'half', 'empty']
  );
});

test('buildRatingStars returns one full star for 1.0 rating', () => {
  assert.deepEqual(
    buildRatingStars(1).map((item) => item.fill),
    ['full', 'empty', 'empty', 'empty', 'empty']
  );
});

test('buildCategoryOptions keeps all category chips visible after selecting one category', () => {
  const previousCategories = ['面馆', '快餐', '烧烤'];
  const filteredRestaurants = [
    { id: 'a', category: '面馆' },
    { id: 'b', category: '面馆' }
  ];

  assert.deepEqual(
    buildCategoryOptions(filteredRestaurants, previousCategories, '面馆'),
    ['面馆', '快餐', '烧烤']
  );
});
