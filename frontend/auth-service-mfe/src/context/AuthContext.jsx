import { useState, useCallback, useEffect, useRef } from 'react';
import { AuthContext } from './createAuthContext';
import authService from '../services/auth-service/authService';

/**
 * AuthProvider — wraps the app with auth context.
 *
 * Persistence strategy:
 * - refreshToken stored in httpOnly cookies (server-managed) — frontend never touches it
 * - accessToken stored in React state (memory) only — never persisted
 * - On mount: call /auth/refresh with no body (browser sends cookie automatically)
 * - `initialized` is false until the restore attempt completes; prevents a flash of
 *   the login page on reload for authenticated users
 *
 * Multi-tab sync (Phase 1):
 * - BroadcastChannel('auth') broadcasts state changes to other tabs
 * - On login/logout/refresh, sends { type, accessToken, user }
 * - Other tabs receive and update in-memory state without calling backend
 * - If another tab is already restoring, wait for its broadcast instead of calling refresh
 */
export { AuthContext } from './createAuthContext';

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(null);
  const [user, setUser]               = useState(null);
  const [initialized, setInitialized] = useState(false);
  const restoring = useRef(false); // guard against StrictMode double-invoke
  const broadcastChannel = useRef(null);
  const isRestoringRef = useRef(false); // track if we're in restore phase

  // Phase 1: Setup BroadcastChannel for multi-tab sync
  useEffect(() => {
    try {
      broadcastChannel.current = new BroadcastChannel('auth');
      broadcastChannel.current.onmessage = (event) => {
        const { type, accessToken: newAccessToken, user: newUser } = event.data;

        if (type === 'AUTH_INITIALIZED' || type === 'AUTH_LOGIN' || type === 'AUTH_REFRESH') {
          // Another tab has successfully authenticated — sync state
          setAccessToken(newAccessToken);
          setUser(newUser);
          if (!initialized) {
            setInitialized(true);
          }
        } else if (type === 'AUTH_LOGOUT') {
          // Another tab logged out — clear this tab's state
          setAccessToken(null);
          setUser(null);
        }
      };
    } catch (err) {
      // BroadcastChannel not supported in this browser/context
      console.warn('BroadcastChannel not available:', err.message);
    }

    return () => {
      if (broadcastChannel.current) {
        broadcastChannel.current.close();
      }
    };
  }, [initialized]);

  useEffect(() => {
    if (restoring.current) return;
    restoring.current = true;

    // Phase 1: Check if another tab is already restoring
    // If so, wait for its broadcast instead of calling refresh
    const checkAndRestore = async () => {
      isRestoringRef.current = true;

      // Small delay to allow other tabs to broadcast if they're restoring
      await new Promise(resolve => setTimeout(resolve, 100));

      // If we already got state from another tab via broadcast, we're done
      if (accessToken) {
        setInitialized(true);
        isRestoringRef.current = false;
        return;
      }

      // Otherwise, call refresh (Phase 2: no refreshToken param, browser sends cookie)
      try {
        const data = await authService.refresh();
        setAccessToken(data.accessToken);
        setUser(data.user);

        // Phase 1: Broadcast to other tabs
        if (broadcastChannel.current) {
          broadcastChannel.current.postMessage({
            type: 'AUTH_INITIALIZED',
            accessToken: data.accessToken,
            user: data.user,
          });
        }
      } catch (err) {
        // No stored session or refresh failed — user is logged out
        // Nothing to clear since Phase 2 uses cookies
      } finally {
        setInitialized(true);
        isRestoringRef.current = false;
      }
    };

    checkAndRestore();
  }, [accessToken]);

  const login = useCallback((token, userData) => {
    setAccessToken(token);
    setUser(userData);

    // Phase 1: Broadcast to other tabs
    if (broadcastChannel.current) {
      broadcastChannel.current.postMessage({
        type: 'AUTH_LOGIN',
        accessToken: token,
        user: userData,
      });
    }
  }, []);

  const logout = useCallback(() => {
    // Fire-and-forget backend logout to revoke refresh tokens server-side
    if (accessToken) {
      authService.logout(accessToken).catch(() => {});
    }
    setAccessToken(null);
    setUser(null);

    // Phase 1: Broadcast logout to other tabs
    if (broadcastChannel.current) {
      broadcastChannel.current.postMessage({
        type: 'AUTH_LOGOUT',
      });
    }
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
