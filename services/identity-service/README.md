# identity-service

Users, credentials and RS256 JWT issuing (replaces v1 auth-service + account-service). Port 8001.

- `POST /api/v1/auth/register` (role is always USER), `POST /api/v1/auth/login`, `GET /.well-known/jwks.json`
- `GET|PUT /api/v1/users/me`, `GET|DELETE /api/v1/users/{id}` (self or ADMIN), `GET /api/v1/users` (ADMIN, paged)
- OpenAPI: `/v3/api-docs`, `/swagger-ui.html`

**Dev admin**: migration `V2__seed_admin.sql` seeds `admin` / `admin` (dev only - change before real use).

**Signing keys**: set `JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION` (PKCS#8 / X.509 PEM). Profile `dev`
uses a DEV-ONLY pair from `.local/keys/` (run `make dev-keys` at the repo root; git-ignored). With nothing configured an ephemeral
key pair is generated at startup (WARN logged). Token TTL: `JWT_TTL` (default `15m`).
