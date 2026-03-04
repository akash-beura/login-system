const { ModuleFederationPlugin } = require('webpack').container;

const deps = require('./package.json').dependencies;

// Detect environment: Docker uses service names, local dev uses localhost
const isDocker = process.env.DOCKER_ENV === 'true';
const landingPageHost = isDocker ? 'http://landing-page:3000' : 'http://localhost:3000';

module.exports = {
  webpack: {
    configure: (webpackConfig) => {
      // Remove ForkTsCheckerWebpackPlugin — JS-only app, its vendored ajv-keywords@3
      // conflicts with the forced ajv@8 override.
      webpackConfig.plugins = webpackConfig.plugins.filter(
        (p) => p.constructor.name !== 'ForkTsCheckerWebpackPlugin'
      );

      webpackConfig.output.uniqueName = 'accountSettings';
      webpackConfig.output.publicPath = 'http://localhost:3002/';

      webpackConfig.plugins.push(
        new ModuleFederationPlugin({
          name: 'accountSettings',
          filename: 'remoteEntry.js',
          remotes: {
            landingPage: `landingPage@${landingPageHost}/remoteEntry.js`,
          },
          exposes: {
            './AccountSettingsPage': './src/pages/AccountSettingsPage',
          },
          shared: {
            react: {
              singleton: true,
              requiredVersion: deps.react,
              eager: false,
            },
            'react-dom': {
              singleton: true,
              requiredVersion: deps['react-dom'],
              eager: false,
            },
            'react-router-dom': {
              singleton: true,
              requiredVersion: deps['react-router-dom'],
              eager: false,
            },
          },
        })
      );

      return webpackConfig;
    },
  },
  devServer: {
    port: 3002,
    headers: {
      'Access-Control-Allow-Origin': '*',
    },
  },
};
