import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import authService from '../../services/auth-service/authService';
import AuthLayout from '../../components/auth-layout/AuthLayout';
import Input from '../../components/input/Input';
import Button from '../../components/button/Button';
import ErrorBanner from '../../components/error-banner/ErrorBanner';
import styles from '../../styles/AuthPage.module.css';

export default function SetPasswordPage() {
  const navigate = useNavigate();
  const { accessToken, login } = useAuth();
  const [form, setForm] = useState({ password: '', confirmPassword: '', phoneCountryCode: '+91', phoneNumber: '' });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    const sanitized = name === 'phoneNumber' ? value.replace(/\D/g, '').slice(0, 10) : value;
    setForm((prev) => ({ ...prev, [name]: sanitized }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: '' }));
  };

  const validate = () => {
    const next = {};
    if (!form.password) next.password = 'Password is required';
    else if (form.password.length < 12) next.password = 'Password must be at least 12 characters';
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
      const data = await authService.setPassword(form.password, form.confirmPassword, form.phoneCountryCode, form.phoneNumber, accessToken);
      login(data.accessToken, data.sessionToken, data.user);
      navigate('/homepage');
    } catch (err) {
      setApiError(err.response?.data?.message || 'Failed to set password. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout>
      <div className={styles.header}>
        <h1 className={styles.title}>Set your password</h1>
        <p className={styles.subtitle}>You signed in with Google. Set a password to also log in with email.</p>
      </div>
      <form onSubmit={handleSubmit} className={styles.form} noValidate>
        <ErrorBanner message={apiError} />
        <Input id="password" name="password" type="password" label="New password"
          placeholder="Min. 12 characters" value={form.password} onChange={handleChange}
          error={errors.password} autoComplete="new-password" />
        <Input id="confirmPassword" name="confirmPassword" type="password" label="Confirm password"
          placeholder="••••••••" value={form.confirmPassword} onChange={handleChange}
          error={errors.confirmPassword} autoComplete="new-password" />
        <Input id="phoneNumber" name="phoneNumber" type="tel" label="Phone number"
          placeholder="98765 43210" value={form.phoneNumber} onChange={handleChange}
          autoComplete="tel-national" maxLength={10} inputMode="numeric" />
        <Button type="submit" loading={loading}>Set password &amp; continue</Button>
      </form>
    </AuthLayout>
  );
}
