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
        className="rounded-3xl overflow-hidden"
        style={{
          background: '#fff',
          boxShadow: '0 10px 40px rgba(0,0,0,0.14)',
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
              background: 'linear-gradient(to top, rgba(0,0,0,0.72) 0%, rgba(0,0,0,0) 55%)',
            }}
          />

          {/* WANT stamp */}
          {isTop && (
            <motion.div
              style={{
                opacity: likeOpacity,
                position: 'absolute', top: 20, left: 18,
                border: '3px solid #2ed573', borderRadius: '10px',
                padding: '4px 14px',
                transform: 'rotate(-12deg)',
              }}
            >
              <span style={{ color: '#2ed573', fontSize: '20px', fontWeight: 900, letterSpacing: 1 }}>WANT!</span>
            </motion.div>
          )}

          {/* NOPE stamp */}
          {isTop && (
            <motion.div
              style={{
                opacity: nopeOpacity,
                position: 'absolute', top: 20, right: 18,
                border: '3px solid #FF4757', borderRadius: '10px',
                padding: '4px 14px',
                transform: 'rotate(12deg)',
              }}
            >
              <span style={{ color: '#FF4757', fontSize: '20px', fontWeight: 900, letterSpacing: 1 }}>NOPE</span>
            </motion.div>
          )}

          {/* Category badge */}
          <div
            style={{
              position: 'absolute', top: 14, left: '50%', transform: 'translateX(-50%)',
              background: 'rgba(0,0,0,0.4)', backdropFilter: 'blur(8px)',
              borderRadius: '20px', padding: '4px 14px',
              border: '1px solid rgba(255,255,255,0.2)',
            }}
          >
            <span style={{ color: '#fff', fontSize: '12px', fontWeight: 600 }}>{restaurant.category}</span>
          </div>

          {/* Bottom info on image */}
          <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '14px 16px' }}>
            <h3 style={{ color: '#fff', fontSize: '20px', fontWeight: 800 }}>{restaurant.name}</h3>
            <div className="flex items-center gap-3 mt-1">
              <div className="flex items-center gap-1">
                <Star size={12} fill="#FFA502" color="#FFA502" />
                <span style={{ color: '#FFA502', fontSize: '12px', fontWeight: 700 }}>{restaurant.rating}</span>
              </div>
              <div className="flex items-center gap-1">
                <MapPin size={10} color="rgba(255,255,255,0.75)" />
                <span style={{ color: 'rgba(255,255,255,0.75)', fontSize: '11px' }}>{restaurant.distance}</span>
              </div>
              <span style={{ color: 'rgba(255,255,255,0.75)', fontSize: '11px' }}>
                {'¥'.repeat(restaurant.priceLevel)}
              </span>
            </div>
          </div>
        </div>

        {/* Card body */}
        <div className="px-4 py-3">
          <p style={{ color: '#666', fontSize: '12px', lineHeight: 1.5 }}>{restaurant.description}</p>
          <div className="flex items-center gap-1 mt-1.5">
            <MapPin size={10} color="#bbb" />
            <span style={{ color: '#bbb', fontSize: '10px' }}>{restaurant.address}</span>
          </div>
          <div className="flex flex-wrap gap-1.5 mt-2">
            {restaurant.tags.slice(0, 4).map(t => (
              <span
                key={t}
                style={{
                  fontSize: '10px', color: '#FF6B35',
                  background: '#FFF0ED', borderRadius: '6px', padding: '2px 7px',
                }}
              >
                #{t}
              </span>
            ))}
          </div>
          <div className="flex items-center gap-3 mt-2 pt-2" style={{ borderTop: '1px solid #f5f5f5' }}>
            <span style={{ fontSize: '11px', color: '#2ed573' }}>👍 {restaurant.votes.up}</span>
            <span style={{ fontSize: '11px', color: '#FF4757' }}>👎 {restaurant.votes.down}</span>
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
    <div className="flex flex-col" style={{ minHeight: '100%', background: '#F7F8FA' }}>
      {/* Header */}
      <div
        className="flex items-center justify-between px-4 pt-3 pb-4"
        style={{ background: 'linear-gradient(135deg, #11998e, #38ef7d)' }}
      >
        <div className="flex items-center gap-2">
          <button onClick={() => navigate('/')}>
            <ChevronLeft size={22} color="#fff" />
          </button>
          <div>
            <h2 style={{ color: '#fff', fontSize: '16px', fontWeight: 700 }}>卡片滑选</h2>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span style={{ color: 'rgba(255,255,255,0.9)', fontSize: '12px', fontWeight: 600 }}>
            ❤️ {liked.length} 家
          </span>
          <button
            onClick={handleReset}
            className="flex items-center gap-1 px-2 py-1 rounded-full"
            style={{ background: 'rgba(255,255,255,0.2)' }}
          >
            <RotateCcw size={14} color="#fff" />
            <span style={{ color: '#fff', fontSize: '11px' }}>重置</span>
          </button>
        </div>
      </div>

      {/* Swipe direction hint */}
      <div
        className="flex justify-between px-6 py-2.5"
        style={{ background: '#fff', borderBottom: '1px solid #f0f0f0' }}
      >
        <div className="flex items-center gap-1.5">
          <div
            className="flex items-center justify-center rounded-full"
            style={{ width: 24, height: 24, background: '#FFE8EA' }}
          >
            <X size={12} color="#FF4757" />
          </div>
          <span style={{ fontSize: '11px', color: '#FF4757', fontWeight: 600 }}>左滑不想吃</span>
        </div>
        <div className="flex items-center gap-0.5">
          <span style={{ fontSize: '11px', color: '#999' }}>拖动卡片选择</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span style={{ fontSize: '11px', color: '#2ed573', fontWeight: 600 }}>右滑想吃！</span>
          <div
            className="flex items-center justify-center rounded-full"
            style={{ width: 24, height: 24, background: '#E8FBF0' }}
          >
            <Heart size={12} color="#2ed573" />
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
            <p style={{ fontSize: '18px', fontWeight: 800, color: '#1a1a1a' }}>全部滑完啦！</p>

            {liked.length > 0 ? (
              <>
                <p style={{ fontSize: '13px', color: '#888' }}>你喜欢的 {liked.length} 家餐厅 👇</p>
                <div className="flex flex-col gap-2 w-full">
                  {liked.map((r, i) => (
                    <motion.div
                      key={r.id}
                      initial={{ opacity: 0, x: 30 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: i * 0.07 }}
                      className="flex items-center gap-3 p-3 rounded-2xl"
                      style={{ background: '#fff', boxShadow: '0 2px 10px rgba(0,0,0,0.06)' }}
                    >
                      <img src={r.image} alt={r.name} style={{ width: 44, height: 44, borderRadius: 10, objectFit: 'cover' }} />
                      <div className="flex-1 min-w-0">
                        <p style={{ fontSize: '13px', fontWeight: 700, color: '#1a1a1a' }}>{r.name}</p>
                        <p style={{ fontSize: '11px', color: '#999' }}>{r.category} · {r.distance}</p>
                      </div>
                      <Heart size={16} color="#FF4757" fill="#FF4757" />
                    </motion.div>
                  ))}
                </div>
              </>
            ) : (
              <p style={{ fontSize: '14px', color: '#999' }}>哇，一家都不喜欢？也太难伺候了吧 😅</p>
            )}

            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={handleReset}
              className="mt-2 px-10 py-3.5 rounded-full"
              style={{
                background: 'linear-gradient(135deg, #11998e, #38ef7d)',
                color: '#fff', fontSize: '15px', fontWeight: 700,
                boxShadow: '0 6px 20px rgba(17,153,142,0.3)',
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
              className="flex items-center justify-center rounded-full"
              style={{
                width: 60, height: 60,
                background: '#fff',
                boxShadow: '0 4px 20px rgba(255,71,87,0.25)',
                border: '2.5px solid #FF4757',
              }}
            >
              <X size={26} color="#FF4757" strokeWidth={2.5} />
            </motion.button>

            {history.length > 0 && (
              <motion.button
                whileTap={{ scale: 0.85 }}
                onClick={handleUndo}
                className="flex items-center justify-center rounded-full"
                style={{
                  width: 40, height: 40,
                  background: '#fff',
                  boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
                  border: '1.5px solid #ddd',
                }}
              >
                <RotateCcw size={16} color="#aaa" />
              </motion.button>
            )}

            <motion.button
              whileTap={{ scale: 0.85 }}
              onClick={() => handleSwipe('right')}
              className="flex items-center justify-center rounded-full"
              style={{
                width: 60, height: 60,
                background: '#fff',
                boxShadow: '0 4px 20px rgba(46,213,115,0.3)',
                border: '2.5px solid #2ed573',
              }}
            >
              <Heart size={26} color="#2ed573" strokeWidth={2.5} />
            </motion.button>
          </div>

          {/* Progress bar */}
          <div className="px-6 pb-3">
            <div className="flex justify-between mb-1">
              <span style={{ fontSize: '10px', color: '#bbb' }}>还剩 {cardQueue.length} 家</span>
              <span style={{ fontSize: '10px', color: '#bbb' }}>已看 {history.length} 家</span>
            </div>
            <div style={{ height: 3, background: '#eee', borderRadius: 2 }}>
              <div
                style={{
                  height: '100%',
                  width: `${(history.length / (history.length + cardQueue.length)) * 100}%`,
                  background: 'linear-gradient(135deg, #11998e, #38ef7d)',
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
                background: 'linear-gradient(135deg, #11998e, #38ef7d)',
                boxShadow: '0 20px 60px rgba(17,153,142,0.5)',
                minWidth: '180px',
              }}
            >
              <div style={{ fontSize: '36px' }}>💚</div>
              <p style={{ color: 'rgba(255,255,255,0.85)', fontSize: '11px', marginTop: 4 }}>右滑了！</p>
              <p style={{ color: '#fff', fontSize: '17px', fontWeight: 800 }}>{showMatch.name}</p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
