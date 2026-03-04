import { createContext } from 'react';

/**
 * Singleton AuthContext — created once and shared across the entire app.
 * This module is imported by both AuthProvider and useAuth to ensure
 * they use the SAME context object (no webpack module duplication).
 */
export const AuthContext = createContext(null);
