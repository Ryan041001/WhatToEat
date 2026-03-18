import { useState } from 'react';
import { useNavigate } from 'react-router';
import { motion, AnimatePresence } from 'motion/react';
import { Star, MapPin, TrendingUp, Plus } from 'lucide-react';
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
      animate={
        shaking
          ? {
              rotate: [0, -15, 15, -12, 12, -8, 8, -4, 4, 0],
              scale: [1, 1.1, 1.1, 1.1, 1.1, 1.05, 1.05, 1.02, 1.02, 1],
            }
          : {}
      }
      transition={{ duration: 0.8 }}
      onClick={handleShake}
      className="glass-panel glass-blur-sm click-hover-lift flex flex-1 cursor-pointer flex-col items-center gap-2 rounded-2xl p-4 active:scale-95"
      style={{ background: 'linear-gradient(135deg, color-mix(in srgb, var(--primary) 88%, var(--accent)), var(--primary))' }}
    >
      <span className="text-[28px]">📱</span>
      <span className="text-[12px] font-semibold" style={{ color: 'var(--primary-foreground)' }}>
        摇一摇
      </span>
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
    <div className="flex min-h-full flex-col pb-5">
      <div
        className="relative overflow-hidden px-5 pb-6 pt-4"
        style={{ background: 'linear-gradient(135deg, var(--primary) 0%, color-mix(in srgb, var(--primary) 62%, var(--accent)) 100%)' }}
      >
        <div className="absolute -right-5 -top-5 h-24 w-24 rounded-full" style={{ background: 'color-mix(in srgb, var(--glass-surface-strong) 34%, transparent)' }} />
        <div className="absolute -bottom-7 left-16 h-20 w-20 rounded-full" style={{ background: 'color-mix(in srgb, var(--glass-surface-light) 40%, transparent)' }} />

        <div className="relative">
          <div className="mb-3 flex items-center justify-between">
            <div>
              <p className="text-[12px]" style={{ color: 'color-mix(in srgb, var(--glass-text-inverse) 82%, transparent)' }}>今天吃什么？</p>
              <h1 className="text-[22px] font-bold leading-tight" style={{ color: 'var(--glass-text-inverse)' }}>干饭防纠结 🍚</h1>
            </div>
            <div className="glass-chip glass-blur-sm flex items-center gap-1 px-3 py-1.5" style={{ background: 'var(--glass-surface-strong)', border: '1px solid var(--glass-border-medium)' }}>
              <span className="text-[11px] font-medium" style={{ color: 'var(--glass-text-inverse)' }}>{actives.length} 家可选</span>
            </div>
          </div>

          <motion.button
            whileTap={{ scale: 0.97 }}
            onClick={() => navigate('/spin')}
            className="glass-overlay glass-blur-sm click-hover-lift w-full rounded-2xl px-4 py-3"
            style={{ background: 'var(--glass-surface-strong)', border: '1px solid var(--glass-border-medium)' }}
          >
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <span className="text-[24px]">🎰</span>
                <div className="text-left">
                  <p className="text-[14px] font-semibold" style={{ color: 'var(--glass-text-inverse)' }}>随机替我选一个！</p>
                  <p className="text-[11px]" style={{ color: 'var(--glass-text-muted-transparent)' }}>告别选择困难症</p>
                </div>
              </div>
              <div className="glass-chip glass-blur-sm px-3 py-1" style={{ background: 'var(--glass-surface-strong)', border: '1px solid var(--glass-border-medium)' }}>
                <span className="text-[12px] font-bold" style={{ color: 'var(--primary)' }}>
                  开始
                </span>
              </div>
            </div>
          </motion.button>
        </div>
      </div>

      <div className="-mt-2 px-4">
        <div className="glass-panel glass-blur-sm grid grid-cols-3 gap-3 p-4" style={{ background: 'var(--glass-surface-strong)', border: '1px solid var(--glass-border-medium)' }}>
          <motion.button
            whileTap={{ scale: 0.93 }}
            onClick={() => navigate('/spin')}
            className="glass-panel glass-blur-sm click-hover-lift flex cursor-pointer flex-col items-center gap-2 rounded-2xl p-4"
            style={{ background: 'linear-gradient(135deg, var(--primary), color-mix(in srgb, var(--primary) 54%, var(--accent)))' }}
          >
            <span className="text-[28px]">🎡</span>
            <span className="text-[12px] font-semibold" style={{ color: 'var(--glass-text-inverse)' }}>大转盘</span>
          </motion.button>
          <ShakeButton onResult={setShakeResult} />
          <motion.button
            whileTap={{ scale: 0.93 }}
            onClick={() => navigate('/swipe')}
            className="glass-panel click-hover-lift flex cursor-pointer flex-col items-center gap-2 rounded-2xl p-4"
            style={{ background: 'linear-gradient(135deg, color-mix(in srgb, var(--accent) 72%, var(--primary)), color-mix(in srgb, var(--color-success) 76%, var(--accent)))' }}
          >
            <span className="text-[28px]">💘</span>
            <span className="text-[12px] font-semibold" style={{ color: 'var(--glass-text-inverse)' }}>卡片滑</span>
          </motion.button>
        </div>
      </div>

      <AnimatePresence>
        {shakeResult && (
          <motion.div
            initial={{ opacity: 0, scale: 0.8, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.8, y: 20 }}
            className="glass-overlay fixed inset-0 z-50 flex items-center justify-center"
            onClick={() => setShakeResult(null)}
          >
            <div className="glass-overlay glass-blur-sm max-w-[280px] rounded-3xl px-8 py-6 text-center" style={{ background: 'var(--glass-surface-strong)', border: '1px solid var(--glass-border-medium)' }}>
              <div className="mb-2 text-[48px]">🎉</div>
              <p className="text-[14px]" style={{ color: 'color-mix(in srgb, var(--glass-text-inverse) 85%, transparent)' }}>摇到了！就吃</p>
              <p className="my-1 text-[22px] font-bold" style={{ color: 'var(--glass-text-inverse)' }}>{shakeResult}</p>
              <p className="text-[12px]" style={{ color: 'color-mix(in srgb, var(--glass-text-inverse) 70%, transparent)' }}>点击关闭</p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="mt-3 grid grid-cols-3 gap-2 px-4">
        {[
          { icon: '🍽️', value: restaurants.length, label: '总餐厅' },
          { icon: '✅', value: actives.length, label: '可选择' },
          { icon: '🚫', value: restaurants.length - actives.length, label: '已拉黑' },
        ].map(({ icon, value, label }) => (
          <div key={label} className="glass-panel glass-blur-sm flex flex-col items-center rounded-xl py-3" style={{ background: 'var(--glass-surface-strong)', border: '1px solid var(--glass-border-medium)' }}>
            <span className="text-[18px]">{icon}</span>
            <span className="text-[18px] font-bold leading-tight text-foreground">{value}</span>
            <span className="text-[10px] text-muted-foreground">{label}</span>
          </div>
        ))}
      </div>

      <div className="mb-4 mt-4 px-4">
        <div className="mb-3 flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <TrendingUp size={14} color="var(--primary)" />
            <span className="text-[14px] font-bold text-foreground">热门推荐</span>
          </div>
          <button onClick={() => navigate('/restaurants')} className="text-[12px] font-medium" style={{ color: 'var(--primary)' }}>
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
              className="glass-panel glass-blur-sm flex items-center gap-3 rounded-xl p-3"
              style={{ background: 'var(--glass-surface-strong)', border: '1px solid var(--glass-border-medium)' }}
            >
              <div className="relative shrink-0">
                <img src={r.image} alt={r.name} className="h-14 w-14 rounded-xl object-cover" />
                <div
                  className="absolute -left-1 -top-1 flex h-[18px] w-[18px] items-center justify-center rounded-full text-[9px] font-bold"
                  style={{ color: 'var(--primary-foreground)', background: idx === 0 ? 'var(--primary)' : idx === 1 ? 'var(--warning)' : 'var(--muted-foreground)' }}
                >
                  {idx + 1}
                </div>
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-[13px] font-semibold text-foreground">{r.name}</p>
                <div className="mt-0.5 flex items-center gap-2">
                  <div className="flex items-center gap-0.5">
                    <Star size={10} fill="var(--warning)" color="var(--warning)" />
                    <span className="text-[11px] font-semibold" style={{ color: 'var(--warning)' }}>{r.rating}</span>
                  </div>
                  <div className="flex items-center gap-0.5">
                    <MapPin size={9} color="var(--muted-foreground)" />
                    <span className="text-[10px] text-muted-foreground">{r.distance}</span>
                  </div>
                  <span className="text-[10px] text-muted-foreground">{'¥'.repeat(r.priceLevel)}</span>
                </div>
                <div className="mt-1 flex flex-wrap gap-1">
                  {r.tags.slice(0, 3).map((t) => (
                    <span key={t} className="glass-chip glass-blur-sm px-1.5 py-0.5 text-[9px]" style={{ background: 'var(--glass-surface-strong)', border: '1px solid var(--glass-border-medium)', color: 'var(--primary)' }}>
                      {t}
                    </span>
                  ))}
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>

      <div className="mb-6 px-4">
        <motion.button
          whileTap={{ scale: 0.97 }}
          onClick={() => navigate('/mine')}
          className="glass-panel glass-blur-sm click-hover-lift w-full rounded-2xl border-2 border-dashed py-3"
          style={{ borderColor: 'var(--primary)', color: 'var(--primary)', background: 'var(--glass-surface-strong)' }}
        >
          <div className="flex items-center justify-center gap-2">
            <Plus size={16} />
            <span className="text-[13px] font-semibold">添加新餐厅</span>
          </div>
        </motion.button>
      </div>
    </div>
  );
}
