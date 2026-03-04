const { ModuleFederationPlugin } = require('webpack').container;

const deps = require('./package.json').dependencies;

// Detect environment: Docker uses service names, local dev uses localhost
const isDocker = process.env.DOCKER_ENV === 'true';
const authServiceHost = isDocker ? 'http://auth-service:3001' : 'http://localhost:3001';
const accountSettingsHost = isDocker ? 'http://account-settings:3002' : 'http://localhost:3002';

module.exports = {
  webpack: {
    configure: (webpackConfig) => {
      // Remove ForkTsCheckerWebpackPlugin — JS-only app, its vendored ajv-keywords@3
      // conflicts with the forced ajv@8 override.
      webpackConfig.plugins = webpackConfig.plugins.filter(
        (p) => p.constructor.name !== 'ForkTsCheckerWebpackPlugin'
      );

      // Module Federation requires a unique chunk name — disable CRA's default
      webpackConfig.output.uniqueName = 'landingPage';

      webpackConfig.plugins.push(
        new ModuleFederationPlugin({
          name: 'landingPage',
          filename: 'remoteEntry.js',
          remotes: {
            authService: `authService@${authServiceHost}/remoteEntry.js`,
            accountSettings: `accountSettings@${accountSettingsHost}/remoteEntry.js`,
          },
          exposes: {
            './AuthContext': './src/context/createAuthContext',
            './useAuth': './src/hooks/useAuth',
            './ThemeContext': './src/context/ThemeContext',
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
    port: 3000,
    headers: {
      'Access-Control-Allow-Origin': '*',
    },
  },
};
