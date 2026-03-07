import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import authService from '../../services/auth-service/authService';
import AuthLayout from '../../components/auth-layout/AuthLayout';
import styles from './OAuthCallbackPage.module.css';

export default function OAuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuth();
  const exchanged = useRef(false);

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
        login(data.accessToken, data.sessionToken, data.user);
        navigate(data.requiresPasswordSet ? '/set-password' : '/homepage');
      })
      .catch(() => navigate('/login?error=oauth_failed'));
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
