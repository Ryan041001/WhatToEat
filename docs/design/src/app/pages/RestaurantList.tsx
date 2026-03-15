import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { ChevronLeft, Star, MapPin, Ban, Trash2, ThumbsUp, ThumbsDown, Search, Filter, ChevronDown } from 'lucide-react';
import { useNavigate } from 'react-router';
import { useRestaurants, Restaurant } from '../context/RestaurantContext';

const CATEGORIES = ['全部', '川菜', '日料', '快餐', '烧烤', '米线', '面食', '韩餐', '西餐', '北方面食', '其他'];
const PRICE_FILTERS = ['全部', '¥', '¥¥', '¥¥¥'];

function RestaurantCard({ restaurant }: { restaurant: Restaurant }) {
  const { toggleBlacklist, voteRestaurant, deleteRestaurant } = useRestaurants();
  const [showMenu, setShowMenu] = useState(false);
  const [voted, setVoted] = useState<'up' | 'down' | null>(null);

  const handleVote = (type: 'up' | 'down') => {
    if (voted) return;
    voteRestaurant(restaurant.id, type);
    setVoted(type);
  };

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.95 }}
      className="rounded-2xl overflow-hidden"
      style={{
        background: restaurant.isBlacklisted ? '#f9f9f9' : '#fff',
        boxShadow: '0 2px 12px rgba(0,0,0,0.06)',
        opacity: restaurant.isBlacklisted ? 0.7 : 1,
      }}
    >
      <div style={{ position: 'relative' }}>
        <img
          src={restaurant.image}
          alt={restaurant.name}
          style={{ width: '100%', height: '160px', objectFit: 'cover', display: 'block' }}
        />
        {restaurant.isBlacklisted && (
          <div
            style={{
              position: 'absolute', inset: 0,
              background: 'rgba(0,0,0,0.5)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
          >
            <div className="flex flex-col items-center gap-1">
              <Ban size={28} color="#fff" />
              <span style={{ color: '#fff', fontSize: '12px', fontWeight: 700 }}>已拉黑</span>
            </div>
          </div>
        )}
        {/* Category */}
        <div
          style={{
            position: 'absolute', top: 8, left: 8,
            background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)',
            borderRadius: '12px', padding: '3px 8px',
          }}
        >
          <span style={{ color: '#fff', fontSize: '10px', fontWeight: 600 }}>{restaurant.category}</span>
        </div>
        {restaurant.isUserAdded && (
          <div
            style={{
              position: 'absolute', top: 8, right: 8,
              background: '#FFA502', borderRadius: '12px', padding: '3px 8px',
            }}
          >
            <span style={{ color: '#fff', fontSize: '9px', fontWeight: 700 }}>我添加的</span>
          </div>
        )}
        {/* More button */}
        <button
          onClick={() => setShowMenu(!showMenu)}
          style={{
            position: 'absolute', bottom: 8, right: 8,
            background: 'rgba(255,255,255,0.2)', backdropFilter: 'blur(4px)',
            borderRadius: '50%', width: 28, height: 28,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            border: '1px solid rgba(255,255,255,0.4)',
          }}
        >
          <ChevronDown size={14} color="#fff" />
        </button>
      </div>

      <div className="px-3 py-3">
        <div className="flex items-start justify-between">
          <div className="flex-1 min-w-0">
            <h3 style={{ fontSize: '14px', fontWeight: 700, color: '#1a1a1a' }}>{restaurant.name}</h3>
            <div className="flex items-center gap-2 mt-0.5">
              <div className="flex items-center gap-0.5">
                <Star size={11} fill="#FFA502" color="#FFA502" />
                <span style={{ fontSize: '11px', color: '#FFA502', fontWeight: 600 }}>{restaurant.rating}</span>
              </div>
              <span style={{ fontSize: '11px', color: '#999' }}>{'¥'.repeat(restaurant.priceLevel)}</span>
              <div className="flex items-center gap-0.5">
                <MapPin size={9} color="#999" />
                <span style={{ fontSize: '10px', color: '#999' }}>{restaurant.distance}</span>
              </div>
            </div>
          </div>
        </div>

        <p style={{ fontSize: '11px', color: '#888', marginTop: '4px', lineHeight: 1.4 }}>
          {restaurant.description}
        </p>

        {/* Tags */}
        <div className="flex flex-wrap gap-1 mt-2">
          {restaurant.tags.map(t => (
            <span
              key={t}
              style={{
                fontSize: '10px', color: '#FF6B35',
                background: '#FFF0ED', borderRadius: '4px', padding: '1px 6px',
              }}
            >
              #{t}
            </span>
          ))}
        </div>

        {/* Actions */}
        <div className="flex items-center justify-between mt-3 pt-2" style={{ borderTop: '1px solid #f0f0f0' }}>
          <div className="flex items-center gap-3">
            <button
              onClick={() => handleVote('up')}
              className="flex items-center gap-1"
              style={{ opacity: voted === 'down' ? 0.4 : 1 }}
            >
              <ThumbsUp size={13} color={voted === 'up' ? '#2ed573' : '#999'} fill={voted === 'up' ? '#2ed573' : 'none'} />
              <span style={{ fontSize: '11px', color: voted === 'up' ? '#2ed573' : '#999' }}>
                {restaurant.votes.up}
              </span>
            </button>
            <button
              onClick={() => handleVote('down')}
              className="flex items-center gap-1"
              style={{ opacity: voted === 'up' ? 0.4 : 1 }}
            >
              <ThumbsDown size={13} color={voted === 'down' ? '#FF4757' : '#999'} fill={voted === 'down' ? '#FF4757' : 'none'} />
              <span style={{ fontSize: '11px', color: voted === 'down' ? '#FF4757' : '#999' }}>
                {restaurant.votes.down}
              </span>
            </button>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => toggleBlacklist(restaurant.id)}
              className="flex items-center gap-1 px-2 py-1 rounded-lg"
              style={{
                background: restaurant.isBlacklisted ? '#FFE8EA' : '#f5f5f5',
                color: restaurant.isBlacklisted ? '#FF4757' : '#999',
              }}
            >
              <Ban size={11} />
              <span style={{ fontSize: '10px', fontWeight: 600 }}>
                {restaurant.isBlacklisted ? '解除拉黑' : '拉黑'}
              </span>
            </button>
          </div>
        </div>

        {/* Expanded actions */}
        <AnimatePresence>
          {showMenu && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden"
            >
              <div
                className="flex items-center justify-between mt-2 pt-2"
                style={{ borderTop: '1px dashed #f0f0f0' }}
              >
                <span style={{ fontSize: '11px', color: '#bbb' }}>{restaurant.address}</span>
                {restaurant.isUserAdded && (
                  <button
                    onClick={() => deleteRestaurant(restaurant.id)}
                    className="flex items-center gap-1 px-2 py-1 rounded-lg"
                    style={{ background: '#FFE8EA', color: '#FF4757' }}
                  >
                    <Trash2 size={11} />
                    <span style={{ fontSize: '10px', fontWeight: 600 }}>删除</span>
                  </button>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  );
}

export function RestaurantList() {
  const navigate = useNavigate();
  const { restaurants } = useRestaurants();
  const [search, setSearch] = useState('');
  const [activeCategory, setActiveCategory] = useState('全部');
  const [activePriceFilter, setActivePriceFilter] = useState('全部');
  const [showBlacklisted, setShowBlacklisted] = useState(false);

  const filtered = restaurants.filter(r => {
    if (!showBlacklisted && r.isBlacklisted) return false;
    if (activeCategory !== '全部' && r.category !== activeCategory) return false;
    if (activePriceFilter !== '全部') {
      const level = activePriceFilter.length as 1 | 2 | 3;
      if (r.priceLevel !== level) return false;
    }
    if (search) {
      const q = search.toLowerCase();
      return (
        r.name.toLowerCase().includes(q) ||
        r.category.toLowerCase().includes(q) ||
        r.tags.some(t => t.toLowerCase().includes(q)) ||
        r.description.toLowerCase().includes(q)
      );
    }
    return true;
  });

  const activeCount = restaurants.filter(r => !r.isBlacklisted).length;
  const blacklistCount = restaurants.filter(r => r.isBlacklisted).length;

  return (
    <div className="flex flex-col" style={{ minHeight: '100%', background: '#F7F8FA' }}>
      {/* Header */}
      <div style={{ background: 'linear-gradient(135deg, #FF4757, #FF6B35)', padding: '12px 16px 16px' }}>
        <div className="flex items-center gap-2 mb-3">
          <button onClick={() => navigate('/')}>
            <ChevronLeft size={22} color="#fff" />
          </button>
          <h2 style={{ color: '#fff', fontSize: '16px', fontWeight: 700 }}>食堂总览</h2>
          <div className="ml-auto flex items-center gap-2">
            <span
              className="px-2 py-0.5 rounded-full"
              style={{ background: 'rgba(255,255,255,0.2)', color: '#fff', fontSize: '11px' }}
            >
              {activeCount} 可选
            </span>
            {blacklistCount > 0 && (
              <span
                className="px-2 py-0.5 rounded-full"
                style={{ background: 'rgba(0,0,0,0.2)', color: 'rgba(255,255,255,0.8)', fontSize: '11px' }}
              >
                {blacklistCount} 拉黑
              </span>
            )}
          </div>
        </div>

        {/* Search bar */}
        <div
          className="flex items-center gap-2 px-3 py-2 rounded-xl"
          style={{ background: 'rgba(255,255,255,0.95)' }}
        >
          <Search size={14} color="#999" />
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="搜餐厅名、口味、标签..."
            style={{
              flex: 1, background: 'transparent', border: 'none', outline: 'none',
              fontSize: '13px', color: '#333',
            }}
          />
        </div>
      </div>

      {/* Category pills */}
      <div
        className="flex gap-2 px-3 py-3 overflow-x-auto"
        style={{ background: '#fff', borderBottom: '1px solid #f0f0f0', flexShrink: 0 }}
      >
        {CATEGORIES.map(cat => (
          <button
            key={cat}
            onClick={() => setActiveCategory(cat)}
            className="flex-shrink-0 px-3 py-1.5 rounded-full"
            style={{
              background: activeCategory === cat ? '#FF4757' : '#f5f5f5',
              color: activeCategory === cat ? '#fff' : '#666',
              fontSize: '12px',
              fontWeight: activeCategory === cat ? 600 : 400,
              transition: 'all 0.2s',
            }}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Filter row */}
      <div
        className="flex items-center gap-2 px-3 py-2"
        style={{ background: '#fff', borderBottom: '1px solid #f0f0f0' }}
      >
        <Filter size={12} color="#999" />
        <div className="flex gap-1.5">
          {PRICE_FILTERS.map(p => (
            <button
              key={p}
              onClick={() => setActivePriceFilter(p)}
              className="px-2.5 py-1 rounded-lg"
              style={{
                background: activePriceFilter === p ? '#FFF0ED' : '#f8f8f8',
                color: activePriceFilter === p ? '#FF4757' : '#999',
                fontSize: '11px',
                fontWeight: activePriceFilter === p ? 600 : 400,
              }}
            >
              {p}
            </button>
          ))}
        </div>
        <button
          onClick={() => setShowBlacklisted(!showBlacklisted)}
          className="ml-auto px-2.5 py-1 rounded-lg"
          style={{
            background: showBlacklisted ? '#FFE8EA' : '#f8f8f8',
            color: showBlacklisted ? '#FF4757' : '#999',
            fontSize: '11px',
          }}
        >
          {showBlacklisted ? '隐藏雷区' : '显示雷区'}
        </button>
      </div>

      {/* List */}
      <div className="flex-1 overflow-y-auto px-3 py-3">
        {filtered.length === 0 ? (
          <div className="flex flex-col items-center gap-3 py-16">
            <span style={{ fontSize: '48px' }}>🔍</span>
            <p style={{ fontSize: '14px', color: '#999' }}>没找到符合条件的餐厅</p>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            <AnimatePresence>
              {filtered.map(r => (
                <RestaurantCard key={r.id} restaurant={r} />
              ))}
            </AnimatePresence>
          </div>
        )}
        <div style={{ height: 16 }} />
      </div>
    </div>
  );
}
