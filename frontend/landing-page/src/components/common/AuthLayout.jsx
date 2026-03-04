import styles from './AuthLayout.module.css';

/**
 * Full-page centred card wrapper used by all auth pages.
 * Accepts optional cardClassName / pageClassName to let individual pages
 * override width or alignment (e.g. wider card on RegisterPage).
 */
export default function AuthLayout({ children, cardClassName = '', pageClassName = '' }) {
  return (
    <div className={`${styles.page} ${pageClassName}`}>
      <div className={`${styles.card} ${cardClassName}`}>
        {children}
      </div>
    </div>
  );
}
