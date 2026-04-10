Component({
  properties: {
    restaurant: {
      type: Object,
      value: {}
    }
  },
  data: {},
  methods: {
    onTap() {
      this.triggerEvent('tapcard', { restaurant: this.data.restaurant });
    }
  }
})