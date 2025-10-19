# Implementation Plan

## Task Overview

This implementation replaces Zipkin with HyperDX using a configuration-first approach. All changes are declarative (Maven dependencies, application properties, Docker Compose), requiring zero application code modifications. The migration maintains existing Spring Modulith observability features while upgrading to OpenTelemetry protocol with gRPC transport.

## Steering Document Compliance

**Technical Standards (tech.md)**:
- Maintains Spring Boot 3.5.5 + Spring Modulith 1.4.3 compatibility
- Uses Maven dependency management for version control
- Follows Spring Boot property configuration patterns
- Leverages Docker Compose for local development

**Project Structure (structure.md)**:
- Dependencies managed in `pom.xml` and `orders/pom.xml`
- Runtime configuration in `src/main/resources/application.properties`
- Infrastructure defined in `compose.yml`
- Documentation updated in `CLAUDE.md` and `README.md`

## Atomic Task Requirements

**Each task meets these criteria for optimal execution**:
- **File Scope**: Touches 1-3 related files maximum
- **Time Boxing**: Completable in 15-30 minutes
- **Single Purpose**: One testable outcome per task
- **Specific Files**: Exact file paths to create/modify
- **Agent-Friendly**: Clear input/output with minimal context switching

## Tasks

### Phase 1: Maven Dependency Updates

- [x] 1. Remove Zipkin exporter from main POM and add OTLP exporter
  - File: `pom.xml` (lines 88-91)
  - Remove `io.opentelemetry:opentelemetry-exporter-zipkin` dependency (line 88-91)
  - Add `io.opentelemetry:opentelemetry-exporter-otlp` dependency after line 87
  - Keep existing `micrometer-tracing-bridge-otel` dependency (line 85-87)
  - Verify dependency block compiles successfully with `./mvnw clean compile -DskipTests`
  - Purpose: Replace Zipkin-specific exporter with OpenTelemetry OTLP exporter for vendor-neutral observability
  - _Leverage: Existing dependencyManagement from Spring Boot Parent POM (line 19-22)_
  - _Requirements: 1.1, 1.2, 1.4_

- [x] 2. Update orders-service POM dependencies to match main POM
  - File: `orders/pom.xml`
  - Search for `io.opentelemetry:opentelemetry-exporter-zipkin` and remove if present
  - Search for `micrometer-tracing-bridge-brave` and remove if present
  - Search for `zipkin-reporter-brave` and remove if present
  - Ensure `io.opentelemetry:opentelemetry-exporter-otlp` is added or inherited from parent
  - Ensure `micrometer-tracing-bridge-otel` is present or inherited
  - Verify orders module compiles with `./mvnw clean compile -pl orders -DskipTests`
  - Purpose: Ensure consistent observability dependencies across all modules
  - _Leverage: Parent POM dependency management_
  - _Requirements: 1.3, 1.4_

### Phase 2: Application Properties Configuration

- [x] 3. Replace Zipkin endpoint with OpenTelemetry OTLP endpoint in application.properties
  - File: `src/main/resources/application.properties` (line 75)
  - Comment out existing line: `# management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans`
  - Add new OTLP configuration after line 75:
    ```properties
    # OpenTelemetry OTLP exporter configuration (HTTP endpoint for local development)
    management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
    ```
  - Keep existing `management.tracing.enabled=true` and `management.tracing.sampling.probability=1.0` (lines 73-74)
  - Purpose: Configure Spring Boot to export traces to HyperDX using OTLP HTTP endpoint
  - _Leverage: Existing management.* property namespace and Spring Boot auto-configuration_
  - _Requirements: 2.1, 2.5_

### Phase 3: Docker Compose Service Updates (Already Completed)

- [x] 4. Docker Compose service replacement (completed in previous session)
  - File: `compose.yml`
  - Zipkin service removed and replaced with HyperDX service
  - HyperDX configured with ports 4317 (gRPC), 4318 (HTTP), 8081 (UI)
  - Monolith environment variables updated with OTLP endpoints
  - Orders-service environment variables updated with OTLP endpoints
  - Purpose: Provide HyperDX observability platform in local development environment
  - _Leverage: Existing Docker Compose proxy network and environment variable patterns_
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 4a. Verify Docker Compose environment variables for gRPC protocol
  - File: `compose.yml` (monolith and orders-service sections)
  - Current state: Environment variables have been updated to use simplified configuration
  - Verified configuration:
    - Both monolith and orders-service have `OTLP_ENDPOINT: http://hyperdx:4318/v1/traces`
    - Both services have `HYPERDX_API_KEY: XXXX`
    - YAML syntax validated successfully with `docker-compose config`
  - Purpose: Ensure OTLP endpoint configuration with authentication is complete for both services
  - _Leverage: Existing Docker Compose configuration_
  - _Requirements: 2.2, 2.3, 3.5_

### Phase 4: Documentation Updates

- [x] 5. Update CLAUDE.md Application URLs section
  - File: `CLAUDE.md` (Application URLs section around line 368)
  - Search for "Zipkin" and identify the line to replace
  - Remove Zipkin URL: `- **Zipkin**: http://localhost:9411`
  - Add HyperDX URLs in the same section:
    ```markdown
    - **HyperDX UI**: http://localhost:8081
    - **HyperDX gRPC**: localhost:4317
    - **HyperDX HTTP**: localhost:4318
    ```
  - Purpose: Update application URLs to reflect new observability platform
  - _Leverage: Existing documentation structure_
  - _Requirements: 5.1, 5.2_

- [x] 6. Update CLAUDE.md Technology Stack section
  - File: `CLAUDE.md` (Technology Stack section)
  - Search for "Zipkin" in Observability subsection
  - Replace "Zipkin" with "HyperDX - All-in-one observability platform"
  - Update description to mention "OpenTelemetry with HyperDX for distributed tracing"
  - Purpose: Reflect new observability stack in technical documentation
  - _Leverage: Existing documentation structure_
  - _Requirements: 5.4_

- [x] 7. Update README.md Technology Stack section
  - File: `README.md`
  - Search for "Zipkin" in Technology Stack or Observability sections
  - Replace with "OpenTelemetry with HyperDX"
  - Add brief description: "HyperDX - All-in-one observability platform with integrated traces, metrics, and logs visualization"
  - Update any docker-compose instructions that mention Zipkin
  - Purpose: Update project documentation to reflect new observability infrastructure
  - _Leverage: Existing README structure_
  - _Requirements: 5.3, 5.4_

### Phase 5: Integration Testing and Validation

- [ ] 8. Start Docker services and verify HyperDX connectivity
  - Commands to run:
    ```bash
    # Start all Docker services including HyperDX
    docker-compose up -d

    # Wait for services to start
    sleep 10

    # Verify all services are running
    docker-compose ps

    # Check HyperDX HTTP port
    curl -I http://localhost:4318/v1/traces
    ```
  - Expected outcomes:
    - All Docker services show "Up" status (postgres, rabbitmq, hyperdx, orders-service)
    - HyperDX container exposes ports 4317, 4318, 8081
    - HTTP endpoint returns HTTP/1.1 response (not connection refused)
    - No port conflict errors in logs
  - Purpose: Validate that HyperDX service is running and accepting connections
  - _Leverage: Existing docker-compose infrastructure_
  - _Requirements: 3.6, 4.1, 4.2_

- [ ] 9. Start application and verify OTLP exporter initialization
  - Prerequisites: Docker services running from task 8
  - Commands:
    ```bash
    # Start the application
    ./mvnw spring-boot:run &

    # Wait for startup
    sleep 30

    # Check for OTLP exporter in logs
    docker-compose logs monolith | grep -i "otlp\|opentelemetry" | head -20
    ```
  - Expected log patterns:
    - "OtlpGrpcSpanExporter" or "OtlpHttpSpanExporter" appears in logs
    - "endpoint=http://hyperdx:4318" or similar endpoint configuration
    - No "ClassNotFoundException" for opentelemetry classes
    - No "NoSuchMethodError" or dependency conflicts
  - Purpose: Confirm OTLP exporter is loaded and configured correctly
  - _Leverage: Spring Boot auto-configuration for OpenTelemetry_
  - _Requirements: 1.4, 2.4, 2.5_

- [ ] 10. Generate test traces and verify export to HyperDX
  - Prerequisites: Application running from task 9
  - Commands:
    ```bash
    # Generate traces by calling API endpoints
    curl http://localhost:8080/api/products
    curl http://localhost:8080/api/catalog

    # Wait for trace export
    sleep 5

    # Check application logs for trace IDs
    docker-compose logs monolith | grep "traceId" | tail -5

    # Check HyperDX logs for incoming OTLP requests
    docker-compose logs hyperdx | grep -i "otlp\|trace" | tail -10
    ```
  - Expected outcomes:
    - Application logs show trace IDs in correlation pattern: `[spring-modular-monolith,{traceId},{spanId}]`
    - HyperDX logs show incoming OTLP requests or trace processing
    - No connection errors or refused connections to hyperdx:4318
  - Purpose: Verify traces are generated and successfully exported to HyperDX
  - _Leverage: Spring Modulith automatic observability instrumentation_
  - _Requirements: 4.3, 6.1, 6.2_

- [ ] 11. Verify traces appear in HyperDX UI
  - Prerequisites: Traces generated from task 10
  - Manual verification steps:
    1. Open web browser to http://localhost:8081
    2. Navigate to traces section in HyperDX UI
    3. Search for service name "spring-modular-monolith"
    4. Verify traces appear with recent timestamps
    5. Click on a trace to view span details
    6. Verify multiple spans showing different modules (catalog, orders, etc.)
  - Expected outcomes:
    - Traces visible in UI within 10 seconds of generation
    - Service name "spring-modular-monolith" appears in traces
    - Individual spans show module names and operation details
    - Traces are queryable and filterable by time, service, operation
  - Purpose: Confirm end-to-end tracing pipeline from application to HyperDX UI
  - _Requirements: 4.3, 4.4_

- [ ] 12. Test module boundary tracing with cross-module operations
  - Prerequisites: Application and HyperDX running with traces visible
  - Commands:
    ```bash
    # Create an order (triggers cross-module events: Orders → Inventory → Notifications)
    curl -X POST http://localhost:8080/api/orders \
      -H "Content-Type: application/json" \
      -d '{"productCode":"BOOK-001","quantity":1,"customerId":"test@example.com"}'

    # Wait for async event processing
    sleep 10

    # Check logs for module event spans
    docker-compose logs monolith | grep -i "modulith\|event" | tail -20
    ```
  - Expected log patterns:
    - "Publishing event" messages with trace IDs
    - "ApplicationModuleListener" processing events
    - Spans for different modules (orders, inventory, notifications)
    - Parent-child span relationships in trace IDs
  - Manual UI verification:
    - Open HyperDX UI and find the order creation trace
    - Verify trace shows spans across multiple modules
    - Check span names include module identifiers
  - Purpose: Confirm Spring Modulith module boundary tracing works with OpenTelemetry
  - _Leverage: Spring Modulith Observability automatic module instrumentation_
  - _Requirements: 6.2, 6.3, 6.4_

- [ ] 13. Test graceful degradation when HyperDX is unavailable
  - Prerequisites: Application running successfully
  - Commands:
    ```bash
    # Stop HyperDX service
    docker-compose stop hyperdx

    # Execute application operations
    curl http://localhost:8080/api/products
    curl http://localhost:8080/api/catalog

    # Check application logs for connection errors (should be logged but not crash)
    docker-compose logs monolith | grep -i "error\|exception" | tail -10

    # Verify application still responds
    curl -I http://localhost:8080/actuator/health
    ```
  - Expected outcomes:
    - Application continues responding to requests (HTTP 200)
    - Connection errors logged but gracefully handled (no exceptions to user)
    - Actuator health endpoint shows UP status
    - No application crash or restart
  - Recovery verification:
    ```bash
    # Restart HyperDX
    docker-compose start hyperdx
    sleep 10

    # Generate new traces
    curl http://localhost:8080/api/products
    sleep 5

    # Verify traces resume in HyperDX logs
    docker-compose logs hyperdx | grep -i "otlp" | tail -5
    ```
  - Purpose: Validate graceful degradation and recovery behavior
  - _Leverage: Spring Boot's fault-tolerant observability configuration_
  - _Requirements: Non-functional: Reliability, Graceful Degradation_

- [ ] 14. Run existing test suite to ensure no regressions
  - Command: `./mvnw clean verify`
  - Expected outcomes:
    - All unit tests pass (no new failures)
    - All integration tests pass (ModularityTests, module integration tests)
    - No compilation errors related to missing Zipkin dependencies
    - Spotless formatting checks pass
    - Build completes with "BUILD SUCCESS"
  - If failures occur:
    - Review failure logs for dependency-related issues
    - Check ClassNotFoundException or NoClassDefFoundError
    - Verify no tests explicitly referenced Zipkin classes
    - Ensure Testcontainers start correctly
  - Purpose: Ensure observability changes don't break existing functionality
  - _Leverage: Existing JUnit 5 test suite and Testcontainers setup_
  - _Requirements: 6.4, Non-functional: Reliability_

### Phase 6: Final Verification

- [ ] 15. Review Maven dependency changes in POM files
  - Files to review: `pom.xml`, `orders/pom.xml`
  - Verification checklist:
    - [ ] `io.opentelemetry:opentelemetry-exporter-otlp` present in main pom.xml
    - [ ] `io.opentelemetry:opentelemetry-exporter-zipkin` removed from main pom.xml
    - [ ] `micrometer-tracing-bridge-otel` present (unchanged)
    - [ ] orders/pom.xml has consistent dependencies (no Zipkin references)
    - [ ] No commented Brave/Zipkin dependencies are uncommented
  - Commands:
    ```bash
    # Verify OTLP exporter is present
    grep -n "opentelemetry-exporter-otlp" pom.xml

    # Verify Zipkin exporter is absent (should return nothing)
    grep -n "opentelemetry-exporter-zipkin" pom.xml

    # Check orders module
    grep -n "opentelemetry" orders/pom.xml
    ```
  - Purpose: Validate dependency migration completeness
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 16. Review configuration file changes
  - Files to review: `src/main/resources/application.properties`, `compose.yml`
  - Verification checklist:
    - [ ] application.properties has `management.otlp.tracing.endpoint` configured
    - [ ] Zipkin endpoint is commented out or removed
    - [ ] compose.yml has hyperdx service with correct image and ports
    - [ ] Zipkin service is removed from compose.yml
    - [ ] Environment variables set correctly in monolith and orders-service
  - Commands:
    ```bash
    # Verify OTLP endpoint configured
    grep "management.otlp.tracing.endpoint" src/main/resources/application.properties

    # Verify Zipkin endpoint commented (should show # at start)
    grep "zipkin.tracing.endpoint" src/main/resources/application.properties

    # Verify HyperDX service in compose
    docker-compose config | grep -A 10 "hyperdx:"
    ```
  - Purpose: Validate property and infrastructure configuration
  - _Requirements: 2.1, 3.1, 3.2, 3.3, 3.5_

- [ ] 17. Review documentation updates
  - Files to review: `CLAUDE.md`, `README.md`
  - Verification checklist:
    - [ ] CLAUDE.md Application URLs section lists HyperDX URLs
    - [ ] CLAUDE.md Technology Stack mentions OpenTelemetry + HyperDX
    - [ ] No Zipkin references in CLAUDE.md
    - [ ] README.md Technology Stack updated
    - [ ] README.md docker-compose instructions mention HyperDX
  - Commands:
    ```bash
    # Check for any remaining Zipkin references (should return minimal/none)
    grep -i "zipkin" CLAUDE.md
    grep -i "zipkin" README.md

    # Verify HyperDX is documented
    grep -i "hyperdx" CLAUDE.md
    grep -i "hyperdx" README.md
    ```
  - Purpose: Ensure documentation accurately reflects new setup
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 18. Execute final validation checklist
  - Run comprehensive verification based on design.md Testing Strategy:
    ```bash
    # 1. Application starts without errors
    docker-compose up -d && ./mvnw spring-boot:run

    # 2. HyperDX service check
    docker-compose ps hyperdx
    netstat -an | grep -E "4317|4318|8081"

    # 3. Traces appear in UI (manual check at http://localhost:8081)

    # 4. Module boundary spans (check HyperDX UI for module names in spans)

    # 5. gRPC connection logs
    docker-compose logs hyperdx | grep -i "grpc\|4317"

    # 6. Graceful degradation (already tested in task 13)

    # 7. Documentation accurate (already reviewed in task 17)

    # 8. Health checks
    docker-compose ps --filter "status=running"
    curl http://localhost:8080/actuator/health
    ```
  - Validation checklist from design.md:
    - [ ] Application starts without errors after dependency changes
    - [ ] HyperDX service starts and exposes ports 4317, 4318, 8081
    - [ ] Traces appear in HyperDX UI within seconds of generation
    - [ ] Module boundary spans are visible in trace visualization
    - [ ] gRPC connection is established (check logs for "connected" messages)
    - [ ] Application continues functioning if HyperDX is stopped (graceful degradation)
    - [ ] Documentation accurately describes new setup
    - [ ] All docker-compose services pass health checks
  - Purpose: Final comprehensive validation that all requirements are met
  - _Requirements: 1.1-1.4, 2.1-2.5, 3.1-3.6, 4.1-4.5, 5.1-5.4, 6.1-6.4, Non-functional: All_

## Implementation Notes

**Configuration-First Approach**:
- No application code changes required
- All changes are in configuration files (Maven, properties, Docker Compose)
- Zero-code-change migration maintains existing application behavior

**Dependency Management**:
- Spring Boot Parent POM manages OpenTelemetry versions automatically
- No need to specify versions for `opentelemetry-exporter-otlp`
- Existing `micrometer-tracing-bridge-otel` remains unchanged

**Testing Strategy**:
- Unit tests: No changes needed (application code unchanged)
- Integration tests: Verify with existing test suite
- Manual testing: Focus on trace visibility and module boundaries
- Performance testing: Not required (gRPC is more efficient than HTTP)

**Rollback Plan**:
If issues arise, rollback is straightforward:
1. Restore `opentelemetry-exporter-zipkin` dependency in pom.xml
2. Restore `management.zipkin.tracing.endpoint` in application.properties
3. Restore Zipkin service in compose.yml
4. Restart application and services

**Success Criteria**:
- Application starts successfully with new configuration
- Traces visible in HyperDX UI at http://localhost:8081
- Module boundaries correctly traced in distributed traces
- All existing tests pass without modification
- Documentation accurately reflects new setup
