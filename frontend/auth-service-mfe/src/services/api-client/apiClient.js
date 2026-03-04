import axios from 'axios';

/**
 * Axios instance pre-configured with the backend base URL.
 * The Authorization header is injected per-request by authService
 * so the client itself stays stateless.
 *
 * Phase 2: withCredentials enables sending httpOnly cookies with requests.
 * This allows the backend to read the refreshToken cookie automatically.
 */
const apiClient = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true, // Phase 2: Send cookies with all requests
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
