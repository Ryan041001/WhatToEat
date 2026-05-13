import { useEffect, useRef, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { ChevronLeft } from 'lucide-react';
import { useNavigate } from 'react-router';
import { useRestaurants } from '../context/RestaurantContext';

const SEGMENT_COLORS = [
  '#FF6B35', '#FF8C42', '#FFA502', '#FFBE0B',
  '#F77F00', '#FCBF49', '#E09F3E', '#F4A261',
  '#FF9F1C', '#E76F51', '#D62828', '#F28482',
];

function renderWheel(
  canvas: HTMLCanvasElement,
  names: string[],
  currentAngle: number,
  highlightIdx?: number
) {
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  const W = canvas.width;
  const H = canvas.height;
  const cx = W / 2;
  const cy = H / 2;
  const radius = cx - 12;
  const n = names.length;
  if (n === 0) return;

  // Clear
  ctx.clearRect(0, 0, W, H);

  // Outer ring glow
  ctx.save();
  ctx.beginPath();
  ctx.arc(cx, cy, radius + 6, 0, 2 * Math.PI);
  ctx.fillStyle = 'color-mix(in srgb, var(--glass-surface-strong) 90%, white)';
  ctx.shadowBlur = 16;
  ctx.shadowColor = 'color-mix(in srgb, var(--foreground) 18%, transparent)';
  ctx.fill();
  ctx.restore();

  // Rotate for spinning
  ctx.save();
  ctx.translate(cx, cy);
  ctx.rotate(currentAngle);

  const arcSize = (2 * Math.PI) / n;
  for (let i = 0; i < n; i++) {
    const startAngle = i * arcSize - Math.PI / 2;
    const endAngle = startAngle + arcSize;
    const color = SEGMENT_COLORS[i % SEGMENT_COLORS.length];

    // Segment
    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.arc(0, 0, radius, startAngle, endAngle);
    ctx.closePath();
    ctx.fillStyle = highlightIdx === i ? 'color-mix(in srgb, var(--glass-surface-strong) 88%, var(--warning) 12%)' : color;
    ctx.fill();
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.85)';
    ctx.lineWidth = 2.5;
    ctx.stroke();

    // Text
    ctx.save();
    ctx.rotate(startAngle + arcSize / 2);
    ctx.textAlign = 'right';
    ctx.textBaseline = 'middle';
    const fontSize = n <= 6 ? 13 : n <= 9 ? 11 : 9;
    ctx.font = `bold ${fontSize}px -apple-system, PingFang SC, sans-serif`;
    ctx.fillStyle = highlightIdx === i ? color : 'rgba(255, 255, 255, 0.95)';
    ctx.strokeStyle = 'rgba(0, 0, 0, 0.4)';
    ctx.lineWidth = n <= 8 ? 3 : 2.4;
    const maxLen = n <= 6 ? 6 : n <= 9 ? 5 : 4;
    const label = names[i].length > maxLen ? names[i].slice(0, maxLen) + '…' : names[i];
    const labelOffset = n <= 6 ? radius - 12 : n <= 9 ? radius - 16 : radius - 20;
    ctx.strokeText(label, labelOffset, 1);
    ctx.fillText(label, labelOffset, 1);
    ctx.restore();
  }

  ctx.restore();

  // Center circle
  ctx.save();
  ctx.beginPath();
  ctx.arc(cx, cy, 28, 0, 2 * Math.PI);
  ctx.fillStyle = 'var(--glass-surface-strong)';
  ctx.shadowBlur = 6;
  ctx.shadowColor = 'color-mix(in srgb, var(--foreground) 12%, transparent)';
  ctx.fill();
  ctx.restore();

  // Center emoji
  ctx.font = '20px sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText('🍚', cx, cy);
}

export function SpinWheel() {
  const navigate = useNavigate();
  const { getActiveRestaurants } = useRestaurants();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [spinning, setSpinning] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const animRef = useRef<number | null>(null);
  const currentAngleRef = useRef(0);

  const restaurants = getActiveRestaurants();
  const names = restaurants.map(r => r.name);

  const draw = useCallback((angle: number, highlight?: number) => {
    if (canvasRef.current) {
      renderWheel(canvasRef.current, names, angle, highlight);
    }
  }, [names]);

  useEffect(() => {
    draw(currentAngleRef.current);
  }, [draw]);

  const spin = () => {
    if (spinning || restaurants.length === 0) return;
    setResult(null);
    setSpinning(true);

    const n = restaurants.length;
    const pickedIdx = Math.floor(Math.random() * n);
    const arcSize = (2 * Math.PI) / n;

    // Calculate the target angle so that pickedIdx segment faces the top indicator
    // The top of the canvas is at -π/2 in canvas coords.
    // Segment i spans from (i*arcSize - π/2) to ((i+1)*arcSize - π/2) after rotation.
    // We want the center of segment pickedIdx to be at angle -π/2 (pointing up).
    // Center of segment i (without rotation) = i*arcSize + arcSize/2 - π/2
    // After adding rotation θ: i*arcSize + arcSize/2 - π/2 + θ = -π/2
    // => θ = -(i*arcSize + arcSize/2)
    const segCenter = pickedIdx * arcSize + arcSize / 2;
    const targetBase = -segCenter;

    // Add full rotations to make it spin nicely
    const fullSpins = (5 + Math.floor(Math.random() * 3)) * 2 * Math.PI;
    // Normalize target relative to current angle
    const currentAngle = currentAngleRef.current;
    const normalizedCurrent = ((currentAngle % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
    const normalizedTarget = ((targetBase % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
    const diff = ((normalizedTarget - normalizedCurrent) + 2 * Math.PI) % (2 * Math.PI);
    const totalSpin = fullSpins + diff;
    const endAngle = currentAngle + totalSpin;

    const duration = 4500;
    const startTime = performance.now();
    const startAngle = currentAngle;

    const animate = (now: number) => {
      const elapsed = Math.min(now - startTime, duration);
      const t = elapsed / duration;
      // Ease out quint
      const eased = 1 - Math.pow(1 - t, 5);
      const angle = startAngle + (endAngle - startAngle) * eased;
      currentAngleRef.current = angle;
      draw(angle);

      if (elapsed < duration) {
        animRef.current = requestAnimationFrame(animate);
      } else {
        currentAngleRef.current = endAngle;
        setSpinning(false);
        setResult(restaurants[pickedIdx].name);
        draw(endAngle, pickedIdx);
      }
    };

    animRef.current = requestAnimationFrame(animate);
  };

  useEffect(() => {
    return () => {
      if (animRef.current) cancelAnimationFrame(animRef.current);
    };
  }, []);

  return (
    <div className="flex flex-col" style={{ minHeight: '100%', background: 'var(--color-background)' }}>
      {/* Header */}
      <div
        className="unified-topbar flex items-center px-4 pt-3 pb-4"
        style={{ background: 'linear-gradient(135deg, var(--primary), color-mix(in srgb, var(--primary) 60%, var(--color-warning)))' }}
      >
        <button onClick={() => navigate('/')} className="mr-3">
          <ChevronLeft size={22} color="var(--primary-foreground)" />
        </button>
        <h2 style={{ color: 'var(--primary-foreground)', fontSize: '16px', fontWeight: 700 }}>大转盘</h2>
        <span style={{ marginLeft: 8, fontSize: '12px', color: 'color-mix(in srgb, var(--primary-foreground) 78%, transparent)' }}>
          随机替你决定！
        </span>
      </div>

      <div className="flex flex-col items-center px-4 pt-6">
        {restaurants.length === 0 ? (
          <div className="flex flex-col items-center gap-3 py-16">
            <span style={{ fontSize: '48px' }}>🍽️</span>
            <p style={{ color: 'var(--muted-foreground)', fontSize: '14px' }}>还没有可选餐厅，先去添加吧~</p>
          </div>
        ) : (
          <>
            {/* Pointer indicator */}
            <div
              style={{
                width: 0, height: 0,
                borderLeft: '12px solid transparent',
                borderRight: '12px solid transparent',
                borderTop: '24px solid var(--primary)',
                zIndex: 10,
                filter: 'drop-shadow(0 3px 6px color-mix(in srgb, var(--primary) 45%, transparent))',
                marginBottom: 2,
              }}
            />

            {/* Wheel canvas */}
            <div
              className="glass-panel glass-blur-sm enhanced-neumorph-card neumorph-strong click-hover-lift"
              style={{
                borderRadius: '50%',
                boxShadow: 'var(--neumorph-shadow-strong)',
                overflow: 'hidden',
                background: 'var(--glass-surface-strong)',
                border: '1px solid var(--glass-border-medium)',
              }}
            >
              <canvas
                ref={canvasRef}
                width={290}
                height={290}
                style={{ display: 'block' }}
              />
            </div>

            {/* Spin button */}
            <motion.button
              whileTap={!spinning ? { scale: 0.93 } : {}}
              onClick={spin}
              disabled={spinning}
              className="glass-chip glass-blur-sm click-hover-lift mt-7 px-12 py-4 rounded-full"
              style={{
                background: spinning
                  ? 'linear-gradient(135deg, color-mix(in srgb, var(--muted-foreground) 35%, white), var(--muted-foreground))'
                  : 'linear-gradient(135deg, var(--primary), color-mix(in srgb, var(--primary) 60%, var(--color-warning)))',
                color: 'var(--primary-foreground)',
                fontSize: '16px',
                fontWeight: 700,
                boxShadow: spinning ? 'none' : 'var(--glass-shadow-glow)',
                border: spinning
                  ? '1px solid var(--glass-border-medium)'
                  : '1px solid var(--glass-border-medium)',
                cursor: spinning ? 'not-allowed' : 'pointer',
                transition: 'background 0.3s',
              }}
            >
              {spinning ? '🎡 转动中…' : '🎡 开始转！'}
            </motion.button>
          </>
        )}
      </div>

      {/* Result banner */}
      <AnimatePresence>
        {result && !spinning && (
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 40 }}
            className="mx-4 mt-6 p-5 rounded-3xl glass-overlay glass-blur-md"
            style={{
              background: 'linear-gradient(135deg, var(--primary), color-mix(in srgb, var(--primary) 60%, var(--color-warning)))',
              boxShadow: 'var(--glass-shadow-glow)',
            }}
          >
            <div className="flex items-center gap-4">
              <span style={{ fontSize: '40px' }}>🎉</span>
              <div>
                <p style={{ color: 'color-mix(in srgb, var(--primary-foreground) 85%, transparent)', fontSize: '12px' }}>转盘已决定！今天就吃</p>
                <p style={{ color: 'var(--primary-foreground)', fontSize: '22px', fontWeight: 700 }}>{result}</p>
              </div>
            </div>
            <div className="flex gap-2 mt-4">
              <button
                onClick={() => { setResult(null); setTimeout(spin, 200); }}
                className="glass-chip glass-blur-sm click-hover-lift flex-1 py-2.5 rounded-2xl"
                style={{
                  background: 'var(--glass-surface-strong)',
                  color: 'var(--primary-foreground)',
                  fontSize: '13px',
                  fontWeight: 600,
                  border: '1px solid var(--glass-border-medium)',
                }}
              >
                再转一次
              </button>
              <button
                onClick={() => setResult(null)}
                className="glass-chip glass-blur-sm click-hover-lift flex-1 py-2.5 rounded-2xl"
                style={{
                  background: 'var(--primary-foreground)',
                  color: 'var(--primary)',
                  fontSize: '13px',
                  fontWeight: 700,
                  border: '1px solid var(--glass-border-medium)',
                }}
              >
                就这家！去干饭 🍽️
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Hint */}
      <div className="px-4 mt-4 mb-6">
        <p style={{ fontSize: '12px', color: 'var(--glass-text-muted-transparent)', textAlign: 'center' }}>
          共 {restaurants.length} 家餐厅参与 · 去「食堂」管理列表
        </p>
      </div>
    </div>
  );
}
