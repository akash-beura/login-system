// Module Federation requires an async entry point so shared modules
// (react, react-dom, react-router-dom) are loaded before the app bootstraps.
import('./bootstrap');
