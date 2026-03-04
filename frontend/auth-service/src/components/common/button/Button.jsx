import styles from './Button.module.css';

export default function Button({ children, variant = 'primary', loading = false, ...props }) {
  return (
    <button
      className={`${styles.btn} ${styles[variant]}`}
      disabled={loading || props.disabled}
      {...props}
    >
      {loading ? <span className={styles.spinner} /> : children}
    </button>
  );
}
