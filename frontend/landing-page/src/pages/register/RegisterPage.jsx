import { useState } from 'react';
import { Link, useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import authService from '../../services/authService';
import AuthLayout from '../../components/common/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import ErrorBanner from '../../components/common/ErrorBanner';
import styles from '../../styles/AuthPage.module.css';
import regStyles from './RegisterPage.module.css';

const GOOGLE_OAUTH_URL = `${process.env.REACT_APP_API_URL?.replace('/api/v1', '')}/oauth2/authorization/google`;

const PHONE_CODES = [
  { code: "+1", name: "United States/Canada" }, { code: "+7", name: "Russia/Kazakhstan" },
  { code: "+20", name: "Egypt" }, { code: "+27", name: "South Africa" },
  { code: "+30", name: "Greece" }, { code: "+31", name: "Netherlands" },
  { code: "+32", name: "Belgium" }, { code: "+33", name: "France" },
  { code: "+34", name: "Spain" }, { code: "+36", name: "Hungary" },
  { code: "+39", name: "Italy" }, { code: "+40", name: "Romania" },
  { code: "+41", name: "Switzerland" }, { code: "+43", name: "Austria" },
  { code: "+44", name: "United Kingdom" }, { code: "+45", name: "Denmark" },
  { code: "+46", name: "Sweden" }, { code: "+47", name: "Norway" },
  { code: "+48", name: "Poland" }, { code: "+49", name: "Germany" },
  { code: "+52", name: "Mexico" }, { code: "+54", name: "Argentina" },
  { code: "+55", name: "Brazil" }, { code: "+56", name: "Chile" },
  { code: "+57", name: "Colombia" }, { code: "+60", name: "Malaysia" },
  { code: "+61", name: "Australia" }, { code: "+62", name: "Indonesia" },
  { code: "+63", name: "Philippines" }, { code: "+64", name: "New Zealand" },
  { code: "+65", name: "Singapore" }, { code: "+66", name: "Thailand" },
  { code: "+81", name: "Japan" }, { code: "+82", name: "South Korea" },
  { code: "+84", name: "Vietnam" }, { code: "+86", name: "China" },
  { code: "+90", name: "Turkey" }, { code: "+91", name: "India" },
  { code: "+92", name: "Pakistan" }, { code: "+94", name: "Sri Lanka" },
  { code: "+234", name: "Nigeria" }, { code: "+254", name: "Kenya" },
  { code: "+351", name: "Portugal" }, { code: "+353", name: "Ireland" },
  { code: "+380", name: "Ukraine" }, { code: "+420", name: "Czech Republic" },
  { code: "+880", name: "Bangladesh" }, { code: "+966", name: "Saudi Arabia" },
  { code: "+971", name: "United Arab Emirates" }, { code: "+972", name: "Israel" },
];

const COUNTRIES = [
  "Afghanistan", "Albania", "Algeria", "Argentina", "Armenia", "Australia", "Austria",
  "Azerbaijan", "Bangladesh", "Belgium", "Brazil", "Bulgaria", "Canada", "Chile",
  "China", "Colombia", "Croatia", "Czech Republic", "Denmark", "Egypt", "Ethiopia",
  "Finland", "France", "Germany", "Ghana", "Greece", "Hungary", "India", "Indonesia",
  "Iran", "Iraq", "Ireland", "Israel", "Italy", "Japan", "Jordan", "Kazakhstan",
  "Kenya", "Lebanon", "Malaysia", "Mexico", "Morocco", "Netherlands", "New Zealand",
  "Nigeria", "Norway", "Pakistan", "Peru", "Philippines", "Poland", "Portugal",
  "Romania", "Russia", "Saudi Arabia", "Serbia", "Singapore", "Slovakia", "Slovenia",
  "South Africa", "South Korea", "Spain", "Sri Lanka", "Sweden", "Switzerland",
  "Taiwan", "Tanzania", "Thailand", "Tunisia", "Turkey", "Ukraine",
  "United Arab Emirates", "United Kingdom", "United States", "Uruguay",
  "Uzbekistan", "Venezuela", "Vietnam", "Yemen", "Zambia", "Zimbabwe",
];

export default function RegisterPage() {
  const navigate = useNavigate();
  const { login, isAuthenticated } = useAuth();
  const [form, setForm] = useState({
    name: '', email: '', password: '', confirmPassword: '',
    country: '', addressLine1: '', city: '', state: '', zipCode: '',
    phoneCountryCode: '+91', phoneNumber: '',
  });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) return <Navigate to="/homepage" replace />;

  const handleChange = (e) => {
    const { name, value } = e.target;
    const sanitized = name === 'phoneNumber' ? value.replace(/\D/g, '').slice(0, 10) : value;
    setForm((prev) => ({ ...prev, [name]: sanitized }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: '' }));
  };

  const validate = () => {
    const next = {};
    if (!form.name.trim()) next.name = 'Name is required';
    if (!form.email) next.email = 'Email is required';
    else if (!/\S+@\S+\.\S+/.test(form.email)) next.email = 'Enter a valid email';
    if (!form.password) next.password = 'Password is required';
    else if (form.password.length < 12) next.password = 'Password must be at least 12 characters';
    if (!form.confirmPassword) next.confirmPassword = 'Please confirm your password';
    else if (form.confirmPassword !== form.password) next.confirmPassword = 'Passwords do not match';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setLoading(true);
    setApiError('');
    try {
      const { confirmPassword: _, ...submitData } = form;
      const data = await authService.register(submitData);
      login(data.accessToken, data.user, data.refreshToken);
      navigate('/homepage');
    } catch (err) {
      setApiError(err.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout cardClassName={regStyles.wideCard} pageClassName={regStyles.scrollPage}>
      <div className={styles.header}>
        <h1 className={styles.title}>Create account</h1>
        <p className={styles.subtitle}>It's quick and easy.</p>
      </div>

      <form onSubmit={handleSubmit} className={`${styles.form} ${regStyles.form}`} noValidate>
        <ErrorBanner message={apiError} />

        <Input id="name" name="name" type="text" label="Full name" placeholder="Jane Smith"
          value={form.name} onChange={handleChange} error={errors.name} autoComplete="name" />
        <Input id="email" name="email" type="email" label="Email" placeholder="you@example.com"
          value={form.email} onChange={handleChange} error={errors.email} autoComplete="email" />
        <Input id="password" name="password" type="password" label="Password" placeholder="Min. 12 characters"
          value={form.password} onChange={handleChange} error={errors.password} autoComplete="new-password" />
        <Input id="confirmPassword" name="confirmPassword" type="password" label="Confirm password" placeholder="Re-enter your password"
          value={form.confirmPassword} onChange={handleChange} error={errors.confirmPassword} autoComplete="new-password" />

        <div className={regStyles.selectField}>
          <label className={regStyles.selectLabel} htmlFor="country">Country</label>
          <select id="country" name="country" className={regStyles.countrySelect} value={form.country} onChange={handleChange}>
            <option value="">Select country</option>
            {COUNTRIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>

        <p className={regStyles.sectionLabel}>Address (optional)</p>

        <Input id="addressLine1" name="addressLine1" type="text" label="Address" placeholder="Flat 4B, Andheri West"
          value={form.addressLine1} onChange={handleChange} autoComplete="street-address" />

        <div className={regStyles.twoCol}>
          <Input id="city" name="city" type="text" label="City" placeholder="Mumbai"
            value={form.city} onChange={handleChange} autoComplete="address-level2" />
          <Input id="state" name="state" type="text" label="State" placeholder="Maharashtra"
            value={form.state} onChange={handleChange} autoComplete="address-level1" />
        </div>

        <Input id="zipCode" name="zipCode" type="text" label="PIN code" placeholder="400053"
          value={form.zipCode} onChange={handleChange} autoComplete="postal-code" />

        <div className={regStyles.phoneRow}>
          <div className={regStyles.selectField}>
            <label className={regStyles.selectLabel} htmlFor="phoneCountryCode">Country code</label>
            <select id="phoneCountryCode" name="phoneCountryCode" className={regStyles.phoneSelect}
              value={form.phoneCountryCode} onChange={handleChange}>
              {PHONE_CODES.map(({ code, name }) => (
                <option key={code} value={code}>{code} {name}</option>
              ))}
            </select>
          </div>
          <div className={regStyles.phoneNumberWrapper}>
            <Input id="phoneNumber" name="phoneNumber" type="tel" label="Phone number (optional)"
              placeholder="98765 43210" value={form.phoneNumber} onChange={handleChange}
              autoComplete="tel-national" maxLength={10} inputMode="numeric" />
          </div>
        </div>

        <Button type="submit" loading={loading}>Create account</Button>

        <div className={styles.divider}><span>or</span></div>

        <a href={GOOGLE_OAUTH_URL}>
          <Button type="button" variant="google"><GoogleIcon />Sign up with Google</Button>
        </a>
      </form>

      <p className={styles.footer}>Already have an account? <Link to="/login">Sign in</Link></p>
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
