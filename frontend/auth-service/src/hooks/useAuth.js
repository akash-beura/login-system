// Re-export useAuth from the landing-page singleton.
// When auth-service pages are hosted inside landing-page, this resolves to the
// landing-page's AuthContext — ensuring login/logout updates the landing-page's auth state.
export { useAuth } from 'landingPage/useAuth';
