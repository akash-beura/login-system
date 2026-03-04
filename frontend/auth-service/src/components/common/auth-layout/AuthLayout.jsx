import styles from './AuthLayout.module.css';

export default function AuthLayout({ children, cardClassName = '', pageClassName = '' }) {
  return (
    <div className={`${styles.page} ${pageClassName}`}>
      <div className={`${styles.card} ${cardClassName}`}>
        {children}
      </div>
    </div>
  );
}
