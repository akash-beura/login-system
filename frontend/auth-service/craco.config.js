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

      webpackConfig.output.uniqueName = 'authService';
      webpackConfig.output.publicPath = 'http://localhost:3001/';

      webpackConfig.plugins.push(
        new ModuleFederationPlugin({
          name: 'authService',
          filename: 'remoteEntry.js',
          // Consuming useAuth/AuthContext from landingPage avoids duplicating auth state
          remotes: {
            landingPage: `landingPage@${landingPageHost}/remoteEntry.js`,
          },
          exposes: {
            './LoginPage':             './src/pages/login/LoginPage',
            './RegisterPage':          './src/pages/register/RegisterPage',
            './OAuthCallbackPage':     './src/pages/oauth-callback/OAuthCallbackPage',
            './SetPasswordPage':       './src/pages/set-password/SetPasswordPage',
            './SetPasswordPromptPage': './src/pages/set-password-prompt/SetPasswordPromptPage',
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
    port: 3001,
    headers: {
      'Access-Control-Allow-Origin': '*',
    },
  },
};
