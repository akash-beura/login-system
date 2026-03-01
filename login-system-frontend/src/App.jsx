import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import ThemeToggle from './components/common/ThemeToggle';
import ProtectedRoute from './routes/ProtectedRoute';
import { useAuth } from './hooks/useAuth';
import LoginPage            from './pages/LoginPage';
import RegisterPage         from './pages/RegisterPage';
import SetPasswordPage      from './pages/SetPasswordPage';
import SetPasswordPromptPage from './pages/SetPasswordPromptPage';
import OAuthCallbackPage    from './pages/OAuthCallbackPage';
import HomePage             from './pages/HomePage';

function NotFoundRedirect() {
  const { isAuthenticated } = useAuth();
  return <Navigate to={isAuthenticated ? '/homepage' : '/login'} replace />;
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public routes */}
          <Route path="/login"               element={<LoginPage />} />
          <Route path="/register"            element={<RegisterPage />} />
          <Route path="/set-password-prompt" element={<SetPasswordPromptPage />} />

          {/*
           * OAuth callback — backend redirects here with ?code=<opaque>
           * Both /homepage and /set-password share the same callback path;
           * OAuthCallbackPage reads the code, exchanges it, then navigates.
           */}
          <Route path="/oauth/callback"      element={<OAuthCallbackPage />} />

          {/* Semi-protected: requires a token in context (set by OAuthCallbackPage) */}
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

          {/* Default redirect */}
          <Route path="/"  element={<Navigate to="/login" replace />} />
          <Route path="*"  element={<NotFoundRedirect />} />
        </Routes>
      </BrowserRouter>
      </AuthProvider>
      <ThemeToggle />
    </ThemeProvider>
  );
}
