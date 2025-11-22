# System Architecture Diagrams

> **Relevant source files**
> * [compose.yml](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/compose.yml)
> * [docs/1.png](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/docs/1.png)
> * [docs/2.png](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/docs/2.png)
> * [docs/3.png](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/docs/3.png)
> * [docs/4.png](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/docs/4.png)
> * [docs/5.png](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/docs/5.png)
> * [docs/6.png](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/docs/6.png)
> * [docs/history.md](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/docs/history.md)
> * [pom.xml](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/pom.xml)
> * [src/main/resources/application.properties](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/resources/application.properties)
> * [src/test/java/com/sivalabs/bookstore/BookStoreApplicationTests.java](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/test/java/com/sivalabs/bookstore/BookStoreApplicationTests.java)
> * [src/test/java/com/sivalabs/bookstore/TestcontainersConfiguration.java](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/test/java/com/sivalabs/bookstore/TestcontainersConfiguration.java)

This page provides visual representations of the system's architecture at multiple levels of abstraction, from the complete deployment topology down to internal module communication patterns. These diagrams illustrate how the Spring Modular Monolith transitions toward a microservices architecture while maintaining operational simplicity.

For detailed information about individual business modules, see [Business Modules](/philipz/spring-modular-monolith/8-business-modules). For deployment procedures, see [Deployment and Infrastructure](/philipz/spring-modular-monolith/10-deployment-and-infrastructure). For module boundaries and communication patterns, see [Spring Modulith Architecture](/philipz/spring-modular-monolith/3-spring-modulith-architecture).

---

## Complete System Deployment Architecture

The following diagram shows the full Docker Compose deployment topology, including all services, networking, and external dependencies:

```mermaid
flowchart TD

Client["Web Browser/API Client"]
Nginx["webproxy<br>nginx:latest<br>Port 80<br>OpenTelemetry enabled"]
Frontend["frontend-next<br>Next.js 14<br>Port 3000<br>NODE_ENV=production"]
Monolith["monolith<br>sivaprasadreddy/spring-modular-monolith:0.0.1-SNAPSHOT<br>HTTP: 8080, gRPC: 9091"]
OrdersSvc["orders-service<br>philipz/orders-service:0.0.1-SNAPSHOT<br>gRPC: 9090"]
AMQPMod["amqp-modulith<br>philipz/amqp-modulith:0.0.1-SNAPSHOT<br>Port 8082"]
Hazelcast["HazelcastInstance<br>bookstore-cluster<br>Distributed Cache & Sessions"]
HZMgmt["hazelcast-mgmt<br>hazelcast/management-center<br>Port 38080"]
RabbitMQ["RabbitMQ<br>rabbitmq:4.1.3-management-alpine<br>AMQP: 5672<br>Management: 15672"]
PostgresMain["postgres<br>postgres:17-alpine<br>Port 5432<br>max_connections=300"]
PostgresOrders["orders-postgres<br>postgres:17-alpine<br>Dedicated DB"]
HyperDX["hyperdx<br>docker.hyperdx.io/hyperdx/hyperdx-all-in-one<br>UI: 8081<br>OTLP gRPC: 4317<br>OTLP HTTP: 4318"]

Client -->|"HTTP/HTTPS"| Nginx
Nginx -->|"/ → proxy_pass"| Frontend
Nginx -->|"/api/** → traffic split"| Monolith
Monolith -->|"Read/Writespring.session.hazelcast"| Hazelcast
Monolith -->|"spring.rabbitmq.host"| RabbitMQ
Monolith -->|"spring.datasource.urljdbc:postgresql://postgres:5432/postgres"| PostgresMain
Monolith -->|"OTLP_ENDPOINTUnsupported markdown: link"| HyperDX
OrdersSvc -->|"Read/Write"| Hazelcast
OrdersSvc -->|"SPRING_RABBITMQ_HOST"| RabbitMQ
OrdersSvc -->|"SPRING_DATASOURCE_URL"| PostgresOrders
OrdersSvc -->|"OTLP_ENDPOINT"| HyperDX
AMQPMod -->|"Consume Events"| RabbitMQ
AMQPMod -->|"SPRING_DATASOURCE_URL"| PostgresOrders

subgraph subGraph6 ["Observability Layer"]
    HyperDX
end

subgraph subGraph5 ["Data Layer"]
    PostgresMain
    PostgresOrders
end

subgraph subGraph4 ["Messaging Layer"]
    RabbitMQ
end

subgraph subGraph3 ["Caching & Session Layer"]
    Hazelcast
    HZMgmt
    Hazelcast -->|"Monitored byHZ_CLUSTERNAME=bookstore-cluster"| HZMgmt
end

subgraph subGraph2 ["Application Layer"]
    Frontend
    Monolith
    OrdersSvc
    AMQPMod
    Frontend -->|"NEXT_API_PROXY_TARGETUnsupported markdown: link"| Monolith
    Monolith -->|"BOOKSTORE_GRPC_CLIENT_TARGETorders-service:9090"| OrdersSvc
end

subgraph subGraph1 ["Entry Point Layer"]
    Nginx
end

subgraph subGraph0 ["External Access"]
    Client
end
```

**Key Configuration Properties:**

| Service | Key Properties | File Reference |
| --- | --- | --- |
| `monolith` | `SPRING_DATASOURCE_URL`, `BOOKSTORE_GRPC_CLIENT_TARGET`, `OTLP_ENDPOINT` | [compose.yml L58-L86](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/compose.yml#L58-L86) |
| `orders-service` | `SPRING_DATASOURCE_URL`, `PRODUCT_API_BASE_URL`, `SPRING_MODULITH_EVENTS_SCHEMA` | [compose.yml L88-L117](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/compose.yml#L88-L117) |
| `frontend-next` | `NEXT_PUBLIC_API_URL=/api`, `NEXT_API_PROXY_TARGET` | [compose.yml L140-L158](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/compose.yml#L140-L158) |
| `webproxy` | `HYPERDX_API_KEY` | [compose.yml L160-L173](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/compose.yml#L160-L173) |

**Sources:** [compose.yml L1-L189](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/compose.yml#L1-L189)

---

## Spring Modulith Module Communication Architecture

This diagram shows the internal structure of the Spring Boot monolith, focusing on module boundaries enforced by Spring Modulith:

```mermaid
flowchart TD

RestAPI["REST Controllers<br>/api/** endpoints<br>@RestController"]
GrpcAPI["OrdersGrpcServiceImpl<br>@GrpcService<br>Port 9091"]
HazelcastConfig["HazelcastConfig<br>@Configuration"]
GrpcServerConfig["GrpcServerConfig<br>bookstore.grpc.server.*"]
OtlpGrpcConfig["OtlpGrpcTracingConfig<br>otlp.grpc.*"]
CacheService["CacheService<br>Utility methods"]
ExceptionHandling["GlobalExceptionHandler<br>@RestControllerAdvice"]
ProductApi["ProductApi<br>@ApplicationModuleListener.AllowedDependencies"]
ProductService["ProductService<br>@Service"]
ProductMapStore["ProductMapStore<br>MapStore<String, Product>"]
ProductRepo["ProductRepository<br>JpaRepository"]
OrdersApi["OrdersApi<br>Exported API"]
OrderService["OrderService<br>@Transactional"]
OrderMapStore["OrderMapStore<br>MapStore<String, OrderDTO>"]
OrdersGrpcClient["OrdersGrpcClient<br>ManagedChannel"]
OrderRepo["OrderRepository<br>JpaRepository"]
InventoryService["InventoryService<br>@Service"]
InventoryMapStore["InventoryMapStore<br>MapStore<Long, Inventory>"]
InventoryRepo["InventoryRepository<br>JpaRepository"]
NotificationEventListener["NotificationEventListener<br>@ApplicationModuleListener"]
ModulithEvents["ApplicationEvents<br>spring.modulith.events.jdbc.schema=events"]
RabbitExternalization["RabbitMQ Externalization<br>@Externalized"]
CatalogSchema["catalog schema<br>Liquibase migrations<br>db/catalog/"]
OrdersSchema["orders schema<br>db/orders/"]
InventorySchema["inventory schema<br>db/inventory/"]
EventsSchema["events schema<br>EVENT_PUBLICATION table"]

RestAPI --> OrderService
RestAPI --> ProductService
GrpcAPI --> OrderService
OrderService -->|"PublishesOrderCreatedEvent"| ModulithEvents
ModulithEvents -->|"@ApplicationModuleListener"| InventoryService
ModulithEvents -->|"@ApplicationModuleListener"| NotificationEventListener
ProductRepo --> CatalogSchema
OrderRepo --> OrdersSchema
InventoryRepo --> InventorySchema
ModulithEvents --> EventsSchema

subgraph subGraph9 ["Data Schemas"]
    CatalogSchema
    OrdersSchema
    InventorySchema
    EventsSchema
end

subgraph subGraph8 ["Event Bus"]
    ModulithEvents
    RabbitExternalization
    ModulithEvents -->|"@Externalized"| RabbitExternalization
end

subgraph subGraph7 ["Spring Modulith Modules"]
    HazelcastConfig -->|"Provides beans"| ProductMapStore
    HazelcastConfig -->|"Provides beans"| OrderMapStore
    HazelcastConfig -->|"Provides beans"| InventoryMapStore
    CacheService -->|"Used by"| ProductMapStore
    CacheService -->|"Used by"| OrderMapStore
    OrderService -->|"API callproductApi.getProductByCode()"| ProductApi

subgraph subGraph6 ["notifications module"]
    NotificationEventListener
end

subgraph subGraph5 ["inventory module"]
    InventoryService
    InventoryMapStore
    InventoryRepo
    InventoryService --> InventoryRepo
end

subgraph subGraph4 ["orders module"]
    OrdersApi
    OrderService
    OrderMapStore
    OrdersGrpcClient
    OrderRepo
    OrderService --> OrderRepo
end

subgraph subGraph3 ["catalog module"]
    ProductApi
    ProductService
    ProductMapStore
    ProductRepo
    ProductService --> ProductRepo
end

subgraph subGraph2 ["common module [OPEN]"]
    CacheService
    ExceptionHandling
end

subgraph subGraph1 ["config module"]
    HazelcastConfig
    GrpcServerConfig
    OtlpGrpcConfig
end
end

subgraph subGraph0 ["Presentation Layer"]
    RestAPI
    GrpcAPI
end
```

**Module Access Rules:**

| Module | Type | Exports | Dependencies |
| --- | --- | --- | --- |
| `config` | Infrastructure | Configuration beans | None (base module) |
| `common` | Shared | All classes (`@ApplicationModule(type = OPEN)`) | None |
| `catalog` | Business | `ProductApi` | `common` |
| `orders` | Business | `OrdersApi` | `common`, `catalog` (via `ProductApi`) |
| `inventory` | Business | None (internal) | `common`, listens to `orders` events |
| `notifications` | Business | None (internal) | `common`, listens to events |

**Sources:** [src/main/java/com/sivalabs/bookstore/config/](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/config/)

 [src/main/java/com/sivalabs/bookstore/catalog/](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/catalog/)

 [src/main/java/com/sivalabs/bookstore/orders/](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/orders/)

 [src/main/java/com/sivalabs/bookstore/inventory/](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/inventory/)

 [src/main/java/com/sivalabs/bookstore/notifications/](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/notifications/)

 [src/main/resources/application.properties L36-L40](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/resources/application.properties#L36-L40)

---

## Data, Caching, and Persistence Architecture

This diagram illustrates the three-tier data architecture with Hazelcast distributed caching and PostgreSQL multi-schema design:

```mermaid
flowchart TD

CatalogSvc["ProductService<br>catalog module"]
OrdersSvc["OrderService<br>orders module"]
InventorySvc["InventoryService<br>inventory module"]
HZInstance["HazelcastInstance<br>bookstore-cluster<br>Config.buildConfig()"]
ProductCache["products-cache<br>MapConfig<br>bookstore.cache.time-to-live-seconds=3600"]
OrderCache["orders-cache<br>MapConfig<br>TTL: 3600s"]
InventoryCache["inventory-cache<br>MapConfig<br>TTL: 1800s"]
InventoryIndex["inventory-by-product-code-cache<br>Index-only IMap"]
ProductMS["ProductMapStore<br>implements MapStore<String, Product><br>write-delay-seconds=0"]
OrderMS["OrderMapStore<br>implements MapStore<String, OrderDTO>"]
InventoryMS["InventoryMapStore<br>implements MapStore<Long, Inventory>"]
SessionStore["spring:session:sessions<br>spring.session.hazelcast.map-name<br>TTL: 30m"]
CircuitBreaker["CircuitBreakerRegistry<br>bookstore.cache.circuit-breaker.failure-threshold=5<br>recovery-timeout=30000ms"]
CatalogSchema["catalog schema<br>products table<br>LiquibaseConfig"]
OrdersSchema["orders schema<br>orders, order_items tables"]
InventorySchema["inventory schema<br>inventory table"]
EventsSchema["events schema<br>event_publication table<br>spring.modulith.events.jdbc.schema"]

CatalogSvc -->|"Read/Write"| ProductCache
OrdersSvc -->|"Read/Write"| OrderCache
InventorySvc -->|"Read/Write"| InventoryCache
InventorySvc -->|"Index Lookup"| InventoryIndex
ProductMS -->|"JDBCJpaRepository"| CatalogSchema
OrderMS -->|"JDBC"| OrdersSchema
InventoryMS -->|"JDBC"| InventorySchema

subgraph subGraph4 ["PostgreSQL Multi-Schema"]
    CatalogSchema
    OrdersSchema
    InventorySchema
    EventsSchema
end

subgraph subGraph3 ["Hazelcast Distributed Cache"]
    HZInstance
    SessionStore
    CircuitBreaker
    ProductCache -->|"Cache Miss/Write-Throughwrite-batch-size=1"| ProductMS
    OrderCache -->|"Cache Miss/Write-Through"| OrderMS
    InventoryCache -->|"Cache Miss/Write-Through"| InventoryMS
    HZInstance -->|"Provides"| ProductCache
    HZInstance -->|"Provides"| OrderCache
    HZInstance -->|"Provides"| InventoryCache
    HZInstance -->|"Provides"| SessionStore
    HZInstance -->|"Protects"| CircuitBreaker

subgraph subGraph2 ["MapStore Layer"]
    ProductMS
    OrderMS
    InventoryMS
end

subgraph subGraph1 ["Module-Specific Caches"]
    ProductCache
    OrderCache
    InventoryCache
    InventoryIndex
end
end

subgraph subGraph0 ["Application Services"]
    CatalogSvc
    OrdersSvc
    InventorySvc
end
```

**Cache Configuration Properties:**

| Property | Value | Purpose |
| --- | --- | --- |
| `bookstore.cache.enabled` | `true` | Global cache enablement |
| `bookstore.cache.max-size` | `1000` | Max entries per cache |
| `bookstore.cache.time-to-live-seconds` | `3600` (products/orders), `1800` (inventory) | Entry TTL |
| `bookstore.cache.write-through` | `true` | Synchronous write to DB |
| `bookstore.cache.write-delay-seconds` | `0` | Immediate persistence |
| `bookstore.cache.backup-count` | `1` | Cluster backup copies |
| `bookstore.cache.circuit-breaker.failure-threshold` | `5` | Circuit breaker threshold |

**Sources:** [src/main/java/com/sivalabs/bookstore/config/HazelcastConfig.java](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/config/HazelcastConfig.java)

 [src/main/java/com/sivalabs/bookstore/catalog/domain/ProductMapStore.java](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/catalog/domain/ProductMapStore.java)

 [src/main/java/com/sivalabs/bookstore/orders/domain/OrderMapStore.java](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/orders/domain/OrderMapStore.java)

 [src/main/java/com/sivalabs/bookstore/inventory/domain/InventoryMapStore.java](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/inventory/domain/InventoryMapStore.java)

 [src/main/resources/application.properties L42-L68](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/resources/application.properties#L42-L68)

---

## API Gateway and Traffic Management

This diagram shows nginx routing logic and the progressive traffic migration pattern for orders:

```mermaid
flowchart TD

Browser["Web Browser"]
APIClient["API Consumer<br>curl, Postman, etc."]
Nginx["nginx:latest<br>Port 80<br>ngx_otel_module.so"]
PathRouter["Path-Based Routing<br>location / vs /api/**"]
TrafficSplit["Traffic Split Logic<br>X-Orders-Backend header<br>orders_backend cookie<br>ORDERS_SERVICE_PERCENT"]
NextJS["frontend-next<br>Next.js 14 App Router<br>Port 3000<br>SSR + API Proxy"]
MonolithREST["Monolith REST API<br>@RestController<br>:8080/api/**"]
MonolithGRPC["Monolith gRPC Server<br>OrdersGrpcServiceImpl<br>:9091"]
OrdersGRPC["orders-service<br>OrdersGrpcServiceImpl<br>:9090"]
OpenAPI["OpenAPI 3.0 Spec<br>springdoc.api-docs.path=/api-docs"]
SwaggerUI["Swagger UI<br>/swagger-ui.html"]

Browser -->|"HTTP"| Nginx
APIClient -->|"HTTP/JSON"| Nginx
PathRouter -->|"/ → proxy_pass Unsupported markdown: link"| NextJS
TrafficSplit -->|"Legacy path0-99%"| MonolithREST
TrafficSplit -->|"New path via gRPC1-100%"| OrdersGRPC
MonolithREST -->|"Exposes"| OpenAPI

subgraph subGraph4 ["API Documentation"]
    OpenAPI
    SwaggerUI
    OpenAPI -->|"Renders"| SwaggerUI
end

subgraph subGraph3 ["Backend Services"]
    NextJS
    MonolithREST
    MonolithGRPC
    OrdersGRPC
    NextJS -->|"Dev ModeNEXT_API_PROXY_TARGET"| MonolithREST
    MonolithREST -->|"Internal delegationOrdersRemoteClient"| MonolithGRPC
    MonolithGRPC -->|"gRPC ProtocolOrdersServiceGrpc.newBlockingStub()"| OrdersGRPC
end

subgraph subGraph2 ["webproxy - nginx"]
    Nginx
    Nginx -->|"Route by Path"| PathRouter

subgraph subGraph1 ["Routing Logic"]
    PathRouter
    TrafficSplit
    PathRouter -->|"/api/** → evaluate split"| TrafficSplit
end
end

subgraph subGraph0 ["External Clients"]
    Browser
    APIClient
end
```

**Traffic Split Configuration:**

Traffic to `/api/orders/**` can be split between the monolith and `orders-service`:

* **Header override:** `X-Orders-Backend: orders` forces routing to `orders-service`
* **Cookie persistence:** `orders_backend=orders` maintains routing across requests
* **Percentage split:** `ORDERS_SERVICE_PERCENT` environment variable (0-100) controls gradual rollout

**Sources:** [webproxy/nginx.conf](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/webproxy/nginx.conf)

 [compose.yml L160-L173](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/compose.yml#L160-L173)

 [src/main/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcClient.java](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcClient.java)

 [src/main/resources/application.properties L113-L126](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/resources/application.properties#L113-L126)

---

## Observability and Distributed Tracing

This diagram shows the comprehensive observability setup using OpenTelemetry and HyperDX:

```mermaid
flowchart TD

Monolith["spring-modular-monolith<br>Port 8080"]
OrdersSvc["orders-service<br>Port 9090"]
Nginx["nginx webproxy<br>Port 80"]
SpringBootAuto1["Spring Boot Auto-Config<br>management.tracing.enabled=true"]
MicrometerBridge1["Micrometer OTLP Bridge<br>micrometer-tracing-bridge-otel"]
ModulithObs1["Spring Modulith Observability<br>spring-modulith-observability"]
SpringBootAuto2["Spring Boot Auto-Config"]
MicrometerBridge2["Micrometer OTLP Bridge"]
OTelModule["ngx_otel_module.so<br>otel_exporter_endpoint"]
WebMVC["Spring Web MVC<br>@RestController"]
JDBC["JDBC/DataSource<br>datasource-micrometer-spring-boot"]
RabbitClient["RabbitMQ Client<br>spring-rabbit"]
gRPCComm["gRPC Client/Server<br>io.grpc:grpc-netty-shaded"]
Actuator["Spring Actuator<br>management.endpoints.web.exposure.include=*"]
Prometheus["Prometheus Registry<br>micrometer-registry-prometheus"]
HyperDX["HyperDX UI<br>Port 8081"]
OTLPGrpc["OTLP gRPC Receiver<br>Port 4317<br>otlp.grpc.endpoint"]
OTLPHttp["OTLP HTTP Receiver<br>Port 4318"]
Traces["Distributed Traces<br>management.tracing.sampling.probability=1.0"]
Metrics["Application Metrics"]
Logs["Structured Logs<br>logging.pattern.correlation"]

Monolith --> SpringBootAuto1
Monolith --> Actuator
SpringBootAuto1 --> WebMVC
SpringBootAuto1 --> JDBC
SpringBootAuto1 --> RabbitClient
SpringBootAuto1 --> gRPCComm
OrdersSvc --> SpringBootAuto2
SpringBootAuto2 --> WebMVC
SpringBootAuto2 --> JDBC
SpringBootAuto2 --> RabbitClient
SpringBootAuto2 --> gRPCComm
Nginx --> OTelModule
SpringBootAuto1 -->|"gRPCotlp.grpc.compression=gzipotlp.grpc.timeout=10s"| OTLPGrpc
SpringBootAuto2 -->|"gRPCOTLP_ENDPOINT"| OTLPGrpc
OTelModule -->|"gRPCHYPERDX_API_KEY"| OTLPGrpc

subgraph subGraph9 ["HyperDX All-in-One"]
    HyperDX
    OTLPGrpc --> Traces
    OTLPGrpc --> Metrics
    OTLPGrpc --> Logs
    OTLPHttp --> Traces
    HyperDX --> Traces
    HyperDX --> Metrics
    HyperDX --> Logs

subgraph subGraph8 ["Data Storage"]
    Traces
    Metrics
    Logs
end

subgraph subGraph7 ["Ingestion Endpoints"]
    OTLPGrpc
    OTLPHttp
end
end

subgraph subGraph6 ["Metrics Collection"]
    Actuator
    Prometheus
    Actuator --> Prometheus
end

subgraph subGraph5 ["Auto-Instrumented Components"]
    WebMVC
    JDBC
    RabbitClient
    gRPCComm
end

subgraph subGraph4 ["OpenTelemetry Instrumentation"]

subgraph subGraph3 ["Nginx Instrumentation"]
    OTelModule
end

subgraph subGraph2 ["orders-service Instrumentation"]
    SpringBootAuto2
    MicrometerBridge2
end

subgraph subGraph1 ["Monolith Instrumentation"]
    SpringBootAuto1
    MicrometerBridge1
    ModulithObs1
    SpringBootAuto1 --> ModulithObs1
end
end

subgraph subGraph0 ["Application Services"]
    Monolith
    OrdersSvc
    Nginx
end
```

**Key Observability Configuration:**

| Property | Value | Purpose |
| --- | --- | --- |
| `management.tracing.enabled` | `true` | Enable distributed tracing |
| `management.tracing.sampling.probability` | `1.0` | 100% trace sampling (dev/staging) |
| `otlp.grpc.endpoint` | `http://hyperdx:4317` | OTLP collector endpoint |
| `otlp.grpc.compression` | `gzip` | Compress trace payloads |
| `otlp.grpc.timeout` | `10s` | Export timeout |
| `logging.pattern.correlation` | `[${spring.application.name:},%X{traceId:-},%X{spanId:-}]` | Trace ID injection in logs |

**Sources:** [compose.yml L49-L56](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/compose.yml#L49-L56)

 [compose.yml L72-L73](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/compose.yml#L72-L73)

 [compose.yml L107-L108](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/compose.yml#L107-L108)

 [src/main/java/com/sivalabs/bookstore/config/OtlpProperties.java](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/config/OtlpProperties.java)

 [src/main/java/com/sivalabs/bookstore/config/OtlpGrpcTracingConfig.java](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/config/OtlpGrpcTracingConfig.java)

 [src/main/resources/application.properties L70-L82](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/resources/application.properties#L70-L82)

 [pom.xml L81-L92](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/pom.xml#L81-L92)

---

## Code-to-Architecture Mapping

The following table maps high-level architectural concepts to specific code entities:

| Architectural Concept | Code Entity | Location |
| --- | --- | --- |
| Module Boundary Enforcement | `@ApplicationModule` annotations | Each module's `package-info.java` |
| Exported APIs | `ProductApi`, `OrdersApi` interfaces | `catalog/api/`, `orders/api/` |
| Event Publication | `ApplicationEventPublisher.publishEvent()` | `OrderService`, `InventoryService` |
| Event Consumption | `@ApplicationModuleListener` methods | `InventoryEventHandler`, `NotificationEventListener` |
| Cache Configuration | `MapConfig` beans | `HazelcastConfig`, module-specific configs |
| Write-Through Caching | `MapStore<K, V>` implementations | `ProductMapStore`, `OrderMapStore`, `InventoryMapStore` |
| gRPC Service | `@GrpcService` + `OrdersServiceGrpc.OrdersServiceImplBase` | `OrdersGrpcServiceImpl` |
| gRPC Client | `ManagedChannel` + `OrdersServiceGrpc.newBlockingStub()` | `OrdersGrpcClient` |
| Schema Migration | `Liquibase` + module-specific changelogs | `db/catalog/`, `db/orders/`, `db/inventory/` |
| Session Distribution | `@EnableHazelcastHttpSession` | `HazelcastConfig` |
| Circuit Breaker | `CircuitBreakerRegistry` | `HazelcastConfig.circuitBreaker()` |
| Tracing Instrumentation | `OtlpGrpcSpanExporter` | `OtlpGrpcTracingConfig` |

**Sources:** [src/main/java/com/sivalabs/bookstore/](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/)

 (various modules), [src/main/resources/db/](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/resources/db/)

 [src/main/resources/application.properties](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/resources/application.properties)