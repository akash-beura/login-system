module.exports = {
  webpack: {
    configure: (webpackConfig) => {
      // Remove plugins that cause issues with ajv version conflicts
      // - ForkTsCheckerWebpackPlugin: vendored ajv-keywords@3 conflicts with forced ajv@8
      // - ESLintPlugin: also has eslint version conflicts
      webpackConfig.plugins = webpackConfig.plugins.filter(
        (p) => p.constructor.name !== 'ForkTsCheckerWebpackPlugin' && p.constructor.name !== 'ESLintPlugin'
      );

      return webpackConfig;
    },
  },
  devServer: {
    port: 3000,
  },
  eslint: {
    enable: false,
  },
};
