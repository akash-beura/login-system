import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import authService from '../services/authService';
import AuthLayout from '../components/common/AuthLayout';
import Input from '../components/common/Input';
import Button from '../components/common/Button';
import ErrorBanner from '../components/common/ErrorBanner';
import styles from './AuthPage.module.css';

/**
 * Account-linking flow — shown to OAuth users on their first sign-in.
 * The user already has a valid accessToken in AuthContext (from OAuthCallbackPage).
 * On success, tokens are refreshed and the user is sent to /homepage.
 */
export default function SetPasswordPage() {
  const navigate = useNavigate();
  const { accessToken, login } = useAuth();

  const [form, setForm] = useState({ password: '', confirmPassword: '' });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: '' }));
  };

  const validate = () => {
    const next = {};
    if (!form.password) next.password = 'Password is required';
    else if (form.password.length < 8) next.password = 'Password must be at least 8 characters';
    if (!form.confirmPassword) next.confirmPassword = 'Please confirm your password';
    else if (form.password !== form.confirmPassword) next.confirmPassword = 'Passwords do not match';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    setApiError('');

    try {
      const data = await authService.setPassword(
        form.password,
        form.confirmPassword,
        accessToken
      );
      login(data.accessToken, data.user);
      navigate('/homepage');
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to set password. Please try again.';
      setApiError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout>
      <div className={styles.header}>
        <h1 className={styles.title}>Set your password</h1>
        <p className={styles.subtitle}>
          You signed in with Google. Set a password to also log in with email.
        </p>
      </div>

      <form onSubmit={handleSubmit} className={styles.form} noValidate>
        <ErrorBanner message={apiError} />

        <Input
          id="password"
          name="password"
          type="password"
          label="New password"
          placeholder="Min. 8 characters"
          value={form.password}
          onChange={handleChange}
          error={errors.password}
          autoComplete="new-password"
        />

        <Input
          id="confirmPassword"
          name="confirmPassword"
          type="password"
          label="Confirm password"
          placeholder="••••••••"
          value={form.confirmPassword}
          onChange={handleChange}
          error={errors.confirmPassword}
          autoComplete="new-password"
        />

        <Button type="submit" loading={loading}>
          Set password &amp; continue
        </Button>
      </form>
    </AuthLayout>
  );
}
