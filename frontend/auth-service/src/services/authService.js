import apiClient from './apiClient';

const authService = {
  register: async (formData) => {
    const { data } = await apiClient.post('/auth/register', formData);
    return data;
  },
  login: async (email, password) => {
    const { data } = await apiClient.post('/auth/login', { email, password });
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
  refresh: async (refreshToken) => {
    const { data } = await apiClient.post('/auth/refresh', { refreshToken });
    return data;
  },
  exchangeOAuthCode: async (code) => {
    const { data } = await apiClient.post('/auth/oauth2/token', { code });
    return data;
  },
  logout: async (accessToken) => {
    await apiClient.post('/auth/logout', {}, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  },
};

export default authService;
