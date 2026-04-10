Component({
  properties: {
    current: {
      type: String,
      value: 'home'
    }
  },

  methods: {
    onNavTap(e) {
      const target = e.currentTarget.dataset.target;
      if (!target || target === this.data.current) return;

      wx.redirectTo({
        url: `/pages/${target}/${target}`
      });
    }
  }
});