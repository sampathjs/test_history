/* eslint-disable @typescript-eslint/no-var-requires */
const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function (app) {
  app.use(
    '/toms',
    createProxyMiddleware({
      target: process.env.REACT_APP_API_URL,
      pathRewrite: {
        '^/toms': '/',
      },
      changeOrigin: true,
      secure: false,
    })
  );
};
