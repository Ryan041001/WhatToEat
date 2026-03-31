import { WechatLogin, GetMe, Logout } from '../../api/auth.js';

const app = getApp();

const CATEGORY_OPTIONS = ['川菜', '日料', '快餐', '烧烤', '米线', '面食', '韩餐', '西餐', '北方面食', '粤菜', '湘菜', '火锅', '小吃', '甜品', '其他'];
const PRESET_TAGS = ['辣', '不辣', '实惠', '量大', '环境好', '快手', '排队多', '打卡', '下饭', '养胃', '清淡', '聚餐', '约会', '宵夜', '宿舍楼下'];
const PRICE_OPTIONS = [
  { label: '¥ 人均15以下', value: 1 },
  { label: '¥¥ 人均15-30', value: 2 },
  { label: '¥¥¥ 人均30+', value: 3 },
];
const DEFAULT_IMAGES = [
  'https://images.unsplash.com/photo-1658853577859-7a75373c2675?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400',
  'https://images.unsplash.com/photo-1627900440398-5db32dba8db1?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400',
  'https://images.unsplash.com/photo-1723691802798-fa6efc67b2c9?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400',
  'https://images.unsplash.com/photo-1694834589398-27b369c6f7a6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400',
  'https://images.unsplash.com/photo-1717809184558-597a0f1b9eb0?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400',
  'https://images.unsplash.com/photo-1760533536738-f0965fd52354?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400'
];

Page({
  data: {
    isLoggedIn: false,
    userInfo: null,
    showAddForm: false,
    stats: {
      total: 0,
      userAdded: 0,
      actives: 0
    },
    userAddedList: [],
    
    // Add Form Options
    categoryOptions: CATEGORY_OPTIONS,
    presetTags: PRESET_TAGS,
    priceOptions: PRICE_OPTIONS,
    defaultImages: DEFAULT_IMAGES,
    
    // Form State
    form: {
      name: '',
      category: '',
      priceLevel: 1,
      selectedTags: [],
      selectedTagsMap: {},
      description: '',
      address: '',
      distance: '',
      rating: 4.0,
      image: DEFAULT_IMAGES[0]
    },
    customTag: '',
    errors: {}
  },

  onShow() {
    this.checkLoginStatus();
    this.loadData();
  },

  async checkLoginStatus() {
    const token = wx.getStorageSync('token');
    if (token) {
      try {
        const res = await GetMe();
        if (res.data) {
          this.setData({
            isLoggedIn: true,
            userInfo: res.data
          });
        }
      } catch (err) {
        console.error('获取用户信息失败', err);
        this.setData({ isLoggedIn: false, userInfo: null });
      }
    } else {
      this.setData({ isLoggedIn: false, userInfo: null });
    }
  },

  handleLogin() {
    wx.showLoading({ title: '登录中...' });
    wx.login({
      success: async (res) => {
        if (res.code) {
          try {
            console.log('wx.login 拿到的 code:', res.code);
            const loginRes = await WechatLogin(res.code);
            const { token, user } = loginRes.data;
            wx.setStorageSync('token', token);
            wx.showToast({ title: '登录成功', icon: 'success' });
            
            this.setData({
              isLoggedIn: true,
              userInfo: user
            });
            console.log('登录成功，已获取用户信息:', user);
          } catch (err) {
            console.error('登录请求失败', err);
            wx.showToast({ title: '登录失败', icon: 'error' });
          }
        } else {
          wx.showToast({ title: '获取code失败', icon: 'none' });
        }
      },
      fail: (err) => {
        wx.showToast({ title: '微信登录失败', icon: 'none' });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await Logout();
          } catch (e) {
            console.error(e);
          }
          wx.removeStorageSync('token');
          this.setData({
            isLoggedIn: false,
            userInfo: null
          });
          wx.showToast({ title: '已退出登录', icon: 'none' });
        }
      }
    })
  },

  goBack() {
    if (getCurrentPages().length > 1) {
      wx.navigateBack();
      return;
    }
    wx.redirectTo({ url: '/pages/home/home' });
  },

  loadData() {
    const all = app.getRestaurants() || [];
    const userAddedList = all.filter(r => r.isUserAdded);
    const actives = app.getActiveRestaurants() || [];
    
    this.setData({
      stats: {
        total: all.length,
        userAdded: userAddedList.length,
        actives: actives.length
      },
      userAddedList
    });
  },

  goToRestaurants() {
    wx.navigateTo({
      url: '/pages/restaurants/restaurants'
    });
  },

  openAddForm() {
    this.setData({ showAddForm: true });
  },

  closeAddForm() {
    this.setData({ showAddForm: false });
  },

  // Form Interactions
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    const val = e.detail.value;
    this.setData({
      [`form.${field}`]: val,
      [`errors.${field}`]: ''
    });
  },

  selectImage(e) {
    this.setData({ 'form.image': e.currentTarget.dataset.img });
  },

  selectCategory(e) {
    this.setData({ 
      'form.category': e.currentTarget.dataset.cat,
      'errors.category': ''
    });
  },

  selectPrice(e) {
    this.setData({ 'form.priceLevel': e.currentTarget.dataset.val });
  },

  selectRating(e) {
    this.setData({ 'form.rating': e.currentTarget.dataset.rate });
  },

  togglePresetTag(e) {
    const tag = e.currentTarget.dataset.tag;
    const { selectedTags, selectedTagsMap } = this.data.form;
    
    let newTags = [...selectedTags];
    let newMap = { ...selectedTagsMap };

    if (newMap[tag]) {
      newTags = newTags.filter(t => t !== tag);
      delete newMap[tag];
    } else {
      if (newTags.length < 8) {
        newTags.push(tag);
        newMap[tag] = true;
      } else {
        wx.showToast({ title: '最多8个标签', icon: 'none' });
        return;
      }
    }

    this.setData({
      'form.selectedTags': newTags,
      'form.selectedTagsMap': newMap
    });
  },

  onInputCustomTag(e) {
    this.setData({ customTag: e.detail.value });
  },

  addCustomTag() {
    const tag = this.data.customTag.trim();
    if (!tag) return;
    
    const { selectedTags, selectedTagsMap } = this.data.form;
    if (selectedTagsMap[tag]) {
       this.setData({ customTag: '' });
       return;
    }

    if (selectedTags.length >= 8) {
      wx.showToast({ title: '最多8个标签', icon: 'none' });
      return;
    }

    const newTags = [...selectedTags, tag];
    const newMap = { ...selectedTagsMap, [tag]: true };

    this.setData({
      'form.selectedTags': newTags,
      'form.selectedTagsMap': newMap,
      customTag: ''
    });
  },

  validate() {
    const { form } = this.data;
    let errors = {};
    if (!form.name.trim()) errors.name = '请填写餐厅名称';
    if (!form.category) errors.category = '请选择分类';
    if (!form.address.trim()) errors.address = '请填写地址';
    
    this.setData({ errors });
    return Object.keys(errors).length === 0;
  },

  handleSubmit() {
    if (!this.validate()) return;

    const { form } = this.data;
    
    app.addRestaurant({
      name: form.name.trim(),
      category: form.category,
      priceLevel: form.priceLevel,
      tags: form.selectedTags,
      description: form.description.trim() || '用户添加的餐厅',
      address: form.address.trim(),
      distance: form.distance.trim() || '附近',
      rating: form.rating,
      image: form.image
    });

    // Reset and close
    this.setData({
      showAddForm: false,
      form: {
        name: '',
        category: '',
        priceLevel: 1,
        selectedTags: [],
        selectedTagsMap: {},
        description: '',
        address: '',
        distance: '',
        rating: 4.0,
        image: DEFAULT_IMAGES[0]
      },
      customTag: '',
      errors: {}
    });

    wx.showToast({
      title: '发布成功！',
      icon: 'success',
      duration: 2000
    });

    this.loadData();
  }
});