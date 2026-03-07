import axios from 'axios';

/**
 * Axios instance pre-configured with the backend base URL.
 * The Authorization header is injected per-request by authService
 * so the client itself stays stateless.
 */
const apiClient = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

/**
 * Request interceptor — injects X-Session-Token from sessionStorage.
 */
apiClient.interceptors.request.use((config) => {
  const sessionToken = sessionStorage.getItem('sessionToken');
  if (sessionToken) {
    config.headers['X-Session-Token'] = sessionToken;
  }
  return config;
});

/**
 * Response interceptor — on 401, clear session and redirect to login.
 */
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem('accessToken');
      sessionStorage.removeItem('sessionToken');
      sessionStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
