module.exports = {
  transpileDependencies: ['vue-echarts', 'resize-detector'],
  publicPath: process.env.BASE_URL,
  chainWebpack: config => {
    config.plugin('define').tap(definitions => {
      // get git info from command line
      // eslint-disable-next-line
      const commitHash = require('child_process')
        .execSync('git rev-parse HEAD')
        .toString()
      definitions[0]['process.env']['__COMMIT_HASH__'] =
        JSON.stringify(commitHash)
      return definitions
    })
  }
}
