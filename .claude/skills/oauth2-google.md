# Skill: Google OAuth2

## Approach
- Use spring-boot-starter-oauth2-client
- Configure Google clientId and clientSecret
- Map OAuth2User to internal User entity
- If new:
  - Create user
  - password = null
  - provider = GOOGLE
- Issue JWT after OAuth success