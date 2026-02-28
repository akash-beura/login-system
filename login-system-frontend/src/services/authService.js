import apiClient from './apiClient';

/**
 * All calls to /api/v1/auth/*.
 * Returns the response data directly; callers handle errors.
 */
const authService = {
  /**
   * POST /auth/register
   * Accepts the full registration form object (name, email, password, and optional
   * phone/address fields) and returns AuthResponse { accessToken, refreshToken, user }.
   */
  register: async (formData) => {
    const { data } = await apiClient.post('/auth/register', formData);
    return data;
  },

  /**
   * POST /auth/login
   * Returns AuthResponse.
   * If requiresPasswordSet=true, tokens will be absent — caller redirects to /set-password.
   */
  login: async (email, password) => {
    const { data } = await apiClient.post('/auth/login', { email, password });
    return data;
  },

  /**
   * POST /auth/set-password  (requires Bearer token)
   * Returns AuthResponse with full tokens after account linking completes.
   */
  setPassword: async (password, confirmPassword, accessToken) => {
    const { data } = await apiClient.post(
      '/auth/set-password',
      { password, confirmPassword },
      { headers: { Authorization: `Bearer ${accessToken}` } }
    );
    return data;
  },

  /**
   * POST /auth/refresh
   * Returns a new AuthResponse with rotated tokens.
   */
  refresh: async (refreshToken) => {
    const { data } = await apiClient.post('/auth/refresh', { refreshToken });
    return data;
  },

  /**
   * POST /auth/oauth2/token
   * Exchanges the one-time OAuth code (from redirect URL) for AuthResponse.
   */
  exchangeOAuthCode: async (code) => {
    const { data } = await apiClient.post('/auth/oauth2/token', { code });
    return data;
  },
};

export default authService;
