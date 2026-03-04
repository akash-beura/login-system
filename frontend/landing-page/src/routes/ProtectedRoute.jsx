import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

/**
 * Wraps routes that require authentication.
 * Waits for session restore (initialized) before deciding — prevents
 * a redirect to /login while a stored refresh token is being validated.
 */
export default function ProtectedRoute({ children }) {
  const { isAuthenticated, initialized } = useAuth();
  const location = useLocation();

  if (!initialized) return null;

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}
