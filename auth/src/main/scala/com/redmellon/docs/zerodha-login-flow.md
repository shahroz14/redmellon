# Zerodha Programmatic Login Flow

The `live-ingest` service authenticates with Zerodha Kite Connect automatically on startup — no browser required after a one-time setup.

## One-time setup

1. Create a Kite Connect app at [kite.trade/api](https://kite.trade/api).
2. Set a redirect URL in the app settings. It can be any valid URL (e.g. `https://www.google.com`) — only used during the one-time authorization below.
3. Copy `application-creds.example.yml` to `application-creds.yml` and fill in your credentials:

   ```yaml
   ingest:
     api-key: YOUR_KITE_API_KEY
     api-secret: YOUR_KITE_API_SECRET
     user-id: YOUR_ZERODHA_CLIENT_ID     # e.g. S1234567
     password: YOUR_ZERODHA_PASSWORD
     totp-secret: YOUR_TOTP_SECRET_KEY   # base32 seed shown during authenticator app setup
   ```

4. **First run only:** Zerodha requires explicit consent the first time an app accesses your account. Open the login URL in a browser and click "Allow":

   ```
   https://kite.zerodha.com/connect/login?v=3&api_key=YOUR_API_KEY
   ```

   After this one-time step, all future logins are fully automatic.

## How it works

`ZerodhaAuthService.login()` performs three HTTP steps and stores the resulting session in `TokenStore`:

### Step 1 — Credentials (`POST /api/login`)

```
POST https://kite.zerodha.com/api/login
Body: user_id=...&password=...
```

Response contains `request_id` (needed for 2FA) and sets `kf_session` + `__cf_bm` cookies.

### Step 2 — TOTP (`POST /api/twofa`)

```
POST https://kite.zerodha.com/api/twofa
Cookie: <login cookies>
Body: user_id=...&request_id=...&twofa_value=<TOTP>&twofa_type=totp&skip_session=True
```

The TOTP is generated from the `totp-secret` (base32 seed → hex → TOTP.getOTP).

The response sets additional cookies including `enctoken`, `public_token`, and an upgraded `kf_session`. All login + 2FA cookies are merged — 2FA values override duplicates.

### Step 3 — OAuth redirect chain (`GET /connect/login`)

```
GET https://kite.zerodha.com/connect/login?v=3&api_key=...
Cookie: <merged cookies>
```

Zerodha redirects through:
```
/connect/finish?sess_id=...
  → /connect/authorize?sess_id=...   (consent screen — auto-skipped after first approval)
    → <your redirect_url>?request_token=...&action=login&status=success
```

`followToRequestToken` follows the chain hop-by-hop (using a no-redirect HTTP client) until `request_token` appears in a Location header.

### Step 4 — Token exchange (`POST /session/token`)

```
POST https://api.kite.trade/session/token
Body: api_key=...&request_token=...&checksum=SHA256(api_key + request_token + api_secret)
```

Returns `access_token` and `public_token`, which are stored in `TokenStore` for use by `ZerodhaClient`.

## Session management

| Component | Role |
|---|---|
| `ZerodhaAuthService` | Performs login, token exchange, and session invalidation |
| `TokenStore` | Thread-safe in-memory holder for the active `ZerodhaSession` |
| `ZerodhaClient` | Reads `access_token` from `TokenStore` for all API/WebSocket calls |
| `MarketDataService` | Calls `authService.login()` on startup (when `ingest.auto-login=true`) |

To disable auto-login (e.g. in tests), set `ingest.auto-login=false`.
