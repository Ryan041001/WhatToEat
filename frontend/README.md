# 干饭防纠结 - 微信小程序版

这是将原 React Web 应用转换为微信小程序的完整代码。

## 📁 项目结构

```
miniprogram/
├── app.js              # 全局逻辑（数据管理）
├── app.json            # 全局配置（页面、导航栏、TabBar）
├── app.wxss            # 全局样式
├── pages/              # 页面目录
│   ├── home/          # 首页
│   │   ├── home.wxml  # 页面结构
│   │   ├── home.wxss  # 页面样式
│   │   ├── home.js    # 页面逻辑
│   │   └── home.json  # 页面配置
│   ├── spin/          # 大转盘页面
│   ├── swipe/         # 卡片滑选页面（需补充）
│   ├── restaurants/   # 餐厅列表页面（需补充）
│   └── mine/          # 我的页面
├── images/            # 图片资源（TabBar 图标等）
└── README.md          # 说明文档
```

## 🎯 已完成的页面

### 1. **首页（home）**
- ✅ 头部橙色区域（今天吃什么）
- ✅ 快速随机选择按钮
- ✅ 三大功能入口（大转盘、摇一摇、卡片滑）
- ✅ 摇一摇动画和结果弹窗
- ✅ 数据统计（总餐厅、可选择、已拉黑）
- ✅ 热门推荐（Top 3 评分）
- ✅ 添加餐厅入口

### 2. **大转盘（spin）**
- ✅ Canvas 绘制转盘
- ✅ 旋转动画（缓动函数）
- ✅ 随机抽取逻辑
- ✅ 结果展示卡片
- ✅ 再转一次功能

### 3. **我的（mine）**
- ✅ 用户信息展示
- ✅ 数据统计卡片
- ✅ 添加餐厅入口
- ✅ 我添加的餐厅列表
- ✅ 设置和关于入口
- ✅ 使用贴士

### 4. **全局功能（app.js）**
- ✅ 餐厅数据管理
- ✅ 本地存储（LocalStorage）
- ✅ 添加/删除餐厅
- ✅ 拉黑/取消拉黑
- ✅ 获取可选餐厅

## 📝 待补充的页面

以下页面需要继续创建：

### 1. **卡片滑选页面（swipe）**
需要实现：
- 卡片堆叠视图
- 左右滑动手势识别
- 滑动动画效果
- 结果展示

### 2. **餐厅列表页面（restaurants）**
需要实现：
- 餐厅列表展示
- 分类筛选
- 价格筛选
- 排序功能
- 拉黑/取消拉黑操作

### 3. **添加餐厅页面（add-restaurant）**
需要实现：
- 表单输入（名称、分类、地址等）
- 图片选择
- 标签选择
- 评分选择
- 表单验证

### 4. **设置页面（settings）**
需要实现：
- 通知开关
- 清除缓存
- 版本信息

### 5. **关于页面（about）**
需要实现：
- 应用介绍
- 核心功能列表
- 版本号

## 🎨 样式说明

### 单位换算
- **rpx**：微信小程序响应式像素单位
- 1px（设计稿）= 2rpx（小程序）
- 例如：React 中的 `12px` → 小程序中的 `24rpx`

### 配色方案
```css
橘子橙：#F97316  /* 主色 */
蓝莓紫：#4361EE  /* 辅助色 */
牛油果绿：#84A98C /* 成功色 */
米饭白：#F8F7F4  /* 背景色 */
芝麻灰：#5C5C5C  /* 正文 */
淡云灰：#E8E8E8  /* 分割线 */
```

### 圆角规范
```css
小圆角：24rpx (12px)
中圆角：32rpx (16px)
大圆角：48rpx (24px)
超大圆角：100rpx (50px) 用于胶囊按钮
```

## 🔧 开发步骤

### 1. 导入项目
1. 打开微信开发者工具
2. 新建小程序项目
3. 将 `miniprogram/` 文件夹中的所有文件复制到项目目录

### 2. 配置 AppID
在微信开发者工具中设置你的小程序 AppID（测试可使用测试号）

### 3. 准备图标资源
在 `images/tab/` 目录下放置 TabBar 图标：
- `home.png` / `home-active.png`
- `spin.png` / `spin-active.png`
- `swipe.png` / `swipe-active.png`
- `restaurant.png` / `restaurant-active.png`
- `mine.png` / `mine-active.png`

图标尺寸：81px × 81px（推荐）

### 4. 补充缺失页面
参考已完成的页面结构，创建以下页面：
- `pages/swipe/` 卡片滑选
- `pages/restaurants/` 餐厅列表
- `pages/add-restaurant/` 添加餐厅
- `pages/settings/` 设置
- `pages/about/` 关于

### 5. 测试功能
- 测试数据读写（本地存储）
- 测试页面跳转
- 测试动画效果
- 测试手势交互

## 📱 与 React 版本的主要区别

| 功能 | React 版本 | 微信小程序版本 |
|------|-----------|--------------|
| **路由** | React Router | `wx.navigateTo` / `wx.switchTab` |
| **状态管理** | React Context | `app.globalData` + `setData` |
| **样式** | Tailwind CSS | WXSS（内联样式） |
| **事件** | `onClick` | `bindtap` / `catchtap` |
| **数据绑定** | `{value}` | `{{value}}` |
| **动画** | Framer Motion | CSS Animation + WXAPI |
| **存储** | LocalStorage | `wx.getStorageSync` / `wx.setStorageSync` |
| **Canvas** | HTML Canvas | 微信 Canvas API |

## 🚀 核心 API 使用

### 页面跳转
```javascript
// Tab 页面切换
wx.switchTab({ url: '/pages/home/home' });

// 普通页面跳转
wx.navigateTo({ url: '/pages/add-restaurant/add-restaurant' });

// 返回上一页
wx.navigateBack();
```

### 数据存储
```javascript
// 同步存储
wx.setStorageSync('key', value);

// 同步读取
const value = wx.getStorageSync('key');

// 异步存储
wx.setStorage({
  key: 'key',
  data: value,
  success: () => {}
});
```

### 提示反馈
```javascript
// Toast 提示
wx.showToast({ title: '操作成功', icon: 'success' });

// Loading
wx.showLoading({ title: '加载中' });
wx.hideLoading();

// Modal 弹窗
wx.showModal({
  title: '提示',
  content: '确定要删除吗？',
  success: (res) => {
    if (res.confirm) {
      // 用户点击确定
    }
  }
});
```

### 震动反馈
```javascript
// 轻震动
wx.vibrateShort({ type: 'light' });

// 中震动
wx.vibrateShort({ type: 'medium' });

// 重震动
wx.vibrateShort({ type: 'heavy' });
```

## 📌 注意事项

1. **Canvas 绘制**：小程序的 Canvas 2D API 与 Web 不同，需要使用 `wx.createSelectorQuery()` 获取节点

2. **手势识别**：使用 `bindtouchstart`、`bindtouchmove`、`bindtouchend` 实现卡片滑动

3. **图片资源**：
   - 本地图片放在项目目录中
   - 网络图片需要在后台配置合法域名

4. **rpx 单位**：
   - 屏幕宽度固定为 750rpx
   - 适配所有屏幕尺寸

5. **性能优化**：
   - 避免频繁 setData
   - 长列表使用虚拟滚动
   - 图片懒加载

## 🎓 学习资源

- [微信小程序官方文档](https://developers.weixin.qq.com/miniprogram/dev/framework/)
- [小程序 Canvas 文档](https://developers.weixin.qq.com/miniprogram/dev/api/canvas/Canvas.html)
- [小程序手势系统](https://developers.weixin.qq.com/miniprogram/dev/framework/view/wxml/event.html)

## 📄 License

MIT License
