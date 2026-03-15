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
    <div className="flex justify-center items-center min-h-screen bg-gray-200" style={{ background: 'linear-gradient(135deg, #667eea20, #f64f5920)' }}>
      {/* Phone frame */}
      <div
        className="relative flex flex-col overflow-hidden shadow-2xl"
        style={{
          width: '390px',
          height: '844px',
          background: '#F7F8FA',
          borderRadius: '44px',
          border: '8px solid #1a1a1a',
          boxShadow: '0 30px 80px rgba(0,0,0,0.35), inset 0 0 0 2px #333',
        }}
      >
        {/* Status bar */}
        <div
          className="flex-shrink-0 flex items-center justify-between px-6"
          style={{ height: '44px', background: '#fff', paddingTop: '8px' }}
        >
          <span style={{ fontSize: '12px', fontWeight: 600, color: '#000' }}>9:41</span>
          <div style={{ width: '120px', height: '30px', background: '#1a1a1a', borderRadius: '16px', position: 'absolute', left: '50%', transform: 'translateX(-50%)', top: '2px' }} />
          <div className="flex items-center gap-1">
            <svg width="16" height="12" viewBox="0 0 16 12" fill="none">
              <rect x="0" y="3" width="3" height="9" rx="1" fill="#000" opacity="0.4"/>
              <rect x="4.5" y="2" width="3" height="10" rx="1" fill="#000" opacity="0.6"/>
              <rect x="9" y="0" width="3" height="12" rx="1" fill="#000" opacity="0.8"/>
              <rect x="13.5" y="0" width="3" height="12" rx="1" fill="#000"/>
            </svg>
            <svg width="16" height="12" viewBox="0 0 20 14" fill="none">
              <path d="M10 2.8C12.8 2.8 15.3 3.9 17.1 5.7L18.5 4.3C16.3 2.2 13.3 1 10 1C6.7 1 3.7 2.2 1.5 4.3L2.9 5.7C4.7 3.9 7.2 2.8 10 2.8Z" fill="#000" opacity="0.4"/>
              <path d="M10 5.8C11.9 5.8 13.7 6.6 14.9 7.9L16.3 6.5C14.7 4.8 12.5 3.8 10 3.8C7.5 3.8 5.3 4.8 3.7 6.5L5.1 7.9C6.3 6.6 8.1 5.8 10 5.8Z" fill="#000" opacity="0.7"/>
              <circle cx="10" cy="11" r="2" fill="#000"/>
            </svg>
            <svg width="25" height="12" viewBox="0 0 25 12" fill="none">
              <rect x="0" y="1" width="21" height="10" rx="3" stroke="#000" strokeWidth="1" strokeOpacity="0.35"/>
              <rect x="1.5" y="2.5" width="16" height="7" rx="2" fill="#000"/>
              <path d="M22 4.5V7.5C23.1 7 23.1 5 22 4.5Z" fill="#000" opacity="0.4"/>
            </svg>
          </div>
        </div>

        {/* Scrollable content */}
        <div className="flex-1 overflow-y-auto overflow-x-hidden" style={{ background: '#F7F8FA' }}>
          <Outlet />
        </div>

        {/* Bottom tab bar */}
        <div
          className="flex-shrink-0 flex items-center justify-around border-t"
          style={{
            height: '78px',
            background: '#fff',
            borderColor: '#e5e5e5',
            paddingBottom: '18px',
          }}
        >
          {NAV_ITEMS.map(({ path, label, icon: Icon }) => {
            const isActive = location.pathname === path;
            return (
              <button
                key={path}
                onClick={() => navigate(path)}
                className="flex flex-col items-center gap-0.5"
                style={{ flex: 1 }}
              >
                <Icon
                  size={22}
                  color={isActive ? '#FF4757' : '#999'}
                  strokeWidth={isActive ? 2.2 : 1.8}
                />
                <span
                  style={{
                    fontSize: '10px',
                    color: isActive ? '#FF4757' : '#999',
                    fontWeight: isActive ? 600 : 400,
                  }}
                >
                  {label}
                </span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
