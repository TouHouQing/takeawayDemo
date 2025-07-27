const path = require('path')
const name = 'Vue Typescript Admin'
const IS_PROD = ['production', 'development'].includes(process.env.NODE_ENV)

module.exports = {
  'publicPath': process.env.NODE_ENV === 'production' ? './' : '/', // TODO: Remember to change this to fit your need
  'lintOnSave': process.env.NODE_ENV === 'development',
  'pwa': {
    'name': name
  },
  'pluginOptions': {
    'style-resources-loader': {
      'preProcessor': 'scss',
      'patterns': [
        path.resolve(__dirname, 'src/styles/_variables.scss'),
        path.resolve(__dirname, 'src/styles/_mixins.scss')
      ]
    }
  },
  // 开启代理
  devServer: {
    host:'0.0.0.0',
    public: '0.0.0.0:8888', // 本地的ip:端口号
    port: 8888,
    open: true,
    disableHostCheck:true,
    hot:true,//自动保存
    // 修复 fsevents 问题
    watchOptions: {
      poll: 1000,
      ignored: /node_modules/
    },
    overlay: {
      warnings: false,
      errors: true
    },
    proxy: {
      '/api': {
        target: process.env.VUE_APP_URL,
        ws: false,
        secure: false,
        changeOrigin: true,
        pathRewrite:{
          '^/api':''
        }
      }
    }
  },
  chainWebpack: (config) => {
    config.resolve.symlinks(true) // 修复热更新失效
  },
  configureWebpack: {
    devtool: 'source-map'
  },

  css: {
    // 是否使用css分离插件 ExtractTextPlugin
    extract: IS_PROD,
    // 开启 CSS source maps?
    sourceMap: false,
    // css预设器配置项
    loaderOptions: {
      scss: {
        // 移除 fibers 配置
        // fiber: require('fibers')
      }
    },
    // 启用 CSS modules for all css / pre-processor files.
    modules: false,
  },
};
