Page({
  data: {
    restaurants: [],
    isLoading: false,
    hasMore: true,
    page: 1,
    searchQuery: ""
  },
  onLoad() {
    this.fetchData();
  },
  onSearchInput(e) {
    this.setData({
      searchQuery: e.detail.value,
      restaurants: [],
      page: 1,
      hasMore: true
    });
    this.fetchData();
  },
  onLoadMore(e) {
    if(!this.data.isLoading && this.data.hasMore) {
      this.setData({ page: this.data.page + 1 });
      this.fetchData();
    }
  },
  goToDetail(e) {
    const { id } = e.detail.restaurant;
    wx.navigateTo({
      url: `/pages/restaurants/restaurants?id=${id}`
    });
  },
  fetchData() {
    this.setData({ isLoading: true });
    // Mock API Call
    setTimeout(() => {
      const mockResult = Array.from({length: 10}).map((_, i) => ({
        id: this.data.page * 100 + i,
        name: `好吃的餐馆 ${this.data.page}-${i+1}${this.data.searchQuery ? " - " + this.data.searchQuery : ""}`,
        rating: (Math.random() * 2 + 3).toFixed(1),
        avgPrice: Math.floor(Math.random() * 50 + 15),
        description: "这里的推荐菜非常不错，回头客很多很多，环境还不错",
        tags: ["中餐", "快餐", "热度高"]
      }));
      this.setData({
        restaurants: [...this.data.restaurants, ...mockResult],
        isLoading: false,
        hasMore: this.data.page < 3 // 模拟只有三页
      });
    }, 1000);
  }
})
