import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import authService from '../services/authService';
import AuthLayout from '../components/common/AuthLayout';
import styles from './OAuthCallbackPage.module.css';

/**
 * Handles the OAuth2 redirect from the backend.
 *
 * Flow (ISSUE-1 fix applied):
 * 1. Backend redirects to /oauth/callback?code=<opaque>
 * 2. This page exchanges the code for AuthResponse via POST /auth/oauth2/token
 * 3. If requiresPasswordSet=true  → navigate to /set-password (token stored in context)
 * 4. Otherwise                    → navigate to /homepage
 */
export default function OAuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuth();
  const exchanged = useRef(false); // prevent double-exchange in StrictMode

  useEffect(() => {
    if (exchanged.current) return;
    exchanged.current = true;

    const code = searchParams.get('code');

    if (!code) {
      navigate('/login?error=oauth_failed');
      return;
    }

    authService.exchangeOAuthCode(code)
      .then((data) => {
        login(data.accessToken, data.user, data.refreshToken);
        if (data.requiresPasswordSet) {
          navigate('/set-password');
        } else {
          navigate('/homepage');
        }
      })
      .catch(() => {
        navigate('/login?error=oauth_failed');
      });
  }, [searchParams, login, navigate]);

  return (
    <AuthLayout>
      <div className={styles.container}>
        <div className={styles.spinner} />
        <p className={styles.text}>Completing sign-in…</p>
      </div>
    </AuthLayout>
  );
}
