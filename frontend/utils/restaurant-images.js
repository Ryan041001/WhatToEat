const DEFAULT_IMAGE = '/assets/restaurant-images/default.svg';

const BRAND_IMAGE_RULES = [
  {
    patterns: ['肯德基', 'kfc'],
    image: 'https://upload.wikimedia.org/wikipedia/commons/b/bf/KFC_logo.svg'
  },
  {
    patterns: ['麦当劳', 'mcdonald'],
    image: 'https://upload.wikimedia.org/wikipedia/commons/4/4b/McDonald%27s_logo.svg'
  },
  {
    patterns: ['星巴克', 'starbucks'],
    image: 'https://upload.wikimedia.org/wikipedia/en/d/d3/Starbucks_Corporation_Logo_2011.svg'
  },
  {
    patterns: ['必胜客', 'pizza hut'],
    image: '/assets/restaurant-images/pizza.svg'
  },
  {
    patterns: ['古茗', '蜜雪冰城', '喜茶', '奈雪', '茶百道', '沪上阿姨', '霸王茶姬', 'coco', '一点点'],
    image: '/assets/restaurant-images/drink.svg'
  }
];

const CATEGORY_IMAGE_RULES = [
  { keywords: ['酒店', '宾馆', '旅馆', '客栈', 'hotel'], image: '/assets/restaurant-images/hotel.svg' },
  { keywords: ['饺子', '水饺', '煎饺', '锅贴'], image: '/assets/restaurant-images/jiaozi.svg' },
  { keywords: ['炒饭', '蛋炒饭', '扬州炒饭'], image: '/assets/restaurant-images/fried-rice.svg' },
  { keywords: ['汉堡', '堡'], image: '/assets/restaurant-images/burger.svg' },
  { keywords: ['披萨', 'pizza'], image: '/assets/restaurant-images/pizza.svg' },
  { keywords: ['淮南牛肉汤', '牛肉汤', '牛肉粿条', '粿条', '面', '粉', '米线', '拉面', '馄饨'], image: '/assets/restaurant-images/noodles.svg' },
  { keywords: ['猪脚饭', '卤肉饭', '盖浇饭', '煲仔饭', '黄焖鸡', '快餐'], image: '/assets/restaurant-images/rice.svg' },
  { keywords: ['包子', '肉包', '早餐', '早点', '粥', '豆浆', '烧麦'], image: '/assets/restaurant-images/dumpling.svg' },
  { keywords: ['奶茶', '茶饮', '饮品', '冷饮', '小甜水', '糖水', '果汁'], image: '/assets/restaurant-images/drink.svg' },
  { keywords: ['甜品', '蛋糕', '面包', '烘焙', '冰淇淋'], image: '/assets/restaurant-images/dessert.svg' },
  { keywords: ['咖啡', '咖啡厅'], image: '/assets/restaurant-images/cafe.svg' },
  { keywords: ['火锅', '麻辣烫', '冒菜', '串串'], image: '/assets/restaurant-images/hotpot.svg' },
  { keywords: ['烧烤', '烤肉', '烤串', '炸串'], image: '/assets/restaurant-images/grill.svg' },
  { keywords: ['日料', '日本料理', '寿司', '刺身'], image: '/assets/restaurant-images/sushi.svg' },
  { keywords: ['炸鸡', '西餐'], image: '/assets/restaurant-images/fast-food.svg' }
];

function normalizeText(value) {
  return String(value || '').toLowerCase();
}

export function resolveRestaurantImage({ name = '', category = '' } = {}) {
  const haystack = `${normalizeText(name)} ${normalizeText(category)}`;

  const brandRule = BRAND_IMAGE_RULES.find((rule) => rule.patterns.some((pattern) => haystack.includes(pattern.toLowerCase())));
  if (brandRule) {
    return brandRule.image;
  }

  const categoryRule = CATEGORY_IMAGE_RULES.find((rule) => rule.keywords.some((keyword) => haystack.includes(keyword.toLowerCase())));
  return categoryRule ? categoryRule.image : DEFAULT_IMAGE;
}
