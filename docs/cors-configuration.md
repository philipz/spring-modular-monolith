# CORS Configuration Guide

## Overview

This document explains the CORS (Cross-Origin Resource Sharing) configuration for the BookStore application and when it is needed.

## Architecture Context

### Production Deployment (Docker Compose)

```
User → nginx (port 8080)
         ├─→ / (frontend)         → frontend-next:3000 (Next.js)
         └─→ /api/* (backend)     → monolith:8080 (Spring Boot)
```

**CORS Status**: ❌ **Not Needed**
- Reason: Same-origin (all requests go through nginx on port 8080)
- Frontend and backend appear to be on the same domain

### Development Scenarios

#### Scenario 1: Docker Compose Development

```bash
docker compose up
```

**CORS Status**: ❌ **Not Needed**
- Same architecture as production
- Requests go through nginx proxy

#### Scenario 2: Local Dev Server

```bash
# Frontend
cd frontend-next
./dev.sh  # Runs on http://localhost:3000

# Backend (in another terminal)
cd ..
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  # Runs on http://localhost:8080
```

**CORS Status**: ✅ **Needed**
- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Different origins require CORS

## CorsConfig Implementation

### File Location
```
src/main/java/com/sivalabs/bookstore/config/CorsConfig.java
```

### Profile Activation
```java
@Configuration
@Profile("dev")
public class CorsConfig implements WebMvcConfigurer {
    // Only active with 'dev' profile
}
```

### Configuration Details

**Allowed Origin**: `http://localhost:3000`
- Next.js dev server default port

**Allowed Methods**: `GET, POST, PUT, DELETE, OPTIONS`
- All standard HTTP methods for RESTful APIs

**Allow Credentials**: `true`
- Required for session cookies (shopping cart state)

**Max Age**: `3600` seconds (1 hour)
- Preflight request cache duration

**Path Pattern**: `/api/**`
- Only applies to backend API endpoints

## Enabling CORS for Development

### Method 1: Spring Profile
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Method 2: Application Properties
```properties
# In application-dev.properties or application.properties
spring.profiles.active=dev
```

### Method 3: Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

## Verification

### Check if CORS is Active

**Without CORS** (production/docker):
```bash
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: GET" \
     -X OPTIONS \
     http://localhost:8080/api/products -v
```
Expected: No `Access-Control-Allow-Origin` header

**With CORS** (dev profile):
```bash
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: GET" \
     -X OPTIONS \
     http://localhost:8080/api/products -v
```
Expected:
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
```

## Troubleshooting

### Issue: CORS errors in browser console

**Symptoms**:
```
Access to fetch at 'http://localhost:8080/api/products' from origin 'http://localhost:3000'
has been blocked by CORS policy
```

**Solutions**:

1. **Verify dev profile is active**:
   ```bash
   # Check application logs for:
   The following profiles are active: dev
   ```

2. **Check frontend is using correct URL**:
   ```typescript
   // In frontend-next/.env.local
   NEXT_PUBLIC_API_URL=http://localhost:8080
   ```

3. **Verify backend is running**:
   ```bash
   curl http://localhost:8080/api/actuator/health
   ```

### Issue: Session cookies not working

**Symptoms**:
- Cart items not persisting
- User session lost between requests

**Solutions**:

1. **Verify credentials are included in fetch**:
   ```typescript
   fetch('http://localhost:8080/api/cart', {
     credentials: 'include'  // Must include cookies
   })
   ```

2. **Check CORS allows credentials**:
   ```java
   .allowCredentials(true)  // Must be enabled
   ```

3. **Ensure origin matches exactly**:
   - CORS origin: `http://localhost:3000`
   - Frontend origin: Must be exactly `http://localhost:3000` (not `127.0.0.1`)

## Security Considerations

### Development Only

⚠️ The `@Profile("dev")` annotation ensures CORS is **ONLY** enabled in development.

**Production**: CORS configuration is automatically disabled when:
- Spring profile is not `dev`
- Running in Docker Compose (default profile)

### Allowed Origins

The configuration **only** allows `http://localhost:3000`.

To add more origins (e.g., for multiple developers):
```java
.allowedOrigins(
    "http://localhost:3000",
    "http://localhost:3001",
    "http://192.168.1.100:3000"
)
```

⚠️ **Never** use `.allowedOrigins("*")` with `.allowCredentials(true)` - browsers will reject this.

## Related Documentation

- [Next.js Frontend Development](../frontend-next/README-OPENAPI.md)
- [Docker Compose Setup](../compose.yml)
- [nginx Configuration](../webproxy/nginx.conf)
