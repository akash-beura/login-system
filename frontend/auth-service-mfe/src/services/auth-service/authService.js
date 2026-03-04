import apiClient from '../api-client/apiClient';

const authService = {
  register: async (formData) => {
    const { data } = await apiClient.post('/auth/register', formData);
    // Phase 2: Response no longer includes refreshToken (stored in httpOnly cookie)
    return data;
  },
  login: async (email, password) => {
    const { data } = await apiClient.post('/auth/login', { email, password });
    // Phase 2: Response no longer includes refreshToken (stored in httpOnly cookie)
    return data;
  },
  setPassword: async (password, confirmPassword, accessToken) => {
    const { data } = await apiClient.post(
      '/auth/set-password',
      { password, confirmPassword },
      { headers: { Authorization: `Bearer ${accessToken}` } }
    );
    return data;
  },
  // Phase 2: refresh() no longer takes refreshToken parameter
  // Browser automatically sends the httpOnly cookie with the request
  refresh: async () => {
    const { data } = await apiClient.post('/auth/refresh', {});
    return data;
  },
  exchangeOAuthCode: async (code) => {
    const { data } = await apiClient.post('/auth/oauth2/token', { code });
    // Phase 2: Response no longer includes refreshToken (stored in httpOnly cookie)
    return data;
  },
  logout: async (accessToken) => {
    await apiClient.post('/auth/logout', {}, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  },
};

export default authService;
