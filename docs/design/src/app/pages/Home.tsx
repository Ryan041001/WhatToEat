import { useState } from 'react';
import { useNavigate } from 'react-router';
import { motion, AnimatePresence } from 'motion/react';
import { Shuffle, Layers, Zap, Plus, Star, MapPin, TrendingUp } from 'lucide-react';
import { useRestaurants } from '../context/RestaurantContext';

function ShakeButton({ onResult }: { onResult: (name: string) => void }) {
  const { getActiveRestaurants } = useRestaurants();
  const [shaking, setShaking] = useState(false);

  const handleShake = () => {
    if (shaking) return;
    setShaking(true);
    const actives = getActiveRestaurants();
    if (actives.length === 0) return;
    const pick = actives[Math.floor(Math.random() * actives.length)];
    setTimeout(() => {
      setShaking(false);
      onResult(pick.name);
    }, 800);
  };

  return (
    <motion.button
      animate={shaking ? {
        rotate: [0, -15, 15, -12, 12, -8, 8, -4, 4, 0],
        scale: [1, 1.1, 1.1, 1.1, 1.1, 1.05, 1.05, 1.02, 1.02, 1],
      } : {}}
      transition={{ duration: 0.8 }}
      onClick={handleShake}
      className="flex flex-col items-center gap-2 p-4 rounded-2xl cursor-pointer select-none active:scale-95"
      style={{ background: 'linear-gradient(135deg, #FF6B35, #FF4757)', flex: 1 }}
    >
      <span style={{ fontSize: '28px' }}>📱</span>
      <span style={{ color: '#fff', fontSize: '12px', fontWeight: 600 }}>摇一摇</span>
    </motion.button>
  );
}

export function Home() {
  const navigate = useNavigate();
  const { getActiveRestaurants, restaurants } = useRestaurants();
  const [shakeResult, setShakeResult] = useState<string | null>(null);
  const actives = getActiveRestaurants();
  const topRated = [...actives].sort((a, b) => b.rating - a.rating).slice(0, 3);

  return (
    <div className="flex flex-col" style={{ minHeight: '100%' }}>
      {/* Header */}
      <div
        className="relative overflow-hidden px-5 pt-4 pb-6"
        style={{
          background: 'linear-gradient(135deg, #FF4757 0%, #FF6B35 50%, #FFA502 100%)',
        }}
      >
        {/* Decorative blobs */}
        <div style={{ position: 'absolute', top: -20, right: -20, width: 100, height: 100, borderRadius: '50%', background: 'rgba(255,255,255,0.1)' }} />
        <div style={{ position: 'absolute', bottom: -30, left: 60, width: 80, height: 80, borderRadius: '50%', background: 'rgba(255,255,255,0.08)' }} />

        <div className="relative">
          <div className="flex items-center justify-between mb-3">
            <div>
              <p style={{ color: 'rgba(255,255,255,0.85)', fontSize: '12px' }}>今天吃什么？</p>
              <h1 style={{ color: '#fff', fontSize: '22px', fontWeight: 700, lineHeight: 1.2 }}>
                干饭防纠结 🍚
              </h1>
            </div>
            <div
              className="flex items-center gap-1 px-3 py-1.5 rounded-full"
              style={{ background: 'rgba(255,255,255,0.2)' }}
            >
              <span style={{ color: '#fff', fontSize: '11px' }}>{actives.length} 家可选</span>
            </div>
          </div>

          {/* Today's pick button */}
          <motion.button
            whileTap={{ scale: 0.97 }}
            onClick={() => navigate('/spin')}
            className="w-full flex items-center justify-between px-4 py-3 rounded-2xl"
            style={{ background: 'rgba(255,255,255,0.2)', backdropFilter: 'blur(10px)', border: '1px solid rgba(255,255,255,0.3)' }}
          >
            <div className="flex items-center gap-3">
              <span style={{ fontSize: '24px' }}>🎰</span>
              <div className="text-left">
                <p style={{ color: '#fff', fontSize: '14px', fontWeight: 600 }}>随机替我选一个！</p>
                <p style={{ color: 'rgba(255,255,255,0.75)', fontSize: '11px' }}>告别选择困难症</p>
              </div>
            </div>
            <div className="rounded-full px-3 py-1" style={{ background: '#fff' }}>
              <span style={{ color: '#FF4757', fontSize: '12px', fontWeight: 700 }}>开始</span>
            </div>
          </motion.button>
        </div>
      </div>

      {/* Quick actions */}
      <div className="px-4 -mt-2">
        <div
          className="flex gap-3 p-4 rounded-2xl"
          style={{ background: '#fff', boxShadow: '0 4px 16px rgba(0,0,0,0.06)' }}
        >
          {/* Spin wheel */}
          <motion.button
            whileTap={{ scale: 0.93 }}
            onClick={() => navigate('/spin')}
            className="flex flex-col items-center gap-2 p-4 rounded-2xl cursor-pointer"
            style={{ background: 'linear-gradient(135deg, #667eea, #764ba2)', flex: 1 }}
          >
            <span style={{ fontSize: '28px' }}>🎡</span>
            <span style={{ color: '#fff', fontSize: '12px', fontWeight: 600 }}>大转盘</span>
          </motion.button>

          {/* Shake */}
          <ShakeButton onResult={setShakeResult} />

          {/* Card swipe */}
          <motion.button
            whileTap={{ scale: 0.93 }}
            onClick={() => navigate('/swipe')}
            className="flex flex-col items-center gap-2 p-4 rounded-2xl cursor-pointer"
            style={{ background: 'linear-gradient(135deg, #11998e, #38ef7d)', flex: 1 }}
          >
            <span style={{ fontSize: '28px' }}>💘</span>
            <span style={{ color: '#fff', fontSize: '12px', fontWeight: 600 }}>卡片滑</span>
          </motion.button>
        </div>
      </div>

      {/* Shake result popup */}
      <AnimatePresence>
        {shakeResult && (
          <motion.div
            initial={{ opacity: 0, scale: 0.8, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.8, y: 20 }}
            className="fixed inset-0 flex items-center justify-center z-50"
            style={{ pointerEvents: 'none' }}
          >
            <div
              className="px-8 py-6 rounded-3xl text-center"
              style={{
                background: 'linear-gradient(135deg, #FF4757, #FF6B35)',
                boxShadow: '0 20px 60px rgba(255,71,87,0.5)',
                pointerEvents: 'all',
                maxWidth: '280px',
              }}
              onClick={() => setShakeResult(null)}
            >
              <div style={{ fontSize: '48px', marginBottom: '8px' }}>🎉</div>
              <p style={{ color: 'rgba(255,255,255,0.85)', fontSize: '14px' }}>摇到了！就吃</p>
              <p style={{ color: '#fff', fontSize: '22px', fontWeight: 700, margin: '4px 0' }}>{shakeResult}</p>
              <p style={{ color: 'rgba(255,255,255,0.7)', fontSize: '12px' }}>点击关闭</p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Stats row */}
      <div className="px-4 mt-3 grid grid-cols-3 gap-2">
        {[
          { icon: '🍽️', value: restaurants.length, label: '总餐厅' },
          { icon: '✅', value: actives.length, label: '可选择' },
          { icon: '🚫', value: restaurants.length - actives.length, label: '已拉黑' },
        ].map(({ icon, value, label }) => (
          <div
            key={label}
            className="flex flex-col items-center py-3 rounded-xl"
            style={{ background: '#fff', boxShadow: '0 2px 8px rgba(0,0,0,0.04)' }}
          >
            <span style={{ fontSize: '18px' }}>{icon}</span>
            <span style={{ fontSize: '18px', fontWeight: 700, color: '#1a1a1a', lineHeight: 1.2 }}>{value}</span>
            <span style={{ fontSize: '10px', color: '#999' }}>{label}</span>
          </div>
        ))}
      </div>

      {/* Hot picks */}
      <div className="px-4 mt-4 mb-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-1.5">
            <TrendingUp size={14} color="#FF4757" />
            <span style={{ fontSize: '14px', fontWeight: 700, color: '#1a1a1a' }}>热门推荐</span>
          </div>
          <button onClick={() => navigate('/restaurants')} style={{ fontSize: '12px', color: '#FF4757' }}>
            查看全部 →
          </button>
        </div>

        <div className="flex flex-col gap-2">
          {topRated.map((r, idx) => (
            <motion.div
              key={r.id}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: idx * 0.08 }}
              className="flex items-center gap-3 p-3 rounded-xl"
              style={{ background: '#fff', boxShadow: '0 2px 8px rgba(0,0,0,0.04)' }}
            >
              <div style={{ position: 'relative', flexShrink: 0 }}>
                <img
                  src={r.image}
                  alt={r.name}
                  style={{ width: 56, height: 56, borderRadius: '12px', objectFit: 'cover' }}
                />
                <div
                  style={{
                    position: 'absolute', top: -4, left: -4,
                    width: 18, height: 18, borderRadius: '50%',
                    background: idx === 0 ? '#FF4757' : idx === 1 ? '#FFA502' : '#a0a0a0',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '9px', fontWeight: 700, color: '#fff',
                  }}
                >
                  {idx + 1}
                </div>
              </div>
              <div className="flex-1 min-w-0">
                <p style={{ fontSize: '13px', fontWeight: 600, color: '#1a1a1a' }}>{r.name}</p>
                <div className="flex items-center gap-2 mt-0.5">
                  <div className="flex items-center gap-0.5">
                    <Star size={10} fill="#FFA502" color="#FFA502" />
                    <span style={{ fontSize: '11px', color: '#FFA502', fontWeight: 600 }}>{r.rating}</span>
                  </div>
                  <div className="flex items-center gap-0.5">
                    <MapPin size={9} color="#999" />
                    <span style={{ fontSize: '10px', color: '#999' }}>{r.distance}</span>
                  </div>
                  <span style={{ fontSize: '10px', color: '#999' }}>
                    {'¥'.repeat(r.priceLevel)}
                  </span>
                </div>
                <div className="flex flex-wrap gap-1 mt-1">
                  {r.tags.slice(0, 3).map(t => (
                    <span
                      key={t}
                      style={{
                        fontSize: '9px', color: '#FF6B35',
                        background: '#FFF0ED', borderRadius: '4px', padding: '1px 5px',
                      }}
                    >
                      {t}
                    </span>
                  ))}
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>

      {/* Add restaurant button */}
      <div className="px-4 mb-6">
        <motion.button
          whileTap={{ scale: 0.97 }}
          onClick={() => navigate('/mine')}
          className="w-full flex items-center justify-center gap-2 py-3 rounded-2xl"
          style={{
            background: 'transparent',
            border: '2px dashed #FF4757',
            color: '#FF4757',
          }}
        >
          <Plus size={16} />
          <span style={{ fontSize: '13px', fontWeight: 600 }}>添加新餐厅</span>
        </motion.button>
      </div>
    </div>
  );
}
