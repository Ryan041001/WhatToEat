Page({
  data: {
    userInfo: {
      nickName: "张三的测试账号",
      avatarUrl: ""
    }
  },
  onLoad() {
    // Load auth status
  },
  handleMenuClick(e) {
    const type = e.currentTarget.dataset.type;
    wx.showToast({
      title: `点击了 ${type}`,
      icon: "none"
    });
  },
  handleLogout() {
    wx.showModal({
      title: "提示",
      content: "确定要退出登录吗？",
      success: (res) => {
        if (res.confirm) {
          wx.reLaunch({
            url: "/pages/index/index"
          });
        }
      }
    });
  }
})
