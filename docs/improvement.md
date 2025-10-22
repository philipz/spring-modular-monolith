# Improvement Backlog (2025-10)

## Hazelcast & Cache
- Monitor `OrderMapStore` and `InventoryMapStore` warning volume during startup; consider lowering log level once `STARTUP_GRACE_PERIOD_MS` elapses.
- Expose cache metrics (hits, evictions) through a dedicated `/actuator/metrics` namespace to ease Grafana dashboarding.
- Evaluate multi-item cart support by introducing an `items` collection in `Cart` and adjusting MapStore write-through logic accordingly.

## Session & Security
- Add authentication (JWT/OAuth) to guard `/api/cart/**` and `/api/orders/**`, then set `server.servlet.session.cookie.secure=true` in production.
- Provide a resilience strategy if Hazelcast is unavailable (e.g., fallback to in-memory session) to avoid checkout failures.

## API & Domain
- Enrich `CreateOrderResponse` with status and timestamps so the frontend can display confirmation details without an extra fetch.
- Extend `/api/orders` list endpoint with pagination once historical data grows.
- Document gRPC proto evolution guidelines to keep monolith and extracted orders-service in sync.

## Observability
- Enable structured logging (JSON) with trace context for easier ingestion in HyperDX.
- Add synthetic checks that call `/api/cart` and `/api/orders` to validate session behaviour across both monolith and orders-service backends.

## Tooling & DX
- Provide `pnpm lint` / `pnpm typecheck` wrappers in `Taskfile.yml` for consistent frontend automation.
- Automate OpenAPI diff checks in CI to warn when breaking API changes are introduced.
- Expand k6 scripts to simulate a complete user journey (browse → add to cart → create order → fetch order history).
