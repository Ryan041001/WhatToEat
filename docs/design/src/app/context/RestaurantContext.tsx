import React, { createContext, useContext, useState, useEffect } from 'react';

export interface Restaurant {
  id: string;
  name: string;
  category: string;
  tags: string[];
  rating: number;
  priceLevel: 1 | 2 | 3;
  image: string;
  isBlacklisted: boolean;
  distance: string;
  description: string;
  address: string;
  votes: { up: number; down: number };
  isUserAdded?: boolean;
}

const INITIAL_RESTAURANTS: Restaurant[] = [
  {
    id: '1',
    name: '辣爆天府川菜馆',
    category: '川菜',
    tags: ['超辣', '实惠', '量大', '下饭'],
    rating: 4.5,
    priceLevel: 2,
    image: 'https://images.unsplash.com/photo-1658853577859-7a75373c2675?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxjaGluZXNlJTIwc3BpY3klMjBob3Rwb3QlMjByZXN0YXVyYW50JTIwZm9vZHxlbnwxfHx8fDE3NzMxOTc2ODh8MA&ixlib=rb-4.1.0&q=80&w=1080',
    isBlacklisted: false,
    distance: '200m',
    description: '正宗四川口味，麻辣鲜香，学生最爱！每天排队打饭的那种',
    address: '学生街38号',
    votes: { up: 234, down: 12 },
    isUserAdded: false,
  },
  {
    id: '2',
    name: '一风堂拉面',
    category: '日料',
    tags: ['日式', '汤浓', '氛围好'],
    rating: 4.7,
    priceLevel: 3,
    image: 'https://images.unsplash.com/photo-1627900440398-5db32dba8db1?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxqYXBhbmVzZSUyMHJhbWVuJTIwYm93bCUyMG5vb2RsZXN8ZW58MXx8fHwxNzczMTk3Njg5fDA&ixlib=rb-4.1.0&q=80&w=1080',
    isBlacklisted: false,
    distance: '500m',
    description: '浓郁猪骨汤底，叉烧入味，豚骨拉面爱好者必打卡',
    address: '美食广场二楼B区',
    votes: { up: 189, down: 8 },
    isUserAdded: false,
  },
  {
    id: '3',
    name: '老王炒饭王',
    category: '快餐',
    tags: ['快手', '实惠', '打包方便'],
    rating: 4.2,
    priceLevel: 1,
    image: 'https://images.unsplash.com/photo-1723691802798-fa6efc67b2c9?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxjaGluZXNlJTIwZnJpZWQlMjByaWNlJTIwd29rfGVufDF8fHx8MTc3MzE4NzY0N3ww&ixlib=rb-4.1.0&q=80&w=1080',
    isBlacklisted: false,
    distance: '100m',
    description: '锅气十足，料给得很足，宿舍楼下5分钟搞定',
    address: '宿舍区西门对面',
    votes: { up: 312, down: 45 },
    isUserAdded: false,
  },
  {
    id: '4',
    name: '撸串儿烧烤夜市',
    category: '烧烤',
    tags: ['夜宵', '聚餐', '扎啤', '热闹'],
    rating: 4.6,
    priceLevel: 2,
    image: 'https://images.unsplash.com/photo-1717809184558-597a0f1b9eb0?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxncmlsbGVkJTIwYmJxJTIwbWVhdCUyMHNrZXdlcnN8ZW58MXx8fHwxNzczMTk3NjkwfDA&ixlib=rb-4.1.0&q=80&w=1080',
    isBlacklisted: false,
    distance: '800m',
    description: '晚上9点后人气爆棚，烤羊肉串和烤茄子必点',
    address: '夜市一条街66号',
    votes: { up: 445, down: 23 },
    isUserAdded: false,
  },
  {
    id: '5',
    name: '炸鸡汉堡快乐屋',
    category: '快餐',
    tags: ['炸鸡', '汉堡', '奶茶', '套餐'],
    rating: 4.0,
    priceLevel: 1,
    image: 'https://images.unsplash.com/photo-1760533536738-f0965fd52354?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxjcmlzcHklMjBmcmllZCUyMGNoaWNrZW4lMjBidXJnZXIlMjBmYXN0JTIwZm9vZHxlbnwxfHx8fDE3NzMxOTc2OTB8MA&ixlib=rb-4.1.0&q=80&w=1080',
    isBlacklisted: false,
    distance: '300m',
    description: '脆皮炸鸡配可乐，上课摸鱼想到了就流口水',
    address: '校门口广场旁',
    votes: { up: 267, down: 31 },
    isUserAdded: false,
  },
  {
    id: '6',
    name: '花见寿司',
    category: '日料',
    tags: ['精致', '颜值高', '约饭', '打卡'],
    rating: 4.8,
    priceLevel: 3,
    image: 'https://images.unsplash.com/photo-1712183718471-dab51f0748ac?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxzdXNoaSUyMHJvbGxzJTIwamFwYW5lc2UlMjBmb29kfGVufDF8fHx8MTc3MzA4NDgyMHww&ixlib=rb-4.1.0&q=80&w=1080',
    isBlacklisted: false,
    distance: '1.2km',
    description: '摆盘好看，适合拍照发朋友圈，三文鱼超新鲜',
    address: '商业街B栋一楼',
    votes: { up: 156, down: 5 },
    isUserAdded: false,
  },
  {
    id: '7',
    name: '云南过桥米线',
    category: '米线',
    tags: ['清淡', '养胃', '汤鲜', '选料多'],
    rating: 4.4,
    priceLevel: 1,
    image: 'https://images.unsplash.com/photo-1644483662084-5a690debfdf8?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxyaWNlJTIwbm9vZGxlJTIwc291cCUyMGJvd2wlMjBhc2lhbnxlbnwxfHx8fDE3NzMxOTc2OTF8MA&ixlib=rb-4.1.0&q=80&w=1080',
    isBlacklisted: false,
    distance: '400m',
    description: '自选配料，汤底鲜甜，不辣选手的福音',
    address: '食堂南区一楼',
    votes: { up: 198, down: 18 },
    isUserAdded: false,
  },
  {
    id: '8',
    name: '隆记饺子馆',
    category: '北方面食',
    tags: ['家常', '皮薄馅大', '暖心'],
    rating: 4.3,
    priceLevel: 1,
    image: 'https://images.unsplash.com/photo-1694834589398-27b369c6f7a6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxkdW1wbGluZ3MlMjBkaW0lMjBzdW0lMjBjaGluZXNlJTIwZm9vZHxlbnwxfHx8fDE3NzMxOTc2OTR8MA&ixlib=rb-4.1.0&q=80&w=1080',
    isBlacklisted: false,
    distance: '600m',
    description: '东北大饺子，韭菜猪肉馅超香，冬天吃最幸福',
    address: '教职工餐厅对面',
    votes: { up: 143, down: 9 },
    isUserAdded: false,
  },
  {
    id: '9',
    name: '韩式拌饭小厨',
    category: '韩餐',
    tags: ['韩式', '营养均衡', '小清新'],
    rating: 4.6,
    priceLevel: 2,
    image: 'https://images.unsplash.com/photo-1628441309764-794e7362f6e6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxrb3JlYW4lMjBiaWJpbWJhcCUyMHJpY2UlMjBib3dsJTIwY29sb3JmdWx8ZW58MXx8fHwxNzczMTgzMzYxfDA&ixlib=rb-4.1.0&q=80&w=1080',
    isBlacklisted: false,
    distance: '700m',
    description: '石锅拌饭锅巴超香，石锅部队火锅也是绝绝子',
    address: '学府路22号',
    votes: { up: 211, down: 14 },
    isUserAdded: false,
  },
  {
    id: '10',
    name: '意大利手工披萨',
    category: '西餐',
    tags: ['拉丝', '聚餐', '西式', '约会'],
    rating: 4.5,
    priceLevel: 3,
    image: 'https://images.unsplash.com/photo-1596458397260-255807e979f1?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwaXp6YSUyMGl0YWxpYW4lMjBmb29kJTIwY2hlZXNlfGVufDF8fHx8MTc3MzE5NzY5NHww&ixlib=rb-4.1.0&q=80&w=1080',
    isBlacklisted: false,
    distance: '1.5km',
    description: '手抛薄底披萨，马苏里拉芝士超拉丝，情侣约会必备',
    address: '创意园区C座',
    votes: { up: 178, down: 22 },
    isUserAdded: false,
  },
  {
    id: '11',
    name: '台式牛肉面馆',
    category: '面食',
    tags: ['台湾味', '卤味', '肉厚', '暖胃'],
    rating: 4.4,
    priceLevel: 2,
    image: 'https://images.unsplash.com/photo-1769358471999-55213f905814?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxiZWVmJTIwbm9vZGxlJTIwc291cCUyMHRhaXdhbmVzZXxlbnwxfHx8fDE3NzMxOTc2OTZ8MA&ixlib=rb-4.1.0&q=80&w=1080',
    isBlacklisted: false,
    distance: '900m',
    description: '慢炖牛腩入口即化，汤头浓郁，一碗下肚超满足',
    address: '美食城三楼C12',
    votes: { up: 224, down: 11 },
    isUserAdded: false,
  },
];

interface RestaurantContextType {
  restaurants: Restaurant[];
  addRestaurant: (r: Omit<Restaurant, 'id' | 'votes' | 'isUserAdded'>) => void;
  toggleBlacklist: (id: string) => void;
  voteRestaurant: (id: string, type: 'up' | 'down') => void;
  deleteRestaurant: (id: string) => void;
  getActiveRestaurants: () => Restaurant[];
}

const RestaurantContext = createContext<RestaurantContextType | null>(null);

export function RestaurantProvider({ children }: { children: React.ReactNode }) {
  const [restaurants, setRestaurants] = useState<Restaurant[]>(() => {
    try {
      const stored = localStorage.getItem('ganfan_restaurants');
      return stored ? JSON.parse(stored) : INITIAL_RESTAURANTS;
    } catch {
      return INITIAL_RESTAURANTS;
    }
  });

  useEffect(() => {
    localStorage.setItem('ganfan_restaurants', JSON.stringify(restaurants));
  }, [restaurants]);

  const addRestaurant = (r: Omit<Restaurant, 'id' | 'votes' | 'isUserAdded'>) => {
    const newR: Restaurant = {
      ...r,
      id: Date.now().toString(),
      votes: { up: 0, down: 0 },
      isUserAdded: true,
    };
    setRestaurants(prev => [newR, ...prev]);
  };

  const toggleBlacklist = (id: string) => {
    setRestaurants(prev =>
      prev.map(r => r.id === id ? { ...r, isBlacklisted: !r.isBlacklisted } : r)
    );
  };

  const voteRestaurant = (id: string, type: 'up' | 'down') => {
    setRestaurants(prev =>
      prev.map(r => r.id === id
        ? { ...r, votes: { ...r.votes, [type]: r.votes[type] + 1 } }
        : r
      )
    );
  };

  const deleteRestaurant = (id: string) => {
    setRestaurants(prev => prev.filter(r => r.id !== id));
  };

  const getActiveRestaurants = () => restaurants.filter(r => !r.isBlacklisted);

  return (
    <RestaurantContext.Provider value={{
      restaurants, addRestaurant, toggleBlacklist, voteRestaurant, deleteRestaurant, getActiveRestaurants
    }}>
      {children}
    </RestaurantContext.Provider>
  );
}

export function useRestaurants() {
  const ctx = useContext(RestaurantContext);
  if (!ctx) throw new Error('useRestaurants must be used within RestaurantProvider');
  return ctx;
}
