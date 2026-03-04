import React, { Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import ThemeToggle from './components/theme-toggle/ThemeToggle';
import Navbar from './components/navbar/Navbar';
import ErrorBoundary from './components/error-boundary/ErrorBoundary';
import ProtectedRoute from './routes/ProtectedRoute';
import { useAuth } from './hooks/useAuth';
import HomePage from './pages/HomePage';

// Import auth pages locally
const LoginPage             = React.lazy(() => import('./pages/login/LoginPage'));
const RegisterPage          = React.lazy(() => import('./pages/register/RegisterPage'));
const OAuthCallbackPage     = React.lazy(() => import('./pages/oauth-callback/OAuthCallbackPage'));
const SetPasswordPage       = React.lazy(() => import('./pages/set-password/SetPasswordPage'));
const SetPasswordPromptPage = React.lazy(() => import('./pages/set-password-prompt/SetPasswordPromptPage'));

// Import account settings page locally
const AccountSettingsPage   = React.lazy(() => import('./pages/account-settings/AccountSettingsPage'));

function NotFoundRedirect() {
  const { isAuthenticated } = useAuth();
  return <Navigate to={isAuthenticated ? '/homepage' : '/login'} replace />;
}

/** Renders Navbar only for authenticated pages */
function AppNavbar() {
  const { isAuthenticated } = useAuth();
  const { pathname } = useLocation();

  const authPaths = ['/login', '/register', '/oauth/callback', '/set-password', '/set-password-prompt'];
  const isAuthPage = authPaths.some((p) => pathname.startsWith(p));

  if (!isAuthenticated || isAuthPage) return null;
  return <Navbar />;
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <AppNavbar />
          <ErrorBoundary>
          <Suspense fallback={<div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>Loading…</div>}>
            <Routes>
              {/* Public routes */}
              <Route path="/login"               element={<LoginPage />} />
              <Route path="/register"            element={<RegisterPage />} />
              <Route path="/set-password-prompt" element={<SetPasswordPromptPage />} />
              <Route path="/oauth/callback"      element={<OAuthCallbackPage />} />

              {/* Semi-protected: requires token set by OAuthCallbackPage */}
              <Route
                path="/set-password"
                element={
                  <ProtectedRoute>
                    <SetPasswordPage />
                  </ProtectedRoute>
                }
              />

              {/* Protected routes */}
              <Route
                path="/homepage"
                element={
                  <ProtectedRoute>
                    <HomePage />
                  </ProtectedRoute>
                }
              />

              <Route
                path="/account-settings"
                element={
                  <ProtectedRoute>
                    <AccountSettingsPage />
                  </ProtectedRoute>
                }
              />

              {/* Default redirect */}
              <Route path="/"  element={<Navigate to="/login" replace />} />
              <Route path="*"  element={<NotFoundRedirect />} />
            </Routes>
          </Suspense>
          </ErrorBoundary>
        </BrowserRouter>
      </AuthProvider>
      <ThemeToggle />
    </ThemeProvider>
  );
}
