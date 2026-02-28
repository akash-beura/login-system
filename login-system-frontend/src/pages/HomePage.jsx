import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import styles from './HomePage.module.css';

export default function HomePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.badge}>🚧</div>
        <h1 className={styles.title}>Under Maintenance</h1>
        <p className={styles.subtitle}>
          We're working on something great. Check back soon.
        </p>
        {user && (
          <p className={styles.greeting}>
            Signed in as <strong>{user.email}</strong>
          </p>
        )}
        <button className={styles.logoutBtn} onClick={handleLogout}>
          Sign out
        </button>
      </div>
    </div>
  );
}
