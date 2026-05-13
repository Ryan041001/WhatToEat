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
import { resolveRestaurantImage } from '../utils/restaurant-images.js';
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

test('resolveRestaurantImage prefers brand logos for major chains', () => {
  assert.equal(
    resolveRestaurantImage({ name: '肯德基 杭州湖滨店', category: '餐饮服务;快餐厅' }),
    'https://upload.wikimedia.org/wikipedia/commons/b/bf/KFC_logo.svg'
  );
});

test('resolveRestaurantImage maps restaurant names and amap categories to local cartoon art', () => {
  assert.equal(
    resolveRestaurantImage({ name: '老李淮南牛肉汤', category: '餐饮服务;中餐厅' }),
    '/assets/restaurant-images/noodles.svg'
  );
  assert.equal(
    resolveRestaurantImage({ name: '隆江猪脚饭', category: '餐饮服务;快餐厅' }),
    '/assets/restaurant-images/rice.svg'
  );
  assert.equal(
    resolveRestaurantImage({ name: '小甜水糖水铺', category: '餐饮服务;甜品店' }),
    '/assets/restaurant-images/drink.svg'
  );
  assert.equal(
    resolveRestaurantImage({ name: '东北手工水饺', category: '餐饮服务;中餐厅' }),
    '/assets/restaurant-images/jiaozi.svg'
  );
  assert.equal(
    resolveRestaurantImage({ name: '老王蛋炒饭', category: '餐饮服务;快餐厅' }),
    '/assets/restaurant-images/fried-rice.svg'
  );
  assert.equal(
    resolveRestaurantImage({ name: '牛肉汉堡店', category: '餐饮服务;西餐厅' }),
    '/assets/restaurant-images/burger.svg'
  );
  assert.equal(
    resolveRestaurantImage({ name: '意式披萨', category: '餐饮服务;西餐厅' }),
    '/assets/restaurant-images/pizza.svg'
  );
  assert.equal(
    resolveRestaurantImage({ name: '湖滨大酒店餐厅', category: '住宿服务;宾馆酒店' }),
    '/assets/restaurant-images/hotel.svg'
  );
});

test('resolveRestaurantImage returns a stable cartoon fallback for unknown restaurants', () => {
  assert.equal(
    resolveRestaurantImage({ name: '随机小店', category: '其他' }),
    '/assets/restaurant-images/default.svg'
  );
});
