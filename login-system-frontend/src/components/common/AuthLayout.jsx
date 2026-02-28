import styles from './AuthLayout.module.css';

/**
 * Full-page centred card wrapper used by all auth pages.
 * Provides the silver background + white card visual.
 */
export default function AuthLayout({ children }) {
  return (
    <div className={styles.page}>
      <div className={styles.card}>
        {children}
      </div>
    </div>
  );
}
