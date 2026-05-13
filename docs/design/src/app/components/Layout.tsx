import { Outlet, useNavigate, useLocation } from 'react-router';
import { Home, RefreshCw, Layers, Store, User } from 'lucide-react';

const NAV_ITEMS = [
  { path: '/', label: '首页', icon: Home },
  { path: '/spin', label: '大转盘', icon: RefreshCw },
  { path: '/swipe', label: '卡片滑', icon: Layers },
  { path: '/restaurants', label: '食堂', icon: Store },
  { path: '/mine', label: '我的', icon: User },
];

export function Layout() {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <div className="glass-atmosphere flex min-h-screen items-center justify-center px-4 py-6">
      <div
        className="glass-overlay glass-blur-md relative flex flex-col overflow-hidden rounded-[44px] border"
        style={{
          width: '390px',
          height: '844px',
          borderColor: 'var(--glass-border-strong)',
          boxShadow: 'var(--glass-shadow-glow)',
          background: 'var(--glass-surface-strong)',
        }}
      >
        <div
          className="glass-nav unified-topbar glass-blur-sm absolute left-1/2 top-1 z-30 h-[30px] w-[120px] -translate-x-1/2 rounded-2xl"
          style={{ background: 'var(--glass-surface-strong)', borderColor: 'transparent' }}
        />

        <div
          className="glass-nav unified-topbar glass-blur-sm z-20 flex h-[46px] flex-shrink-0 items-center justify-between border-b px-6 pt-2"
          style={{
            background: 'var(--topbar-bg)',
            borderBottomColor: 'var(--topbar-border-color)',
          }}
        >
          <span className="text-[12px] font-semibold" style={{ color: 'var(--glass-text-strong)' }}>9:41</span>
          <div className="flex items-center gap-1" style={{ color: 'var(--glass-text-default)' }}>
            <svg width="16" height="12" viewBox="0 0 16 12" fill="none" aria-hidden>
              <rect x="0" y="3" width="3" height="9" rx="1" fill="currentColor" opacity="0.4"/>
              <rect x="4.5" y="2" width="3" height="10" rx="1" fill="currentColor" opacity="0.6"/>
              <rect x="9" y="0" width="3" height="12" rx="1" fill="currentColor" opacity="0.8"/>
              <rect x="13.5" y="0" width="3" height="12" rx="1" fill="currentColor"/>
            </svg>
            <svg width="16" height="12" viewBox="0 0 20 14" fill="none" aria-hidden>
              <path d="M10 2.8C12.8 2.8 15.3 3.9 17.1 5.7L18.5 4.3C16.3 2.2 13.3 1 10 1C6.7 1 3.7 2.2 1.5 4.3L2.9 5.7C4.7 3.9 7.2 2.8 10 2.8Z" fill="currentColor" opacity="0.4"/>
              <path d="M10 5.8C11.9 5.8 13.7 6.6 14.9 7.9L16.3 6.5C14.7 4.8 12.5 3.8 10 3.8C7.5 3.8 5.3 4.8 3.7 6.5L5.1 7.9C6.3 6.6 8.1 5.8 10 5.8Z" fill="currentColor" opacity="0.7"/>
              <circle cx="10" cy="11" r="2" fill="currentColor"/>
            </svg>
            <svg width="25" height="12" viewBox="0 0 25 12" fill="none" aria-hidden>
              <rect x="0" y="1" width="21" height="10" rx="3" stroke="currentColor" strokeWidth="1" strokeOpacity="0.35"/>
              <rect x="1.5" y="2.5" width="16" height="7" rx="2" fill="currentColor"/>
              <path d="M22 4.5V7.5C23.1 7 23.1 5 22 4.5Z" fill="currentColor" opacity="0.4"/>
            </svg>
          </div>
        </div>

        <div className="flex-1 overflow-x-hidden overflow-y-auto" style={{ background: 'var(--background)' }}>
          <Outlet />
        </div>

        <div
          className="glass-nav unified-topbar glass-blur-md z-20 flex h-[82px] flex-shrink-0 items-center justify-around border-t px-1 pb-[18px]"
          style={{
            background: 'var(--topbar-bg)',
            borderTopColor: 'var(--topbar-border-color)',
          }}
        >
          {NAV_ITEMS.map(({ path, label, icon: Icon }) => {
            const isActive = location.pathname === path;
            return (
              <button
                key={path}
                onClick={() => navigate(path)}
                className="flex min-h-11 flex-1 flex-col items-center justify-center gap-0.5 rounded-xl"
                style={{
                  color: isActive ? 'var(--primary)' : 'var(--glass-text-muted)',
                  background: isActive ? 'var(--glass-interaction-hover)' : 'transparent',
                  border: isActive ? '1px solid var(--glass-border-medium)' : '1px solid transparent',
                }}
                aria-current={isActive ? 'page' : undefined}
              >
                <Icon size={22} strokeWidth={isActive ? 2.2 : 1.9} />
                <span className={`text-[10px] ${isActive ? 'font-semibold' : 'font-medium'}`}>{label}</span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

