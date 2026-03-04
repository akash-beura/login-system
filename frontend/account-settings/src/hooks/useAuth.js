// Re-export useAuth from the landing-page singleton — ensures this remote
// reads and writes the same auth state as the landing-page.
export { useAuth } from 'landingPage/useAuth';
