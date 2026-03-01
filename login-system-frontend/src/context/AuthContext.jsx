import { createContext, useState, useCallback, useEffect, useRef } from 'react';
import authService from '../services/authService';

/**
 * AuthContext — single source of truth for auth state.
 *
 * Persistence strategy:
 * - refreshToken stored in localStorage — survives page refresh and browser restart
 * - accessToken stored in React state (memory) only — never persisted directly
 * - On mount: if a stored refreshToken exists, auto-restore the session via /auth/refresh
 * - `initialized` is false until the restore attempt completes; prevents a flash of
 *   the login page on reload for authenticated users
 */
export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(null);
  const [user, setUser]               = useState(null);
  const [initialized, setInitialized] = useState(false);
  const restoring = useRef(false); // guard against StrictMode double-invoke

  useEffect(() => {
    if (restoring.current) return;
    restoring.current = true;

    const storedRefreshToken = localStorage.getItem('refreshToken');
    if (!storedRefreshToken) {
      setInitialized(true);
      return;
    }

    authService.refresh(storedRefreshToken)
      .then((data) => {
        setAccessToken(data.accessToken);
        setUser(data.user);
        if (data.refreshToken) {
          localStorage.setItem('refreshToken', data.refreshToken);
        }
      })
      .catch(() => {
        localStorage.removeItem('refreshToken');
      })
      .finally(() => {
        setInitialized(true);
      });
  }, []);

  const login = useCallback((token, userData, newRefreshToken) => {
    setAccessToken(token);
    setUser(userData);
    if (newRefreshToken) {
      localStorage.setItem('refreshToken', newRefreshToken);
    }
  }, []);

  const logout = useCallback(() => {
    // Fire-and-forget backend logout to revoke refresh tokens server-side
    if (accessToken) {
      authService.logout(accessToken).catch(() => {});
    }
    setAccessToken(null);
    setUser(null);
    localStorage.removeItem('refreshToken');
  }, [accessToken]);

  const value = {
    accessToken,
    user,
    isAuthenticated: !!accessToken,
    initialized,
    login,
    logout,
  };

  if (!initialized) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>
        Loading…
      </div>
    );
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
