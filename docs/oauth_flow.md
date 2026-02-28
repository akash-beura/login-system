# OAuth Login Code Flow

End-to-end walkthrough of every method that executes when a user clicks **"Continue with Google"**.

---

## Phase 1 — Browser Redirect to Google (Frontend → Spring → Google → Spring)

### Step 1 — User clicks "Continue with Google"
**File:** `LoginPage.jsx` · line 10, 110

```jsx
const GOOGLE_OAUTH_URL = `${process.env.REACT_APP_API_URL?.replace('/api/v1', '')}/oauth2/authorization/google`;

<a href={GOOGLE_OAUTH_URL}>   // simple <a> href — full page navigation, not an API call
  <Button type="button" variant="google">Continue with Google</Button>
</a>
```

The browser navigates to `http://backend/oauth2/authorization/google`. Spring Security intercepts this URL automatically.

---

### Step 2 — Spring Security handles the OAuth2 initiation
**File:** `SecurityConfig.java` · lines 56–58

```java
.oauth2Login(oauth2 -> oauth2
    .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
    .successHandler(oAuth2SuccessHandler)
)
```

Spring's built-in OAuth2 machinery redirects the browser to Google's consent screen. The user approves, and Google redirects back to `/login/oauth2/code/google?code=...` on the backend.

---

### Step 3 — Spring exchanges the Google code, then calls our custom service
**File:** `OAuth2UserService.java` · `loadUser()` — lines 34–58

```java
public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);   // ① Spring fetches profile from Google's userinfo endpoint

    String rawEmail   = oAuth2User.getAttribute("email");
    String name       = oAuth2User.getAttribute("name");
    String providerId = oAuth2User.getAttribute("sub");
    String email      = rawEmail.toLowerCase(Locale.ROOT); // ② normalize email

    User user = userRepository.findByEmail(email)
        .map(existing -> linkGoogleAccount(existing, providerId))    // ③a existing user
        .orElseGet(() -> createGoogleUser(email, name, providerId)); // ③b new user

    return new OAuth2UserPrincipal(user, oAuth2User.getAttributes()); // ④ wrap in our principal
}
```

| Branch | Method | What happens |
|---|---|---|
| **Existing user** | `linkGoogleAccount()` | Sets `providerId` if missing, saves user |
| **New user** | `createGoogleUser()` | Creates user with `password=null`, `passwordSet=false` |

---

### Step 4 — `OAuth2SuccessHandler.onAuthenticationSuccess()` fires
**File:** `OAuth2SuccessHandler.java` · lines 39–63

```java
public void onAuthenticationSuccess(..., Authentication authentication) throws IOException {

    OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal(); // ① get our wrapped user

    AuthResponse authResponse = authService.completeOAuthLogin(principal.getUser()); // ② issue JWT tokens

    String code = oAuthTokenStore.store(authResponse); // ③ stash AuthResponse, get back opaque UUID code (30s TTL)

    String redirectUrl = UriComponentsBuilder
        .fromUriString(appProperties.getFrontendUrl() + "/oauth/callback")
        .queryParam("code", code)   // ④ only the opaque code goes in URL — NOT the JWT
        .build().toUriString();

    getRedirectStrategy().sendRedirect(request, response, redirectUrl); // ⑤ redirect browser to frontend
}
```

---

### Step 5 — `AuthServiceImpl.completeOAuthLogin()` → `issueTokens()`
**File:** `AuthServiceImpl.java` · lines 153–156, 227–247

```java
// completeOAuthLogin() just delegates:
public AuthResponse completeOAuthLogin(User user) {
    return issueTokens(user);
}

// issueTokens() does the real work:
private AuthResponse issueTokens(User user) {
    refreshTokenRepository.deleteByUser(user);         // ① single-session enforcement: revoke any existing token

    String accessToken       = jwtProvider.generateAccessToken(user);   // ② generate JWT
    String refreshTokenValue = jwtProvider.generateRefreshTokenValue();  // ③ generate opaque refresh token

    RefreshToken refreshToken = RefreshToken.builder()
        .token(refreshTokenValue).user(user)
        .expiresAt(Instant.now().plusMillis(appProperties.getJwt().getRefreshExpiryMs()))
        .build();
    refreshTokenRepository.save(refreshToken);         // ④ persist refresh token to DB

    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshTokenValue)
        .requiresPasswordSet(!user.isPasswordSet())    // ⑤ flag for new Google users
        .user(UserResponse.from(user))
        .build();
}
```

---

### Step 6 — `OAuthTokenStore.store()` saves the response
**File:** `OAuthTokenStore.java` · lines 33–37

```java
public String store(AuthResponse response) {
    String code = UUID.randomUUID().toString();
    store.put(code, new Entry(response, Instant.now().plusSeconds(30))); // 30s TTL
    return code;
}
```

The `AuthResponse` (containing JWTs) lives in memory. The browser only ever sees the opaque UUID code.

---

## Phase 2 — Frontend Exchanges the Code for Tokens

### Step 7 — `OAuthCallbackPage.useEffect()` reads the code from the URL
**File:** `OAuthCallbackPage.jsx` · lines 23–46

```jsx
useEffect(() => {
    if (exchanged.current) return;  // prevent double-fire in React StrictMode
    exchanged.current = true;

    const code = searchParams.get('code');    // ① read ?code= from URL
    if (!code) { navigate('/login?error=oauth_failed'); return; }

    authService.exchangeOAuthCode(code)       // ② POST to backend
        .then((data) => {
            login(data.accessToken, data.user);   // ③ store token in React memory
            if (data.requiresPasswordSet) {
                navigate('/set-password');        // ④a new Google user → must set password
            } else {
                navigate('/homepage');            // ④b → go to app
            }
        })
        .catch(() => navigate('/login?error=oauth_failed'));
}, [searchParams, login, navigate]);
```

---

### Step 8 — `authService.exchangeOAuthCode()` makes the API call
**File:** `authService.js` · lines 54–57

```js
exchangeOAuthCode: async (code) => {
    const { data } = await apiClient.post('/auth/oauth2/token', { code });
    return data;
}
```

---

### Step 9 — `AuthController.exchangeOAuthCode()` handles the request
**File:** `AuthController.java` · lines 81–84

```java
@PostMapping("/oauth2/token")
public ResponseEntity<AuthResponse> exchangeOAuthCode(@Valid @RequestBody OAuthCodeRequest request) {
    return ResponseEntity.ok(authService.exchangeOAuthCode(request));
}
```

---

### Step 10 — `AuthServiceImpl.exchangeOAuthCode()` consumes the code
**File:** `AuthServiceImpl.java` · lines 165–168

```java
public AuthResponse exchangeOAuthCode(OAuthCodeRequest request) {
    return oAuthTokenStore.consume(request.getCode())
            .orElseThrow(OAuthCodeExpiredException::new);
}
```

---

### Step 11 — `OAuthTokenStore.consume()` validates and deletes the entry
**File:** `OAuthTokenStore.java` · lines 43–53

```java
public Optional<AuthResponse> consume(String code) {
    Entry entry = store.remove(code);           // remove = single-use guarantee
    if (entry == null) return Optional.empty();
    if (Instant.now().isAfter(entry.expiresAt())) {
        log.warn("OAuth code expired: {}", code);
        return Optional.empty();
    }
    return Optional.of(entry.response());       // return the stored AuthResponse
}
```

---

### Step 12 — `AuthContext.login()` stores the token in React memory
**File:** `AuthContext.jsx` · lines 17–20

```jsx
const login = useCallback((token, userData) => {
    setAccessToken(token);  // in-memory only — never localStorage
    setUser(userData);
}, []);
```

`isAuthenticated` becomes `true`. `ProtectedRoute` now allows navigation to `/homepage`.

---

## Complete Call Chain

```
LoginPage (click "Continue with Google")
    ↓  browser navigates to /oauth2/authorization/google
Spring Security → redirects browser to Google consent screen
    ↓  user approves → Google sends code to /login/oauth2/code/google
OAuth2UserService.loadUser()
    ↓  super.loadUser() → fetches profile from Google userinfo endpoint
    ↓  linkGoogleAccount() or createGoogleUser()
    ↓  returns OAuth2UserPrincipal wrapping internal User entity
OAuth2SuccessHandler.onAuthenticationSuccess()
    ↓  authService.completeOAuthLogin(user) → issueTokens() → JWT + refresh token
    ↓  oAuthTokenStore.store(authResponse) → returns opaque UUID code (30s TTL)
    ↓  redirects browser to /oauth/callback?code=<uuid>
OAuthCallbackPage.useEffect()
    ↓  authService.exchangeOAuthCode(code) → POST /api/v1/auth/oauth2/token
AuthController.exchangeOAuthCode()
    ↓  authService.exchangeOAuthCode() → oAuthTokenStore.consume(code)
    ↓  returns AuthResponse { accessToken, refreshToken, requiresPasswordSet, user }
OAuthCallbackPage (back in frontend)
    ↓  AuthContext.login(accessToken, user) → stored in React state (memory only)
    ↓  navigate('/set-password') if new Google user, else navigate('/homepage')
```

---

## Key Design Decisions

| Decision | Where | Why |
|---|---|---|
| **Opaque code instead of JWT in URL** | `OAuthTokenStore` | Prevents JWT leaking in browser history / server logs (CWE-598 mitigation) |
| **30-second TTL + single-use code** | `OAuthTokenStore.consume()` removes on read | Replay attack prevention |
| **JWT stored in React memory, not localStorage** | `AuthContext.jsx` | XSS protection |
| **`passwordSet=false` flag** | `issueTokens()` → `AuthResponse` | New Google users are issued tokens but routed to `/set-password` first |
| **Single active session** | `issueTokens()` calls `deleteByUser()` first | Only one refresh token per user at any time |
