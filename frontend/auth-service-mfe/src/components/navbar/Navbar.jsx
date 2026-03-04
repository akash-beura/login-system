import { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import styles from './Navbar.module.css';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const menuRef = useRef(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(e) {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  function handleLogout() {
    setOpen(false);
    logout();
    navigate('/login');
  }

  function handleSettings() {
    setOpen(false);
    navigate('/account-settings');
  }

  const initials = user?.name
    ? user.name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : user?.email?.[0]?.toUpperCase() ?? '?';

  const isGoogle = user?.provider === 'GOOGLE';

  return (
    <nav className={styles.navbar}>
      <Link to="/homepage" className={styles.logo}>
        LoginSystem
      </Link>

      <div className={styles.userMenu} ref={menuRef}>
        <button
          className={styles.menuTrigger}
          onClick={() => setOpen((o) => !o)}
          aria-haspopup="true"
          aria-expanded={open}
          aria-label="User menu"
        >
          {isGoogle && user.pictureUrl ? (
            <img
              src={user.pictureUrl}
              alt={user.name ?? 'Profile'}
              className={styles.avatar}
              referrerPolicy="no-referrer"
            />
          ) : isGoogle ? (
            <span className={styles.avatarFallback}>{initials}</span>
          ) : (
            <div className={styles.hamburger} aria-hidden="true">
              <span />
              <span />
              <span />
            </div>
          )}
        </button>

        {open && (
          <div className={styles.dropdown} role="menu">
            <div className={styles.dropdownHeader}>
              <div className={styles.dropdownName}>{user?.name ?? 'User'}</div>
              <div className={styles.dropdownEmail}>{user?.email}</div>
            </div>

            <button
              className={styles.dropdownItem}
              onClick={handleSettings}
              role="menuitem"
            >
              Account Settings
            </button>

            <div className={styles.dropdownDivider} />

            <button
              className={`${styles.dropdownItem} ${styles.signOutItem}`}
              onClick={handleLogout}
              role="menuitem"
            >
              Sign out
            </button>
          </div>
        )}
      </div>
    </nav>
  );
}
