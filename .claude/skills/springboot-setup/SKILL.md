# Skill: Spring Boot Setup

## Current Stack (LIVE)
- Spring Boot: 3.2.5
- Java: 17
- Build: Maven (jar packaging)
- Backend path: `login-system-backend/`

## Core Dependencies (pom.xml)
- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-client`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`
- `jjwt-api` + `jjwt-impl` + `jjwt-jackson`
- `spring-boot-starter-data-redis`
- `lombok`
- `postgresql` (runtime)
- `h2` (test/dev scope)

## Profiles
- `dev`: H2 in-memory DB, `ddl-auto: create-drop`
- `prod`: PostgreSQL, `ddl-auto: validate` (overridden to `update` via Docker env var)

## Package Structure (`com.akash.loginsystem`)
```
config/       AppProperties, AuthBeanConfig, CorsConfig, SecurityConfig, WebConfig
controller/   AuthController, UserController
dto/
  request/    LoginRequest, RegisterRequest, OAuthCodeRequest, SetPasswordRequest, UpdateProfileRequest
  response/   AuthResponse, UserSummaryResponse
entity/       User, RefreshToken
exception/    GlobalExceptionHandler + 6 custom exceptions
model/        AuthProvider (LOCAL, GOOGLE), Role (USER)
repository/   UserRepository, RefreshTokenRepository
security/     JwtAuthFilter, JwtProvider, CustomUserDetailsService,
              OAuth2UserService, OAuth2SuccessHandler, OAuth2UserPrincipal, OAuthTokenStore
```

## Key Config Classes
- `AuthBeanConfig`: defines `PasswordEncoder`, `DaoAuthenticationProvider`, `AuthenticationManager` (extracted to break circular dependency)
- `SecurityConfig`: filter chain, OAuth2, stateless session policy
- `CorsConfig`: `allowCredentials=true`, configured origins
