# Swagger UI Security Fix

**Issue:** `HTTP 403 Forbidden` on `/swagger-ui.html` and `/v3/api-docs`  
**Root cause:** Missing URL patterns in Spring Security `permitAll` configuration  
**Status:** Fixed  

---

## Problem Analysis

Spring Security's default policy is `anyRequest().authenticated()` — every URL not explicitly permitted is blocked with HTTP 403.

The original configuration permitted only two Swagger-related patterns:

```java
// BEFORE (incomplete)
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
```

### Missing paths

| Missing Path | Why needed | HTTP status before fix |
|---|---|---|
| `/swagger-ui.html` | Entry-point URL — browser first requests this | **403** |
| `/v3/api-docs` | Exact OpenAPI JSON endpoint (not matched by `/**`) | **403** |
| `/v3/api-docs.yaml` | YAML variant of OpenAPI spec | **403** |
| `/webjars/**` | Swagger UI static assets: JS bundles, CSS, icons | **403** |

### springdoc URL inventory (from `Constants.class`)

springdoc-openapi 2.5.0 serves requests on these paths:

```
/swagger-ui.html                       → redirects to /swagger-ui/index.html
/swagger-ui/index.html                 → Swagger UI HTML shell
/swagger-ui/swagger-initializer.js     → configures the spec URL
/swagger-ui/oauth2-redirect.html       → OAuth2 callback
/swagger-ui/**                         → all other Swagger UI assets

/v3/api-docs                           → OpenAPI JSON spec (EXACT)
/v3/api-docs/**                        → sub-paths (e.g. /v3/api-docs/swagger-config)
/v3/api-docs.yaml                      → OpenAPI YAML spec

/webjars/swagger-ui/5.13.0/*.js        → JavaScript bundles
/webjars/swagger-ui/5.13.0/*.css       → Stylesheets
/webjars/swagger-ui/5.13.0/*.png       → Icons
```

### Additional issue: MVC/Reactive matcher ambiguity

Both `spring-boot-starter-web` (MVC) and `spring-boot-starter-webflux` (used for `WebClient`) are on the classpath. When Spring Security 6 encounters plain string `requestMatchers("/path")` with both starters present, it may throw an `IllegalArgumentException` about ambiguous matcher type. The fix uses explicit `AntPathRequestMatcher` instances to eliminate the ambiguity.

---

## Fix Applied

**File:** `src/main/java/com/bytehr/config/SecurityConfig.java`

### Before

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/messages").permitAll()
    .requestMatchers("/api/sync").permitAll()
    .requestMatchers("/api/chat").permitAll()
    .requestMatchers("/actuator/health").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .anyRequest().authenticated()
);
```

### After

```java
.authorizeHttpRequests(auth -> auth
    // Application endpoints
    .requestMatchers(
        new AntPathRequestMatcher("/api/messages"),
        new AntPathRequestMatcher("/api/sync"),
        new AntPathRequestMatcher("/api/chat")
    ).permitAll()
    // Actuator
    .requestMatchers(
        new AntPathRequestMatcher("/actuator/health"),
        new AntPathRequestMatcher("/actuator/info")
    ).permitAll()
    // Swagger UI — entry point, all UI assets, OAuth2 redirect
    .requestMatchers(
        new AntPathRequestMatcher("/swagger-ui.html"),
        new AntPathRequestMatcher("/swagger-ui/**")
    ).permitAll()
    // OpenAPI spec — JSON (exact + sub-paths), YAML, and swagger-config
    .requestMatchers(
        new AntPathRequestMatcher("/v3/api-docs"),
        new AntPathRequestMatcher("/v3/api-docs/**"),
        new AntPathRequestMatcher("/v3/api-docs.yaml")
    ).permitAll()
    // Webjars — swagger-ui JS/CSS assets served from classpath
    .requestMatchers(
        new AntPathRequestMatcher("/webjars/**")
    ).permitAll()
    .anyRequest().authenticated()
);
```

### Changes summary

| Change | Reason |
|---|---|
| Added `/swagger-ui.html` | Entry-point URL was previously 403 |
| Added `/v3/api-docs` (exact) | `/**` wildcard does not match the exact path in Spring Security |
| Added `/v3/api-docs.yaml` | YAML spec endpoint |
| Added `/webjars/**` | All static assets for Swagger UI (JS/CSS/images) |
| Added `/actuator/info` | Minor: also exposed in `application.yml`, should be permitted |
| Switched to `AntPathRequestMatcher` | Eliminates MVC/Reactive matcher ambiguity when both `spring-webmvc` and `spring-webflux` are on classpath |
| All non-listed endpoints still secured | `anyRequest().authenticated()` unchanged |

---

## Verification

### Static path audit (automated)

All 11 required paths confirmed present in `SecurityConfig.java`:

```
✅ /swagger-ui.html
✅ /swagger-ui/**
✅ /v3/api-docs
✅ /v3/api-docs/**
✅ /v3/api-docs.yaml
✅ /webjars/**
✅ /actuator/health
✅ /actuator/info
✅ /api/messages
✅ /api/sync
✅ /api/chat
✅ AntPathRequestMatcher used (no MVC/Reactive ambiguity)
✅ anyRequest().authenticated() — non-listed endpoints secured
```

### Test suite

```
Tests run: 11, Failures: 0, Errors: 0
BUILD SUCCESS
```

### Live verification (requires running stack)

```bash
# Start the stack
docker compose up -d

# Verify OpenAPI JSON
curl -s http://localhost:8080/v3/api-docs | python3 -m json.tool | head -20
# Expected: {"openapi":"3.0.1","info":{"title":"OpenAPI definition",...}

# Verify Swagger UI loads (follow redirect)
curl -sL -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui.html
# Expected: 200

# Verify webjars serve correctly
curl -sL -o /dev/null -w "%{http_code}" \
  "http://localhost:8080/webjars/swagger-ui/5.13.0/swagger-ui-bundle.js"
# Expected: 200

# Verify security still blocks unknown paths
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/admin/secret
# Expected: 401 or 403
```

### Open in browser

```
http://localhost:8080/swagger-ui.html
```

The Swagger UI will load and display:
- `Chat` tag with `POST /api/chat`
- `POST /api/sync`
- `POST /api/messages`

---

## Request Flow After Fix

```
Browser → GET /swagger-ui.html
              │
              ▼ (302 redirect by springdoc)
         GET /swagger-ui/index.html          ← ✅ permitAll via /swagger-ui/**
              │
              ▼
         GET /swagger-ui/swagger-initializer.js  ← ✅ permitAll via /swagger-ui/**
              │
              ▼
         GET /v3/api-docs/swagger-config      ← ✅ permitAll via /v3/api-docs/**
              │
              ▼
         GET /v3/api-docs                     ← ✅ permitAll via /v3/api-docs (exact)
              │
              ▼
         GET /webjars/swagger-ui/5.13.0/swagger-ui-bundle.js  ← ✅ permitAll via /webjars/**
              │
              ▼
         Swagger UI renders ✅
```
