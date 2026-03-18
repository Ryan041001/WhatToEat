import { useState, useRef, useCallback } from 'react';
import { motion, useMotionValue, useTransform, AnimatePresence } from 'motion/react';
import { ChevronLeft, Star, MapPin, RotateCcw, Heart, X } from 'lucide-react';
import { useNavigate } from 'react-router';
import { useRestaurants, Restaurant } from '../context/RestaurantContext';

function SwipeCard({
  restaurant,
  onSwipe,
  isTop,
  stackIndex,
}: {
  restaurant: Restaurant;
  onSwipe: (dir: 'left' | 'right') => void;
  isTop: boolean;
  stackIndex: number;
}) {
  const x = useMotionValue(0);
  const rotate = useTransform(x, [-200, 200], [-20, 20]);
  const likeOpacity = useTransform(x, [30, 100], [0, 1]);
  const nopeOpacity = useTransform(x, [-100, -30], [1, 0]);

  const handleDragEnd = (_: any, info: any) => {
    const threshold = 100;
    const velThreshold = 400;
    if (info.offset.x > threshold || info.velocity.x > velThreshold) {
      onSwipe('right');
    } else if (info.offset.x < -threshold || info.velocity.x < -velThreshold) {
      onSwipe('left');
    }
  };

  const scaleVal = 1 - stackIndex * 0.05;
  const yVal = stackIndex * 10;

  return (
    <motion.div
      drag={isTop ? 'x' : false}
      dragConstraints={{ left: 0, right: 0 }}
      onDragEnd={handleDragEnd}
      animate={{ scale: scaleVal, y: yVal }}
      transition={{ type: 'spring', stiffness: 280, damping: 22 }}
      style={{
        x: isTop ? x : 0,
        rotate: isTop ? rotate : 0,
        position: 'absolute',
        width: '100%',
        zIndex: 10 - stackIndex,
        cursor: isTop ? 'grab' : 'default',
        touchAction: 'none',
        userSelect: 'none',
        WebkitUserSelect: 'none',
      }}
      whileDrag={{ cursor: 'grabbing', scale: 1.02 }}
    >
      <div
        className="glass-panel glass-blur-sm rounded-3xl overflow-hidden"
        style={{
          boxShadow: 'var(--glass-shadow-ambient)',
          background: 'var(--glass-surface-strong)',
          border: '1px solid var(--glass-border-medium)',
        }}
      >
        {/* Image area */}
        <div style={{ position: 'relative', height: '280px' }}>
          <img
            src={restaurant.image}
            alt={restaurant.name}
            style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block', pointerEvents: 'none' }}
            draggable={false}
          />

          {/* Dark gradient */}
          <div
            style={{
              position: 'absolute', inset: 0,
              background: 'linear-gradient(to top, color-mix(in srgb, var(--foreground) 72%, transparent) 0%, color-mix(in srgb, var(--foreground) 0%, transparent) 55%)',
            }}
          />

          {/* WANT stamp */}
          {isTop && (
            <motion.div
              style={{
                opacity: likeOpacity,
                position: 'absolute', top: 20, left: 18,
                border: '3px solid var(--color-success)', borderRadius: '10px',
                padding: '4px 14px',
                transform: 'rotate(-12deg)',
              }}
            >
              <span style={{ color: 'var(--color-success)', fontSize: '20px', fontWeight: 900, letterSpacing: 1 }}>WANT!</span>
            </motion.div>
          )}

          {/* NOPE stamp */}
          {isTop && (
            <motion.div
              style={{
                opacity: nopeOpacity,
                position: 'absolute', top: 20, right: 18,
                border: '3px solid var(--primary)', borderRadius: '10px',
                padding: '4px 14px',
                transform: 'rotate(12deg)',
              }}
            >
              <span style={{ color: 'var(--primary)', fontSize: '20px', fontWeight: 900, letterSpacing: 1 }}>NOPE</span>
            </motion.div>
          )}

          {/* Category badge */}
          <div
            className="glass-chip glass-blur-sm"
            style={{
              position: 'absolute', top: 14, left: '50%', transform: 'translateX(-50%)',
              background: 'var(--glass-surface-strong)',
              borderRadius: '20px', padding: '4px 14px',
              border: '1px solid var(--glass-border-medium)',
            }}
          >
            <span style={{ color: 'var(--primary-foreground)', fontSize: '12px', fontWeight: 600 }}>{restaurant.category}</span>
          </div>

          {/* Bottom info on image */}
          <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '14px 16px' }}>
            <h3 style={{ color: 'var(--primary-foreground)', fontSize: '20px', fontWeight: 800 }}>{restaurant.name}</h3>
            <div className="flex items-center gap-3 mt-1">
              <div className="flex items-center gap-1">
                <Star size={12} fill="var(--color-warning)" color="var(--color-warning)" />
                <span style={{ color: 'var(--color-warning)', fontSize: '12px', fontWeight: 700 }}>{restaurant.rating}</span>
              </div>
              <div className="flex items-center gap-1">
                <MapPin size={10} color="color-mix(in srgb, var(--glass-text-inverse) 75%, transparent)" />
                <span style={{ color: 'color-mix(in srgb, var(--glass-text-inverse) 75%, transparent)', fontSize: '11px' }}>{restaurant.distance}</span>
              </div>
              <span style={{ color: 'color-mix(in srgb, var(--glass-text-inverse) 75%, transparent)', fontSize: '11px' }}>
                {'¥'.repeat(restaurant.priceLevel)}
              </span>
            </div>
          </div>
        </div>

        {/* Card body */}
        <div className="px-4 py-3">
          <p style={{ color: 'var(--muted-foreground)', fontSize: '12px', lineHeight: 1.5 }}>{restaurant.description}</p>
          <div className="flex items-center gap-1 mt-1.5">
            <MapPin size={10} color="var(--muted-foreground)" />
            <span style={{ color: 'var(--muted-foreground)', fontSize: '10px' }}>{restaurant.address}</span>
          </div>
          <div className="flex flex-wrap gap-1.5 mt-2">
            {restaurant.tags.slice(0, 4).map(t => (
              <span
                key={t}
                style={{
                  fontSize: '10px', color: 'var(--primary)',
                  background: 'color-mix(in srgb, var(--primary) 12%, transparent)', borderRadius: '6px', padding: '2px 7px',
                }}
              >
                #{t}
              </span>
            ))}
          </div>
          <div className="flex items-center gap-3 mt-2 pt-2" style={{ borderTop: '1px solid var(--glass-border-soft)' }}>
            <span style={{ fontSize: '11px', color: 'var(--color-success)' }}>👍 {restaurant.votes.up}</span>
            <span style={{ fontSize: '11px', color: 'var(--primary)' }}>👎 {restaurant.votes.down}</span>
          </div>
        </div>
      </div>
    </motion.div>
  );
}

export function CardSwipe() {
  const navigate = useNavigate();
  const { getActiveRestaurants, voteRestaurant } = useRestaurants();

  const buildQueue = useCallback(() => {
    const actives = getActiveRestaurants();
    return [...actives].sort(() => Math.random() - 0.5);
  }, []);

  const [cardQueue, setCardQueue] = useState<Restaurant[]>(buildQueue);
  const [liked, setLiked] = useState<Restaurant[]>([]);
  const [history, setHistory] = useState<{ r: Restaurant; dir: 'left' | 'right' }[]>([]);
  const [showMatch, setShowMatch] = useState<Restaurant | null>(null);
  const matchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const visibleCards = cardQueue.slice(0, 3);

  const handleSwipe = (dir: 'left' | 'right') => {
    if (cardQueue.length === 0) return;
    const current = cardQueue[0];
    if (dir === 'right') {
      voteRestaurant(current.id, 'up');
      setLiked(prev => [...prev, current]);
      if (matchTimerRef.current) clearTimeout(matchTimerRef.current);
      setShowMatch(current);
      matchTimerRef.current = setTimeout(() => setShowMatch(null), 2000);
    } else {
      voteRestaurant(current.id, 'down');
    }
    setHistory(prev => [...prev, { r: current, dir }]);
    setCardQueue(prev => prev.slice(1));
  };

  const handleUndo = () => {
    if (history.length === 0) return;
    const last = history[history.length - 1];
    setHistory(prev => prev.slice(0, -1));
    setCardQueue(prev => [last.r, ...prev]);
    if (last.dir === 'right') {
      setLiked(prev => prev.filter(r => r.id !== last.r.id));
    }
  };

  const handleReset = () => {
    setCardQueue(buildQueue());
    setLiked([]);
    setHistory([]);
  };

  return (
    <div className="flex flex-col" style={{ minHeight: '100%', background: 'var(--color-background)' }}>
      {/* Header */}
      <div
        className="flex items-center justify-between px-4 pt-3 pb-4"
        style={{ background: 'linear-gradient(135deg, var(--accent), var(--color-success))' }}
      >
        <div className="flex items-center gap-2">
          <button onClick={() => navigate('/')}>
            <ChevronLeft size={22} color="var(--primary-foreground)" />
          </button>
          <div>
            <h2 style={{ color: 'var(--primary-foreground)', fontSize: '16px', fontWeight: 700 }}>卡片滑选</h2>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span style={{ color: 'color-mix(in srgb, var(--primary-foreground) 90%, transparent)', fontSize: '12px', fontWeight: 600 }}>
            ❤️ {liked.length} 家
          </span>
          <button
            onClick={handleReset}
            className="glass-chip glass-blur-sm click-hover-lift flex items-center gap-1 px-2 py-1 rounded-full"
            style={{
              background: 'var(--glass-surface-strong)',
              border: '1px solid var(--glass-border-medium)',
            }}
          >
            <RotateCcw size={14} color="var(--primary-foreground)" />
            <span style={{ color: 'var(--primary-foreground)', fontSize: '11px' }}>重置</span>
          </button>
        </div>
      </div>

      {/* Swipe direction hint */}
      <div
        className="glass-blur-sm flex justify-between px-6 py-2.5"
        style={{ background: 'var(--glass-surface-strong)', borderBottom: '1px solid var(--glass-border-medium)' }}
      >
        <div className="flex items-center gap-1.5">
          <div
            className="glass-chip glass-blur-sm flex items-center justify-center rounded-full"
            style={{
              width: 24,
              height: 24,
              background: 'var(--glass-surface-strong)',
              border: '1px solid var(--glass-border-medium)',
            }}
          >
            <X size={12} color="var(--primary)" />
          </div>
          <span style={{ fontSize: '11px', color: 'var(--primary)', fontWeight: 600 }}>左滑不想吃</span>
        </div>
        <div className="flex items-center gap-0.5">
          <span style={{ fontSize: '11px', color: 'var(--glass-text-muted-transparent)' }}>拖动卡片选择</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span style={{ fontSize: '11px', color: 'var(--color-success)', fontWeight: 600 }}>右滑想吃！</span>
          <div
            className="glass-chip glass-blur-sm flex items-center justify-center rounded-full"
            style={{
              width: 24,
              height: 24,
              background: 'var(--glass-surface-strong)',
              border: '1px solid var(--glass-border-medium)',
            }}
          >
            <Heart size={12} color="var(--color-success)" />
          </div>
        </div>
      </div>

      {/* Cards area */}
      <div className="flex-1 flex flex-col items-center justify-start px-5 pt-4">
        {cardQueue.length === 0 ? (
          /* Done state */
          <div className="flex flex-col items-center gap-4 py-8 w-full">
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: 'spring', stiffness: 200, damping: 15 }}
              style={{ fontSize: '64px' }}
            >
              🎊
            </motion.div>
            <p style={{ fontSize: '18px', fontWeight: 800, color: 'var(--foreground)' }}>全部滑完啦！</p>

            {liked.length > 0 ? (
              <>
                <p style={{ fontSize: '13px', color: 'var(--glass-text-muted-transparent)' }}>你喜欢的 {liked.length} 家餐厅 👇</p>
                <div className="flex flex-col gap-2 w-full">
                  {liked.map((r, i) => (
                    <motion.div
                      key={r.id}
                      initial={{ opacity: 0, x: 30 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: i * 0.07 }}
                      className="glass-blur-sm flex items-center gap-3 p-3 rounded-2xl"
                      style={{ background: 'var(--glass-surface-strong)', border: '1px solid var(--glass-border-medium)', boxShadow: 'var(--glass-shadow-ambient)' }}
                    >
                      <img src={r.image} alt={r.name} style={{ width: 44, height: 44, borderRadius: 10, objectFit: 'cover' }} />
                      <div className="flex-1 min-w-0">
                        <p style={{ fontSize: '13px', fontWeight: 700, color: 'var(--foreground)' }}>{r.name}</p>
                        <p style={{ fontSize: '11px', color: 'var(--muted-foreground)' }}>{r.category} · {r.distance}</p>
                      </div>
                      <Heart size={16} color="var(--primary)" fill="var(--primary)" />
                    </motion.div>
                  ))}
                </div>
              </>
            ) : (
              <p style={{ fontSize: '14px', color: 'var(--glass-text-muted-transparent)' }}>哇，一家都不喜欢？也太难伺候了吧 😅</p>
            )}

            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={handleReset}
              className="mt-2 px-10 py-3.5 rounded-full"
              style={{
                background: 'linear-gradient(135deg, var(--accent), var(--color-success))',
                color: 'var(--primary-foreground)', fontSize: '15px', fontWeight: 700,
                boxShadow: 'var(--glass-shadow-glow)',
              }}
            >
              重新来一遍
            </motion.button>
          </div>
        ) : (
          <div
            style={{
              position: 'relative',
              width: '100%',
              height: '400px',
            }}
          >
            <AnimatePresence>
              {[...visibleCards].reverse().map((restaurant, reverseIdx) => {
                const stackIndex = visibleCards.length - 1 - reverseIdx;
                return (
                  <SwipeCard
                    key={restaurant.id}
                    restaurant={restaurant}
                    onSwipe={handleSwipe}
                    isTop={stackIndex === 0}
                    stackIndex={stackIndex}
                  />
                );
              })}
            </AnimatePresence>
          </div>
        )}
      </div>

      {/* Bottom action buttons */}
      {cardQueue.length > 0 && (
        <div className="flex-shrink-0">
          <div className="flex justify-center items-center gap-5 py-3 px-4">
            <motion.button
              whileTap={{ scale: 0.85 }}
              onClick={() => handleSwipe('left')}
              className="glass-chip glass-blur-sm click-hover-lift flex items-center justify-center rounded-full"
              style={{
                width: 60,
                height: 60,
                background: 'var(--glass-surface-strong)',
                boxShadow: 'var(--neumorph-shadow-soft)',
                border: '1px solid var(--glass-border-medium)',
              }}
            >
              <X size={26} color="var(--primary)" strokeWidth={2.5} />
            </motion.button>

            {history.length > 0 && (
              <motion.button
                whileTap={{ scale: 0.85 }}
                onClick={handleUndo}
                className="glass-chip glass-blur-sm click-hover-lift flex items-center justify-center rounded-full"
                style={{
                  width: 40,
                  height: 40,
                  background: 'var(--glass-surface-strong)',
                  boxShadow: 'var(--neumorph-shadow-soft)',
                  border: '1px solid var(--glass-border-medium)',
                }}
              >
                <RotateCcw size={16} color="var(--muted-foreground)" />
              </motion.button>
            )}

            <motion.button
              whileTap={{ scale: 0.85 }}
              onClick={() => handleSwipe('right')}
              className="glass-chip glass-blur-sm click-hover-lift flex items-center justify-center rounded-full"
              style={{
                width: 60,
                height: 60,
                background: 'var(--glass-surface-strong)',
                boxShadow: 'var(--neumorph-shadow-soft)',
                border: '1px solid var(--glass-border-medium)',
              }}
            >
              <Heart size={26} color="var(--color-success)" strokeWidth={2.5} />
            </motion.button>
          </div>

          {/* Progress bar */}
          <div className="px-6 pb-3">
            <div className="flex justify-between mb-1">
              <span style={{ fontSize: '10px', color: 'var(--muted-foreground)' }}>还剩 {cardQueue.length} 家</span>
              <span style={{ fontSize: '10px', color: 'var(--muted-foreground)' }}>已看 {history.length} 家</span>
            </div>
            <div className="glass-blur-sm" style={{ height: 3, background: 'var(--glass-surface-strong)', border: '1px solid var(--glass-border-medium)', borderRadius: 2 }}>
              <div
                style={{
                  height: '100%',
                  width: `${(history.length / (history.length + cardQueue.length)) * 100}%`,
                  background: 'linear-gradient(135deg, var(--accent), var(--color-success))',
                  borderRadius: 2,
                  transition: 'width 0.3s ease',
                }}
              />
            </div>
          </div>
        </div>
      )}

      {/* Match popup */}
      <AnimatePresence>
        {showMatch && (
          <motion.div
            initial={{ opacity: 0, scale: 0.7, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.7, y: 20 }}
            style={{
              position: 'absolute',
              top: '45%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              zIndex: 50,
              pointerEvents: 'none',
            }}
          >
            <div
              className="px-7 py-5 rounded-3xl text-center"
              style={{
                background: 'linear-gradient(135deg, var(--accent), var(--color-success))',
                boxShadow: 'var(--glass-shadow-glow)',
                minWidth: '180px',
              }}
            >
              <div style={{ fontSize: '36px' }}>💚</div>
              <p style={{ color: 'color-mix(in srgb, var(--primary-foreground) 85%, transparent)', fontSize: '11px', marginTop: 4 }}>右滑了！</p>
              <p style={{ color: 'var(--primary-foreground)', fontSize: '17px', fontWeight: 800 }}>{showMatch.name}</p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
