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
 * Global response interceptor — surfaces 401s clearly.
 * Components can catch these and trigger a logout.
 */
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Propagate as a typed error; AuthContext consumers handle the redirect
      error.isUnauthorized = true;
    }
    return Promise.reject(error);
  }
);

export default apiClient;
