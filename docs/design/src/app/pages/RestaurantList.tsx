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
      className="glass-panel glass-blur-sm enhanced-neumorph-card neumorph-strong click-hover-lift overflow-hidden rounded-2xl"
      style={{
        background: restaurant.isBlacklisted ? 'var(--glass-surface-light)' : 'var(--glass-surface-medium)',
        boxShadow: 'var(--glass-shadow-ambient)',
        opacity: restaurant.isBlacklisted ? 0.75 : 1,
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
              background: 'color-mix(in srgb, var(--foreground) 50%, transparent)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
          >
            <div className="flex flex-col items-center gap-1">
              <Ban size={28} color="var(--primary-foreground)" />
              <span style={{ color: 'var(--primary-foreground)', fontSize: '12px', fontWeight: 700 }}>已拉黑</span>
            </div>
          </div>
        )}
        {/* Category */}
        <div
          className="glass-chip glass-blur-sm"
          style={{
            position: 'absolute', top: 8, left: 8,
            background: 'var(--glass-surface-strong)',
            border: '1px solid var(--glass-border-medium)',
            borderRadius: '12px', padding: '4px 10px',
          }}
        >
          <span style={{ color: 'var(--foreground)', fontSize: '11px', fontWeight: 700 }}>{restaurant.category}</span>
        </div>
        {restaurant.isUserAdded && (
          <div
            className="glass-chip glass-blur-sm"
            style={{
              position: 'absolute', top: 8, right: 8,
              background: 'var(--glass-surface-strong)',
              border: '1px solid var(--glass-border-medium)',
              borderRadius: '12px', padding: '4px 10px',
            }}
          >
            <span style={{ color: 'var(--warning)', fontSize: '10px', fontWeight: 700 }}>我添加的</span>
          </div>
        )}
        {/* More button */}
        <button
          onClick={() => setShowMenu(!showMenu)}
          className="glass-chip glass-blur-sm"
          style={{
            position: 'absolute', bottom: 10, right: 10,
            background: 'var(--glass-surface-strong)',
            border: '1px solid var(--glass-border-medium)',
            borderRadius: '50%', width: 36, height: 36,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}
        >
          <ChevronDown size={16} color="var(--foreground)" />
        </button>
      </div>

      <div
        className="glass-panel glass-blur-md px-3 py-3"
        style={{
          background: 'var(--glass-surface-light)',
          borderTop: '1px solid var(--glass-border-soft)',
          backdropFilter: 'blur(14px)',
          WebkitBackdropFilter: 'blur(14px)',
        }}
      >
        <div className="flex items-start justify-between">
          <div className="flex-1 min-w-0">
            <h3 style={{ fontSize: '14px', fontWeight: 700, color: 'var(--foreground)' }}>{restaurant.name}</h3>
            <div className="flex items-center gap-2 mt-0.5">
              <div className="flex items-center gap-0.5">
                <Star size={11} fill="var(--color-warning)" color="var(--color-warning)" />
                <span style={{ fontSize: '11px', color: 'var(--color-warning)', fontWeight: 600 }}>{restaurant.rating}</span>
              </div>
              <span style={{ fontSize: '11px', color: 'var(--muted-foreground)' }}>{'¥'.repeat(restaurant.priceLevel)}</span>
              <div className="flex items-center gap-0.5">
                <MapPin size={9} color="var(--muted-foreground)" />
                <span style={{ fontSize: '10px', color: 'var(--muted-foreground)' }}>{restaurant.distance}</span>
              </div>
            </div>
          </div>
        </div>

        <p style={{ fontSize: '11px', color: 'var(--glass-text-muted-transparent)', marginTop: '4px', lineHeight: 1.4 }}>
          {restaurant.description}
        </p>

        {/* Tags */}
        <div className="flex flex-wrap gap-1 mt-2">
          {restaurant.tags.map(t => (
            <span
              key={t}
              className="glass-chip glass-blur-sm"
              style={{
                fontSize: '10px', color: 'var(--warning)',
                background: 'color-mix(in srgb, var(--glass-surface-light) 86%, transparent)', borderRadius: '4px', padding: '1px 6px',
              }}
            >
              #{t}
            </span>
          ))}
        </div>

        {/* Actions */}
        <div className="flex items-center justify-between mt-3 pt-2" style={{ borderTop: '1px solid var(--glass-border-soft)' }}>
          <div className="flex items-center gap-3">
            <button
              onClick={() => handleVote('up')}
              className="flex items-center gap-1 click-hover-lift"
              style={{ opacity: voted === 'down' ? 0.4 : 1 }}
            >
              <ThumbsUp size={13} color={voted === 'up' ? 'var(--warning)' : 'var(--muted-foreground)'} fill={voted === 'up' ? 'var(--warning)' : 'none'} />
              <span style={{ fontSize: '11px', color: voted === 'up' ? 'var(--warning)' : 'var(--muted-foreground)' }}>
                {restaurant.votes.up}
              </span>
            </button>
            <button
              onClick={() => handleVote('down')}
              className="flex items-center gap-1 click-hover-lift"
              style={{ opacity: voted === 'up' ? 0.4 : 1 }}
            >
              <ThumbsDown size={13} color={voted === 'down' ? 'var(--primary)' : 'var(--muted-foreground)'} fill={voted === 'down' ? 'var(--primary)' : 'none'} />
              <span style={{ fontSize: '11px', color: voted === 'down' ? 'var(--primary)' : 'var(--muted-foreground)' }}>
                {restaurant.votes.down}
              </span>
            </button>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => toggleBlacklist(restaurant.id)}
              className="flex items-center gap-1 px-2 py-1 rounded-lg click-hover-lift"
              style={{
                background: restaurant.isBlacklisted ? 'var(--glass-surface-light)' : 'var(--glass-surface-medium)',
                color: restaurant.isBlacklisted ? 'var(--primary)' : 'var(--muted-foreground)',
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
                style={{ borderTop: '1px dashed var(--glass-border-soft)' }}
              >
                <span style={{ fontSize: '11px', color: 'var(--glass-text-muted-transparent)' }}>{restaurant.address}</span>
                {restaurant.isUserAdded && (
                  <button
                    onClick={() => deleteRestaurant(restaurant.id)}
                    className="flex items-center gap-1 px-2 py-1 rounded-lg click-hover-lift"
                    style={{ background: 'var(--glass-surface-light)', color: 'var(--primary)' }}
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
    <div className="flex flex-col" style={{ minHeight: '100%', background: 'var(--color-background)' }}>
      {/* Header */}
      <div className="unified-topbar" style={{ background: 'linear-gradient(135deg, var(--primary), var(--warning))', padding: '12px 16px 16px' }}>
        <div className="flex items-center gap-2 mb-3">
          <button onClick={() => navigate('/')}>
            <ChevronLeft size={22} color="var(--primary-foreground)" />
          </button>
          <h2 style={{ color: 'var(--primary-foreground)', fontSize: '16px', fontWeight: 700 }}>食堂总览</h2>
          <div className="ml-auto flex items-center gap-2">
            <span
              className="glass-chip glass-blur-sm px-2 py-0.5 rounded-full"
              style={{ background: 'color-mix(in srgb, var(--glass-surface-strong) 20%, transparent)', color: 'var(--primary-foreground)', fontSize: '11px' }}
            >
              {activeCount} 可选
            </span>
            {blacklistCount > 0 && (
              <span
                className="glass-chip glass-blur-sm px-2 py-0.5 rounded-full"
                style={{ background: 'color-mix(in srgb, var(--foreground) 20%, transparent)', color: 'color-mix(in srgb, var(--glass-text-inverse) 80%, transparent)', fontSize: '11px' }}
              >
                {blacklistCount} 拉黑
              </span>
            )}
          </div>
        </div>

        {/* Search bar */}
        <div
          className="glass-overlay glass-blur-sm flex items-center gap-2 px-3 py-2 rounded-xl"
          style={{ background: 'color-mix(in srgb, var(--glass-surface-strong) 94%, transparent)' }}
        >
          <Search size={14} color="var(--muted-foreground)" />
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="搜餐厅名、口味、标签..."
            style={{
              flex: 1, background: 'transparent', border: 'none', outline: 'none',
              fontSize: '13px', color: 'var(--foreground)',
            }}
          />
        </div>
      </div>

      {/* Category pills */}
      <div
        className="flex gap-2 px-3 py-3 overflow-x-auto"
        style={{ background: 'var(--primary-foreground)', borderBottom: '1px solid var(--glass-border-soft)', flexShrink: 0 }}
      >
        {CATEGORIES.map(cat => (
          <button
            key={cat}
            onClick={() => setActiveCategory(cat)}
            className="glass-chip glass-blur-sm flex-shrink-0 px-3 py-1.5 rounded-full click-hover-lift"
            style={{
              background: activeCategory === cat ? 'var(--primary)' : 'var(--glass-surface-medium)',
              color: activeCategory === cat ? 'var(--primary-foreground)' : 'var(--muted-foreground)',
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
        style={{ background: 'var(--primary-foreground)', borderBottom: '1px solid var(--glass-border-soft)' }}
      >
        <Filter size={12} color="var(--muted-foreground)" />
        <div className="flex gap-1.5">
          {PRICE_FILTERS.map(p => (
            <button
              key={p}
              onClick={() => setActivePriceFilter(p)}
              className="px-2.5 py-1 rounded-lg"
              style={{
                background: activePriceFilter === p ? 'var(--glass-surface-light)' : 'var(--glass-surface-medium)',
                color: activePriceFilter === p ? 'var(--primary)' : 'var(--muted-foreground)',
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
            background: showBlacklisted ? 'var(--glass-surface-light)' : 'var(--glass-surface-medium)',
            color: showBlacklisted ? 'var(--primary)' : 'var(--muted-foreground)',
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
            <p style={{ fontSize: '14px', color: 'var(--muted-foreground)' }}>没找到符合条件的餐厅</p>
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
