Page({
  data: {},
  handleLogin() {
    wx.showLoading({ title: "登录中..." });
    setTimeout(() => {
      wx.hideLoading();
      wx.switchTab({
        url: "/pages/home/home"
      });
    }, 1000);
  }
})
