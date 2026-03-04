import apiClient from '../api-client/apiClient';

const userService = {
  /**
   * GET /users/me — returns full UserResponse (all profile fields).
   */
  getMe: async (accessToken) => {
    const { data } = await apiClient.get('/users/me', {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    return data;
  },

  /**
   * PUT /users/me — partial update; null fields are ignored by the backend.
   */
  updateMe: async (accessToken, payload) => {
    const { data } = await apiClient.put('/users/me', payload, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    return data;
  },
};

export default userService;
