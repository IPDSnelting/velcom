module.exports = {
  transpileDependencies: ['vuetify'],
  configureWebpack: {
    mode:
      process.env.VUE_APP_MODE === 'production' ? 'production' : 'development'
  }
}
