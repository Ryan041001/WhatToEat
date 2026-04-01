Page({
  data: {
    restaurant: null,
    isLoading: true
  },
  onLoad(options) {
    const id = options.id || "101";
    this.fetchDetail(id);
  },
  fetchDetail(id) {
    this.setData({ isLoading: true });
    
    // Mock API
    setTimeout(() => {
      this.setData({
        restaurant: {
          id: id,
          name: `美味餐厅 ${id}`,
          rating: "4.8",
          avgPrice: "56",
          tags: ["中餐", "家常菜", "热门榜第1"],
          address: "浙江省杭州市西湖区教工路149号",
          dishes: [
            { name: "招牌红烧肉", image: "/assets/restaurant-3.svg" },
            { name: "糖醋排骨", image: "/assets/restaurant-4.svg" },
            { name: "酸菜鱼", image: "/assets/restaurant-5.svg" },
            { name: "地三鲜", image: "/assets/restaurant-6.svg" }
          ]
        },
        isLoading: false
      });
      wx.setNavigationBarTitle({
        title: `美味餐厅 ${id}`
      });
    }, 800);
  },
  handleOpenLocation() {
    wx.openLocation({
      latitude: 30.27408,
      longitude: 120.15507,
      name: this.data.restaurant.name,
      address: this.data.restaurant.address,
      scale: 18
    });
  },
  handleBack() {
    if (getCurrentPages().length > 1) {
      wx.navigateBack();
      return;
    }
    wx.switchTab({ url: "/pages/home/home" });
  },
  handleGoThere() {
    wx.showToast({
      title: "冲冲冲！",
      icon: "success"
    });
  }
})
