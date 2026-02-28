import { createContext, useState, useCallback } from 'react';

/**
 * AuthContext — single source of truth for auth state.
 *
 * Security (per react-ui-guidelines.md):
 * - JWT stored in memory (React state) only — never localStorage/sessionStorage.
 * - Tokens are lost on page refresh, which forces re-login (acceptable for this project).
 *   Phase 2 can migrate to httpOnly cookies for persistence.
 */
export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(null);
  const [user, setUser] = useState(null);

  const login = useCallback((token, userData) => {
    setAccessToken(token);
    setUser(userData);
  }, []);

  const logout = useCallback(() => {
    setAccessToken(null);
    setUser(null);
  }, []);

  const value = {
    accessToken,
    user,
    isAuthenticated: !!accessToken,
    login,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
