import { useState, useEffect } from 'react';
import { useAuth } from '../../hooks/useAuth';
import userService from '../../services/user-service/userService';
import Input from '../../components/input/Input';
import Button from '../../components/button/Button';
import ErrorBanner from '../../components/error-banner/ErrorBanner';
import styles from './AccountSettingsPage.module.css';

export default function AccountSettingsPage() {
  const { user, accessToken } = useAuth();
  const [form, setForm] = useState({
    name: '', phoneCountryCode: '+91', phoneNumber: '',
    addressLine1: '', city: '', state: '', zipCode: '', country: '',
  });
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [apiError, setApiError] = useState('');
  const [success, setSuccess] = useState(false);

  // Load current profile on mount
  useEffect(() => {
    if (!accessToken) return;
    setLoading(true);
    userService.getMe(accessToken)
      .then((data) => {
        setForm({
          name:             data.name         ?? '',
          phoneCountryCode: data.phoneCountryCode ?? '+91',
          phoneNumber:      data.phoneNumber   ?? '',
          addressLine1:     data.addressLine1  ?? '',
          city:             data.city          ?? '',
          state:            data.state         ?? '',
          zipCode:          data.zipCode       ?? '',
          country:          data.country       ?? '',
        });
      })
      .catch((err) => {
        console.error('Failed to load profile:', err);
        setApiError('Failed to load profile. Please refresh.');
      })
      .finally(() => setLoading(false));
  }, [accessToken]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    const sanitized = name === 'phoneNumber' ? value.replace(/\D/g, '').slice(0, 10) : value;
    setForm((prev) => ({ ...prev, [name]: sanitized }));
    setSuccess(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name.trim()) { setApiError('Name cannot be empty.'); return; }
    setSaving(true);
    setApiError('');
    setSuccess(false);
    try {
      await userService.updateMe(accessToken, form);
      setSuccess(true);
    } catch (err) {
      setApiError(err.response?.data?.message || 'Failed to save changes. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const initials = user?.name
    ? user.name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : user?.email?.[0]?.toUpperCase() ?? '?';

  const isGoogle = user?.provider === 'GOOGLE';

  if (loading) {
    return (
      <div className={styles.page}>
        <div className={styles.container}>
          <p style={{ color: 'var(--text-secondary)' }}>Loading profile…</p>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <h1 className={styles.heading}>Account Settings</h1>
        <p className={styles.subheading}>Manage your profile and contact information.</p>

        <div className={styles.card}>
          {/* Avatar & identity summary */}
          <div className={styles.avatar}>
            {isGoogle && user.pictureUrl ? (
              <img src={user.pictureUrl} alt={user.name} className={styles.avatarImg} referrerPolicy="no-referrer" />
            ) : (
              <div className={styles.avatarInitials}>{initials}</div>
            )}
            <div className={styles.avatarInfo}>
              <strong>{user?.name}</strong>
              <span>{user?.email}</span>
              <span style={{ marginTop: '2px', fontSize: '0.75rem', opacity: 0.7 }}>
                {isGoogle ? 'Google account' : 'Email account'}
              </span>
            </div>
          </div>

          <form onSubmit={handleSubmit} className={styles.form} noValidate>
            <ErrorBanner message={apiError} />

            {/* Profile section */}
            <div className={styles.section}>
              <p className={styles.sectionTitle}>Profile</p>
              <Input id="name" name="name" type="text" label="Display name"
                placeholder="Your full name" value={form.name} onChange={handleChange} disabled={true} />
            </div>

            {/* Contact section */}
            <div className={styles.section}>
              <p className={styles.sectionTitle}>Phone</p>
              <Input id="phoneNumber" name="phoneNumber" type="tel"
                label="Phone number" placeholder="98765 43210"
                value={form.phoneNumber} onChange={handleChange}
                autoComplete="tel-national" maxLength={10} inputMode="numeric" disabled={true} />
            </div>

            {/* Address section */}
            <div className={styles.section}>
              <p className={styles.sectionTitle}>Address</p>
              <div className={styles.form}>
                <Input id="addressLine1" name="addressLine1" type="text" label="Street address"
                  placeholder="Flat 4B, Andheri West" value={form.addressLine1} onChange={handleChange} />
                <div className={styles.twoCol}>
                  <Input id="city" name="city" type="text" label="City"
                    placeholder="Mumbai" value={form.city} onChange={handleChange} />
                  <Input id="state" name="state" type="text" label="State"
                    placeholder="Maharashtra" value={form.state} onChange={handleChange} />
                </div>
                <div className={styles.twoCol}>
                  <Input id="zipCode" name="zipCode" type="text" label="PIN / ZIP code"
                    placeholder="400053" value={form.zipCode} onChange={handleChange} />
                  <Input id="country" name="country" type="text" label="Country"
                    placeholder="India" value={form.country} onChange={handleChange} />
                </div>
              </div>
            </div>

            <div className={styles.actions}>
              <div className={styles.saveBtn}>
                <Button type="submit" loading={saving}>Save changes</Button>
              </div>
              {success && <span className={styles.successMsg}>Changes saved!</span>}
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
