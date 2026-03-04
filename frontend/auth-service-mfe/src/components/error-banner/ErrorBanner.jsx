import styles from './ErrorBanner.module.css';

/** Inline error banner for API-level errors (not field errors). */
export default function ErrorBanner({ message }) {
  if (!message) return null;
  return <div className={styles.banner}>{message}</div>;
}
