# Running & Testing the Login System

## Prerequisites
- Docker Desktop running
- Java 17 + Maven installed (for local dev only)
- Google OAuth2 credentials (Client ID + Secret)

---

## First-Time Setup

### 1. Create your `.env` file
```bash
cp .env.example .env
```

Fill in the 4 required values:
```env
DB_PASSWORD=anything_you_want
JWT_SECRET=<output of: openssl rand -base64 48>
GOOGLE_CLIENT_ID=<from Google Cloud Console>
GOOGLE_CLIENT_SECRET=<from Google Cloud Console>
```

### 2. Add the OAuth2 redirect URI in Google Cloud Console
Go to **APIs & Services → Credentials → your OAuth2 client** and add:
```
http://localhost:8080/login/oauth2/code/google
```

### 3. Build the backend JAR
```bash
cd login-system-backend
mvn clean install -DskipTests
cd ..
```
> Re-run this step any time you change backend source code.

---

## Starting the Application

```bash
docker compose up --build
```

All three containers start in order: **postgres → backend → frontend**.

| Service    | URL                          |
|------------|------------------------------|
| Frontend   | http://localhost:3000        |
| Backend    | http://localhost:8080        |
| Health     | http://localhost:8080/actuator/health |

Wait for:
```
loginsystem-backend | Started LoginSystemApplication in X seconds
loginsystem-frontend | Configuration complete; ready for start up
```

---

## Stopping the Application

```bash
docker compose down
```

> PostgreSQL data is persisted in a Docker volume (`postgres_data`). To also wipe the database:
> ```bash
> docker compose down -v
> ```

---

## Subsequent Starts (no code changes)

```bash
docker compose up
```
No `--build` needed unless you changed source code.

---

## Testing

### Via Browser
Open http://localhost:3000 and test the full UI flow:

| Flow | Steps |
|---|---|
| Register | Click Register → fill name/email/password → submit |
| Login | Enter credentials → lands on `/homepage` |
| Google OAuth | Click "Continue with Google" → Google consent → lands on `/homepage` |
| Account Linking | Google-only user tries email login → redirected to `/set-password-prompt` → re-auth via Google → set password |
| Logout | Click Logout on HomePage → redirected to `/login` |

### Via curl (API only)

**Register**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"password123"}' | jq
```

**Login**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' | jq
```

**Access protected endpoint** (use `accessToken` from login response)
```bash
curl -s http://localhost:8080/api/v1/auth/set-password \
  -H "Authorization: Bearer <accessToken>"
```

**Refresh token**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}' | jq
```

---

## After Backend Code Changes

```bash
cd login-system-backend
mvn clean install -DskipTests
cd ..
docker compose up --build
```

## After Frontend Code Changes

```bash
docker compose up --build
```
(npm build runs inside Docker automatically)
