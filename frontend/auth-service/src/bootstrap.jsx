/**
 * Standalone entry for auth-remote (port 3001).
 * NOTE: Auth pages import useAuth from shell/useAuth (MF remote).
 * The shell (port 3000) must be running for the pages to work.
 * Standalone mode only renders a dev-mode notice.
 */
import React from 'react';
import ReactDOM from 'react-dom/client';
import './styles/global.css';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      height: '100vh', flexDirection: 'column', gap: '16px',
      fontFamily: 'system-ui, sans-serif', color: '#65676b', textAlign: 'center',
      padding: '24px'
    }}>
      <h2 style={{ color: '#1877f2', fontSize: '1.5rem' }}>auth-remote (port 3001)</h2>
      <p>This is the auth microfrontend. Auth pages are consumed by the shell.</p>
      <p>Start the shell at <strong>http://localhost:3000</strong> to use the full app.</p>
      <p style={{ fontSize: '0.875rem', color: '#9ca3af' }}>
        remoteEntry.js is served at{' '}
        <code style={{ background: '#f0f2f5', padding: '2px 6px', borderRadius: '4px' }}>
          http://localhost:3001/remoteEntry.js
        </code>
      </p>
    </div>
  </React.StrictMode>
);
