import { useState, useCallback } from 'react';
import { AuthContext } from './createAuthContext';
import authService from '../services/auth-service/authService';

export { AuthContext } from './createAuthContext';

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(() => sessionStorage.getItem('accessToken'));
  const [sessionToken, setSessionToken] = useState(() => sessionStorage.getItem('sessionToken'));
  const [user, setUser] = useState(() => {
    const stored = sessionStorage.getItem('user');
    return stored ? JSON.parse(stored) : null;
  });

  const login = useCallback((token, session, userData) => {
    setAccessToken(token);
    setSessionToken(session);
    setUser(userData);
    sessionStorage.setItem('accessToken', token);
    sessionStorage.setItem('sessionToken', session);
    sessionStorage.setItem('user', JSON.stringify(userData));
  }, []);

  const logout = useCallback(() => {
    if (accessToken && sessionToken) {
      authService.logout(accessToken, sessionToken).catch(() => {});
    }
    setAccessToken(null);
    setSessionToken(null);
    setUser(null);
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('sessionToken');
    sessionStorage.removeItem('user');
  }, [accessToken, sessionToken]);

  const value = {
    accessToken,
    sessionToken,
    user,
    isAuthenticated: !!accessToken,
    initialized: true,
    login,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
