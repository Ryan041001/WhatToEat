export const getMockRestaurants = () => {
  return [
    {
      id: 'mock-1',
      poiId: 'B0FF1',
      name: '小陈生煎馆',
      category: '小吃',
      rating: 4.7,
      priceLevel: 1,
      image: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800',
      distance: '420m',
      description: '现包现煎，汤汁饱满。',
      address: '钱塘区学正街 120 号',
      tags: ['出餐快', '人气高'],
      votes: { up: 0, down: 0 },
      isBlacklisted: false,
      isUserAdded: false
    },
    {
      id: 'mock-2',
      poiId: 'B0FF2',
      name: '阿福牛肉面',
      category: '面食',
      rating: 4.5,
      priceLevel: 2,
      image: 'https://images.unsplash.com/photo-1617093727343-374698b1b08d?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800',
      distance: '760m',
      description: '牛骨慢炖汤底，面条劲道。',
      address: '钱塘区文泽路 58 号',
      tags: ['汤底浓', '分量足'],
      votes: { up: 0, down: 0 },
      isBlacklisted: false,
      isUserAdded: false
    },
    {
      id: 'mock-3',
      poiId: 'B0FF3',
      name: '福记川味馆',
      category: '川菜',
      rating: 4.6,
      priceLevel: 2,
      image: 'https://images.unsplash.com/photo-1552566626-52f8b828add9?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800',
      distance: '1.3km',
      description: '口味可调，香辣过瘾。',
      address: '钱塘区学源街 89 号',
      tags: ['可拼桌', '下饭'],
      votes: { up: 0, down: 0 },
      isBlacklisted: false,
      isUserAdded: false
    },
    {
      id: 'mock-4',
      poiId: 'B0FF4',
      name: '暖屋日式简餐',
      category: '日料',
      rating: 4.4,
      priceLevel: 3,
      image: 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800',
      distance: '980m',
      description: '盖饭和乌冬是招牌。',
      address: '钱塘区学林街 66 号',
      tags: ['环境好', '口味稳'],
      votes: { up: 0, down: 0 },
      isBlacklisted: false,
      isUserAdded: false
    },
    {
      id: 'mock-5',
      poiId: 'B0FF5',
      name: '老友烧烤档',
      category: '烧烤',
      rating: 4.3,
      priceLevel: 2,
      image: 'https://images.unsplash.com/photo-1529193591184-b1d58069ecdd?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800',
      distance: '1.8km',
      description: '夜宵首选，烟火气十足。',
      address: '钱塘区高沙商业街 32 号',
      tags: ['夜宵', '朋友聚餐'],
      votes: { up: 0, down: 0 },
      isBlacklisted: false,
      isUserAdded: false
    }
  ];
};

export const getMockUser = () => {
  return {
    id: '10001',
    nickname: '林同学',
    avatarUrl: ''
  };
};
