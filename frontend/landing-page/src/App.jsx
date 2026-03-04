import React, { Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import ThemeToggle from './components/common/ThemeToggle';
import Navbar from './components/common/Navbar';
import ErrorBoundary from './components/common/ErrorBoundary';
import ProtectedRoute from './routes/ProtectedRoute';
import { useAuth } from './hooks/useAuth';
import HomePage from './pages/HomePage';

// Lazy-load auth pages from auth-service microfrontend
const LoginPage             = React.lazy(() => import('authService/LoginPage'));
const RegisterPage          = React.lazy(() => import('authService/RegisterPage'));
const OAuthCallbackPage     = React.lazy(() => import('authService/OAuthCallbackPage'));
const SetPasswordPage       = React.lazy(() => import('authService/SetPasswordPage'));
const SetPasswordPromptPage = React.lazy(() => import('authService/SetPasswordPromptPage'));

// Lazy-load account settings from account-settings microfrontend
const AccountSettingsPage   = React.lazy(() => import('accountSettings/AccountSettingsPage'));

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
              {/* Public routes — served from auth-service */}
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
