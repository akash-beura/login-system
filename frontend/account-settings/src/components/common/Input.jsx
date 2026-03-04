import styles from './Input.module.css';

export default function Input({ label, error, id, ...props }) {
  return (
    <div className={styles.field}>
      {label && <label className={styles.label} htmlFor={id}>{label}</label>}
      <input
        id={id}
        className={`${styles.input} ${error ? styles.inputError : ''}`}
        {...props}
      />
      {error && <span className={styles.error}>{error}</span>}
    </div>
  );
}
