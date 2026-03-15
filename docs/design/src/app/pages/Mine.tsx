import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Plus, X, Star, CheckCircle2 } from 'lucide-react';
import { useNavigate } from 'react-router';
import { useRestaurants } from '../context/RestaurantContext';

const CATEGORY_OPTIONS = ['川菜', '日料', '快餐', '烧烤', '米线', '面食', '韩餐', '西餐', '北方面食', '粤菜', '湘菜', '火锅', '小吃', '甜品', '其他'];
const PRESET_TAGS = ['辣', '不辣', '实惠', '量大', '环境好', '快手', '排队多', '打卡', '下饭', '养胃', '清淡', '聚餐', '约会', '宵夜', '宿舍楼下'];
const PRICE_OPTIONS: { label: string; value: 1 | 2 | 3 }[] = [
  { label: '¥ 人均15以下', value: 1 },
  { label: '¥¥ 人均15-30', value: 2 },
  { label: '¥¥¥ 人均30+', value: 3 },
];

// Default food images for UGC restaurants (user picks one)
const DEFAULT_IMAGES = [
  'https://images.unsplash.com/photo-1658853577859-7a75373c2675?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400',
  'https://images.unsplash.com/photo-1627900440398-5db32dba8db1?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400',
  'https://images.unsplash.com/photo-1723691802798-fa6efc67b2c9?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400',
  'https://images.unsplash.com/photo-1694834589398-27b369c6f7a6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400',
  'https://images.unsplash.com/photo-1717809184558-597a0f1b9eb0?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400',
  'https://images.unsplash.com/photo-1760533536738-f0965fd52354?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400',
];

function AddRestaurantForm({ onClose, onSuccess }: { onClose: () => void; onSuccess: () => void }) {
  const { addRestaurant } = useRestaurants();
  const [name, setName] = useState('');
  const [category, setCategory] = useState('');
  const [priceLevel, setPriceLevel] = useState<1 | 2 | 3>(1);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [customTag, setCustomTag] = useState('');
  const [description, setDescription] = useState('');
  const [address, setAddress] = useState('');
  const [distance, setDistance] = useState('');
  const [rating, setRating] = useState(4.0);
  const [selectedImage, setSelectedImage] = useState(DEFAULT_IMAGES[0]);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const toggleTag = (tag: string) => {
    setSelectedTags(prev =>
      prev.includes(tag) ? prev.filter(t => t !== tag) : [...prev, tag]
    );
  };

  const addCustomTag = () => {
    const t = customTag.trim();
    if (t && !selectedTags.includes(t) && selectedTags.length < 8) {
      setSelectedTags(prev => [...prev, t]);
      setCustomTag('');
    }
  };

  const validate = () => {
    const e: Record<string, string> = {};
    if (!name.trim()) e.name = '请填写餐厅名称';
    if (!category) e.category = '请选择分类';
    if (!address.trim()) e.address = '请填写地址';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = () => {
    if (!validate()) return;
    addRestaurant({
      name: name.trim(),
      category,
      priceLevel,
      tags: selectedTags,
      description: description.trim() || '用户添加的餐厅',
      address: address.trim(),
      distance: distance.trim() || '附近',
      rating,
      image: selectedImage,
      isBlacklisted: false,
    });
    onSuccess();
  };

  return (
    <div className="flex flex-col" style={{ minHeight: '100%' }}>
      {/* Header */}
      <div
        className="flex items-center gap-3 px-4 py-4"
        style={{ background: 'linear-gradient(135deg, #FFA502, #FF6B35)' }}
      >
        <button onClick={onClose}>
          <X size={22} color="#fff" />
        </button>
        <h2 style={{ color: '#fff', fontSize: '16px', fontWeight: 700, flex: 1 }}>添加新餐厅</h2>
        <button
          onClick={handleSubmit}
          className="px-4 py-1.5 rounded-full"
          style={{ background: '#fff', color: '#FF6B35', fontSize: '13px', fontWeight: 700 }}
        >
          发布
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-4 py-4">
        {/* Image picker */}
        <div className="mb-4">
          <p style={{ fontSize: '12px', color: '#999', marginBottom: '8px' }}>选择封面图</p>
          <div className="flex gap-2 overflow-x-auto pb-1">
            {DEFAULT_IMAGES.map((img, idx) => (
              <button
                key={idx}
                onClick={() => setSelectedImage(img)}
                style={{
                  flexShrink: 0, position: 'relative',
                  width: 64, height: 64, borderRadius: 12,
                  overflow: 'hidden',
                  border: `3px solid ${selectedImage === img ? '#FF4757' : 'transparent'}`,
                  transition: 'border-color 0.2s',
                }}
              >
                <img src={img} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                {selectedImage === img && (
                  <div
                    style={{
                      position: 'absolute', inset: 0,
                      background: 'rgba(255,71,87,0.3)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}
                  >
                    <CheckCircle2 size={20} color="#fff" />
                  </div>
                )}
              </button>
            ))}
          </div>
        </div>

        {/* Name */}
        <div className="mb-4">
          <label style={{ fontSize: '13px', fontWeight: 600, color: '#333', display: 'block', marginBottom: '6px' }}>
            餐厅名称 <span style={{ color: '#FF4757' }}>*</span>
          </label>
          <input
            value={name}
            onChange={e => { setName(e.target.value); setErrors(p => ({ ...p, name: '' })); }}
            placeholder="e.g. 老妈蹄花火锅"
            style={{
              width: '100%', padding: '10px 12px',
              borderRadius: '12px', border: `1px solid ${errors.name ? '#FF4757' : '#e5e5e5'}`,
              fontSize: '14px', color: '#333', background: '#fff',
              outline: 'none', boxSizing: 'border-box',
            }}
          />
          {errors.name && <p style={{ color: '#FF4757', fontSize: '11px', marginTop: '3px' }}>{errors.name}</p>}
        </div>

        {/* Category */}
        <div className="mb-4">
          <label style={{ fontSize: '13px', fontWeight: 600, color: '#333', display: 'block', marginBottom: '6px' }}>
            口味分类 <span style={{ color: '#FF4757' }}>*</span>
          </label>
          <div className="flex flex-wrap gap-2">
            {CATEGORY_OPTIONS.map(cat => (
              <button
                key={cat}
                onClick={() => { setCategory(cat); setErrors(p => ({ ...p, category: '' })); }}
                style={{
                  padding: '6px 14px', borderRadius: '20px', fontSize: '12px',
                  background: category === cat ? '#FF4757' : '#f5f5f5',
                  color: category === cat ? '#fff' : '#666',
                  fontWeight: category === cat ? 600 : 400,
                  border: `1px solid ${category === cat ? '#FF4757' : '#e5e5e5'}`,
                  transition: 'all 0.2s',
                }}
              >
                {cat}
              </button>
            ))}
          </div>
          {errors.category && <p style={{ color: '#FF4757', fontSize: '11px', marginTop: '3px' }}>{errors.category}</p>}
        </div>

        {/* Price level */}
        <div className="mb-4">
          <label style={{ fontSize: '13px', fontWeight: 600, color: '#333', display: 'block', marginBottom: '6px' }}>人均消费</label>
          <div className="flex gap-2">
            {PRICE_OPTIONS.map(opt => (
              <button
                key={opt.value}
                onClick={() => setPriceLevel(opt.value)}
                className="flex-1 py-2 rounded-xl"
                style={{
                  background: priceLevel === opt.value ? '#FFF0ED' : '#f5f5f5',
                  color: priceLevel === opt.value ? '#FF6B35' : '#888',
                  fontSize: '11px',
                  fontWeight: priceLevel === opt.value ? 700 : 400,
                  border: `1px solid ${priceLevel === opt.value ? '#FF6B35' : '#e5e5e5'}`,
                }}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Rating */}
        <div className="mb-4">
          <label style={{ fontSize: '13px', fontWeight: 600, color: '#333', display: 'block', marginBottom: '6px' }}>
            给个评分：{rating.toFixed(1)} ⭐
          </label>
          <div className="flex items-center gap-2">
            {[1, 2, 3, 4, 5].map(star => (
              <button
                key={star}
                onClick={() => setRating(star)}
                style={{ fontSize: '24px', opacity: rating >= star ? 1 : 0.3 }}
              >
                ⭐
              </button>
            ))}
          </div>
        </div>

        {/* Tags */}
        <div className="mb-4">
          <label style={{ fontSize: '13px', fontWeight: 600, color: '#333', display: 'block', marginBottom: '6px' }}>
            标签（最多8个）
          </label>
          <div className="flex flex-wrap gap-2 mb-2">
            {PRESET_TAGS.map(tag => (
              <button
                key={tag}
                onClick={() => toggleTag(tag)}
                style={{
                  padding: '4px 12px', borderRadius: '16px', fontSize: '12px',
                  background: selectedTags.includes(tag) ? '#FF6B35' : '#f5f5f5',
                  color: selectedTags.includes(tag) ? '#fff' : '#666',
                  border: `1px solid ${selectedTags.includes(tag) ? '#FF6B35' : '#e5e5e5'}`,
                  transition: 'all 0.15s',
                }}
              >
                {selectedTags.includes(tag) ? '✓ ' : '+ '}{tag}
              </button>
            ))}
          </div>
          {/* Custom tag input */}
          <div className="flex gap-2">
            <input
              value={customTag}
              onChange={e => setCustomTag(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && addCustomTag()}
              placeholder="自定义标签..."
              style={{
                flex: 1, padding: '8px 12px', borderRadius: '10px',
                border: '1px solid #e5e5e5', fontSize: '12px', outline: 'none',
              }}
            />
            <button
              onClick={addCustomTag}
              className="px-3 py-2 rounded-xl"
              style={{ background: '#FF6B35', color: '#fff', fontSize: '12px', fontWeight: 600 }}
            >
              添加
            </button>
          </div>
          {/* Selected tags preview */}
          {selectedTags.length > 0 && (
            <div className="flex flex-wrap gap-1.5 mt-2">
              {selectedTags.map(t => (
                <span
                  key={t}
                  className="flex items-center gap-1 px-2 py-0.5 rounded-full"
                  style={{ background: '#FFF0ED', color: '#FF6B35', fontSize: '11px' }}
                >
                  #{t}
                  <button onClick={() => toggleTag(t)}>
                    <X size={10} />
                  </button>
                </span>
              ))}
            </div>
          )}
        </div>

        {/* Address */}
        <div className="mb-4">
          <label style={{ fontSize: '13px', fontWeight: 600, color: '#333', display: 'block', marginBottom: '6px' }}>
            地址 <span style={{ color: '#FF4757' }}>*</span>
          </label>
          <input
            value={address}
            onChange={e => { setAddress(e.target.value); setErrors(p => ({ ...p, address: '' })); }}
            placeholder="e.g. 学生街18号"
            style={{
              width: '100%', padding: '10px 12px', borderRadius: '12px',
              border: `1px solid ${errors.address ? '#FF4757' : '#e5e5e5'}`,
              fontSize: '14px', color: '#333', outline: 'none', boxSizing: 'border-box',
            }}
          />
          {errors.address && <p style={{ color: '#FF4757', fontSize: '11px', marginTop: '3px' }}>{errors.address}</p>}
        </div>

        {/* Distance */}
        <div className="mb-4">
          <label style={{ fontSize: '13px', fontWeight: 600, color: '#333', display: 'block', marginBottom: '6px' }}>距离（选填）</label>
          <input
            value={distance}
            onChange={e => setDistance(e.target.value)}
            placeholder="e.g. 300m / 10分钟路程"
            style={{
              width: '100%', padding: '10px 12px', borderRadius: '12px',
              border: '1px solid #e5e5e5',
              fontSize: '14px', color: '#333', outline: 'none', boxSizing: 'border-box',
            }}
          />
        </div>

        {/* Description */}
        <div className="mb-6">
          <label style={{ fontSize: '13px', fontWeight: 600, color: '#333', display: 'block', marginBottom: '6px' }}>种草一句话（选填）</label>
          <textarea
            value={description}
            onChange={e => setDescription(e.target.value)}
            placeholder="分享你的吃饭体验，给同学们安利一下~"
            rows={3}
            style={{
              width: '100%', padding: '10px 12px', borderRadius: '12px',
              border: '1px solid #e5e5e5', fontSize: '13px', color: '#333',
              outline: 'none', resize: 'none', lineHeight: 1.5, boxSizing: 'border-box',
            }}
          />
        </div>

        {/* Submit */}
        <motion.button
          whileTap={{ scale: 0.97 }}
          onClick={handleSubmit}
          className="w-full py-4 rounded-2xl mb-6"
          style={{
            background: 'linear-gradient(135deg, #FF4757, #FF6B35)',
            color: '#fff', fontSize: '15px', fontWeight: 700,
            boxShadow: '0 6px 20px rgba(255,71,87,0.35)',
          }}
        >
          🍽️ 发布到食堂
        </motion.button>
      </div>
    </div>
  );
}

export function Mine() {
  const navigate = useNavigate();
  const { restaurants, getActiveRestaurants } = useRestaurants();
  const [showAddForm, setShowAddForm] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);

  const userAdded = restaurants.filter(r => r.isUserAdded);
  const actives = getActiveRestaurants();

  const handleSuccess = () => {
    setShowAddForm(false);
    setShowSuccess(true);
    setTimeout(() => setShowSuccess(false), 3000);
  };

  return (
    <div className="flex flex-col" style={{ minHeight: '100%', background: '#F7F8FA' }}>
      <AnimatePresence mode="wait">
        {showAddForm ? (
          <motion.div
            key="form"
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', stiffness: 300, damping: 30 }}
            style={{ position: 'absolute', inset: 0, zIndex: 20, background: '#F7F8FA', overflowY: 'auto' }}
          >
            <AddRestaurantForm onClose={() => setShowAddForm(false)} onSuccess={handleSuccess} />
          </motion.div>
        ) : (
          <motion.div
            key="profile"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="flex flex-col"
            style={{ minHeight: '100%' }}
          >
            {/* Header */}
            <div
              className="relative px-4 pt-4 pb-8 overflow-hidden"
              style={{ background: 'linear-gradient(135deg, #FF4757, #FF6B35)' }}
            >
              <div style={{ position: 'absolute', top: -20, right: -20, width: 120, height: 120, borderRadius: '50%', background: 'rgba(255,255,255,0.08)' }} />
              <div className="flex items-center gap-3">
                <div
                  className="flex items-center justify-center rounded-full"
                  style={{ width: 56, height: 56, background: 'rgba(255,255,255,0.2)' }}
                >
                  <span style={{ fontSize: '28px' }}>🍚</span>
                </div>
                <div>
                  <p style={{ color: '#fff', fontSize: '16px', fontWeight: 700 }}>干饭达人</p>
                  <p style={{ color: 'rgba(255,255,255,0.75)', fontSize: '12px' }}>每天不纠结，干饭好时光</p>
                </div>
              </div>
            </div>

            {/* Stats */}
            <div className="px-4 -mt-4">
              <div
                className="grid grid-cols-3 rounded-2xl overflow-hidden"
                style={{ background: '#fff', boxShadow: '0 4px 20px rgba(0,0,0,0.08)' }}
              >
                {[
                  { label: '收录餐厅', value: restaurants.length, emoji: '🍽️' },
                  { label: '我添加的', value: userAdded.length, emoji: '✍️' },
                  { label: '当前可选', value: actives.length, emoji: '✅' },
                ].map(({ label, value, emoji }) => (
                  <div key={label} className="flex flex-col items-center py-4" style={{ borderRight: '1px solid #f5f5f5' }}>
                    <span style={{ fontSize: '20px' }}>{emoji}</span>
                    <span style={{ fontSize: '20px', fontWeight: 700, color: '#FF4757', lineHeight: 1.2 }}>{value}</span>
                    <span style={{ fontSize: '10px', color: '#999', marginTop: '2px' }}>{label}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Add restaurant CTA */}
            <div className="px-4 mt-4">
              <motion.button
                whileTap={{ scale: 0.97 }}
                onClick={() => setShowAddForm(true)}
                className="w-full flex items-center gap-4 p-4 rounded-2xl"
                style={{
                  background: 'linear-gradient(135deg, #FFA502, #FF6B35)',
                  boxShadow: '0 6px 20px rgba(255,107,53,0.3)',
                }}
              >
                <div
                  className="flex items-center justify-center rounded-2xl"
                  style={{ width: 48, height: 48, background: 'rgba(255,255,255,0.2)' }}
                >
                  <Plus size={24} color="#fff" />
                </div>
                <div className="text-left">
                  <p style={{ color: '#fff', fontSize: '15px', fontWeight: 700 }}>添加新餐厅</p>
                  <p style={{ color: 'rgba(255,255,255,0.8)', fontSize: '12px' }}>分享你发现的美食宝藏</p>
                </div>
                <div style={{ marginLeft: 'auto', color: '#fff', fontSize: '18px' }}>→</div>
              </motion.button>
            </div>

            {/* My added restaurants */}
            {userAdded.length > 0 && (
              <div className="px-4 mt-4">
                <div className="flex items-center justify-between mb-3">
                  <p style={{ fontSize: '14px', fontWeight: 700, color: '#1a1a1a' }}>我添加的餐厅</p>
                  <button onClick={() => navigate('/restaurants')} style={{ fontSize: '12px', color: '#FF4757' }}>管理 →</button>
                </div>
                <div className="flex flex-col gap-2">
                  {userAdded.map(r => (
                    <div
                      key={r.id}
                      className="flex items-center gap-3 p-3 rounded-xl"
                      style={{ background: '#fff', boxShadow: '0 2px 8px rgba(0,0,0,0.04)' }}
                    >
                      <img src={r.image} alt={r.name} style={{ width: 44, height: 44, borderRadius: 10, objectFit: 'cover' }} />
                      <div className="flex-1 min-w-0">
                        <p style={{ fontSize: '13px', fontWeight: 600, color: '#1a1a1a' }}>{r.name}</p>
                        <p style={{ fontSize: '11px', color: '#999' }}>{r.category} · {r.distance}</p>
                      </div>
                      <div className="flex items-center gap-1">
                        <Star size={11} fill="#FFA502" color="#FFA502" />
                        <span style={{ fontSize: '11px', color: '#FFA502', fontWeight: 600 }}>{r.rating}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Tips section */}
            <div className="px-4 mt-4 mb-6">
              <div
                className="p-4 rounded-2xl"
                style={{ background: '#FFF8F0', border: '1px solid #FFE4CC' }}
              >
                <p style={{ fontSize: '12px', fontWeight: 700, color: '#FF6B35', marginBottom: '8px' }}>
                  💡 使用小贴士
                </p>
                {[
                  '🎡 大转盘：视觉化随机抽取，超好玩',
                  '💘 卡片滑选：右滑喜欢/左滑不喜欢，探探同款',
                  '🚫 拉黑功能：不想看到的餐厅直接排雷',
                  '✍️ UGC录入：发现宝藏小店随时添加',
                ].map(tip => (
                  <p key={tip} style={{ fontSize: '11px', color: '#888', lineHeight: 1.8 }}>{tip}</p>
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Success Toast */}
      <AnimatePresence>
        {showSuccess && (
          <motion.div
            initial={{ opacity: 0, y: -60 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -60 }}
            style={{
              position: 'absolute',
              top: '60px',
              left: '50%',
              transform: 'translateX(-50%)',
              zIndex: 99,
              whiteSpace: 'nowrap',
            }}
          >
            <div
              className="flex items-center gap-2 px-4 py-3 rounded-2xl"
              style={{
                background: '#1a1a1a',
                boxShadow: '0 8px 24px rgba(0,0,0,0.2)',
              }}
            >
              <span style={{ fontSize: '16px' }}>🎉</span>
              <span style={{ color: '#fff', fontSize: '13px', fontWeight: 600 }}>发布成功！已加入食堂</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}