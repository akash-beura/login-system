import { Link } from 'react-router-dom';
import AuthLayout from '../components/common/AuthLayout';
import Button from '../components/common/Button';
import styles from './AuthPage.module.css';
import promptStyles from './SetPasswordPromptPage.module.css';

const GOOGLE_OAUTH_URL = `${process.env.REACT_APP_API_URL?.replace('/api/v1', '')}/oauth2/authorization/google`;

/**
 * Shown when a Google-registered user tries to log in with email/password
 * before setting a password. Instructs them to use Google OAuth to authenticate
 * and then set a password from the settings page.
 */
export default function SetPasswordPromptPage() {
  return (
    <AuthLayout>
      <div className={styles.header}>
        <h1 className={styles.title}>Password not set</h1>
        <p className={styles.subtitle}>
          This account was created with Google. Please sign in with Google to set a password.
        </p>
      </div>

      <div className={promptStyles.actions}>
        <a href={GOOGLE_OAUTH_URL}>
          <Button type="button" variant="google">
            <GoogleIcon />
            Continue with Google
          </Button>
        </a>

        <Link to="/login">
          <Button type="button" variant="secondary">
            Back to login
          </Button>
        </Link>
      </div>
    </AuthLayout>
  );
}

function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
      <path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615Z" fill="#4285F4"/>
      <path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18Z" fill="#34A853"/>
      <path d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332Z" fill="#FBBC05"/>
      <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58Z" fill="#EA4335"/>
    </svg>
  );
}
