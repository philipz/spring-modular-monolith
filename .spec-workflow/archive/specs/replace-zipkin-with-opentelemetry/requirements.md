# Requirements Document

## Introduction

This specification defines the requirements for replacing the current Zipkin tracing infrastructure with HyperDX, an all-in-one observability platform that supports OpenTelemetry (OTel) protocol. The application will use gRPC protocol to send telemetry data to HyperDX at port 4317. This migration aligns with industry best practices for unified observability and provides a vendor-neutral approach to distributed tracing, metrics, and logging with an integrated UI for visualization and analysis.

## Alignment with Product Vision

This feature supports the technical excellence and maintainability goals outlined in product.md by:
- Adopting industry-standard observability practices with OpenTelemetry
- Enabling unified telemetry data collection for traces, metrics, and logs
- Providing flexibility to export data to multiple backends (Jaeger, Prometheus, Grafana Cloud, etc.)
- Maintaining Spring Modulith's automatic module boundary tracing capabilities
- Improving observability infrastructure for production deployments

## Requirements

### Requirement 1: Replace Zipkin Dependencies with OpenTelemetry

**User Story:** As a developer, I want to use OpenTelemetry instead of Zipkin for tracing, so that we have a vendor-neutral, industry-standard observability solution.

#### Acceptance Criteria

1. WHEN the main pom.xml is updated THEN it SHALL remove Zipkin-related dependencies (zipkin-reporter-brave, micrometer-tracing-bridge-brave)
2. WHEN the main pom.xml is updated THEN it SHALL add OpenTelemetry dependencies (opentelemetry-exporter-otlp) while keeping micrometer-tracing-bridge-otel
3. WHEN the orders/pom.xml is updated THEN it SHALL also use OpenTelemetry dependencies instead of Zipkin
4. IF the application starts THEN it SHALL not have any Zipkin dependency conflicts

### Requirement 2: Configure OpenTelemetry Exporter with gRPC Protocol

**User Story:** As a DevOps engineer, I want telemetry data sent via gRPC to HyperDX, so that we have efficient and reliable data transmission with integrated visualization.

#### Acceptance Criteria

1. WHEN application.properties is configured THEN it SHALL use management.otlp.tracing.endpoint pointing to http://hyperdx:4318/v1/traces for HTTP or grpc://hyperdx:4317 for gRPC
2. WHEN environment variables are set THEN they SHALL configure OTEL_EXPORTER_OTLP_ENDPOINT=http://hyperdx:4317 for gRPC protocol
3. WHEN environment variables are set THEN they SHALL configure OTEL_EXPORTER_OTLP_PROTOCOL=grpc
4. IF the application starts THEN the OpenTelemetry exporter SHALL successfully connect to HyperDX on port 4317
5. WHEN traces are generated THEN they SHALL be sent to HyperDX using gRPC protocol

### Requirement 3: Replace Zipkin Service with HyperDX in Docker Compose

**User Story:** As a developer running the application locally, I want HyperDX to replace Zipkin in docker-compose, so that I have an integrated observability platform with UI available.

#### Acceptance Criteria

1. WHEN compose.yml is updated THEN it SHALL remove the zipkin service definition
2. WHEN compose.yml is updated THEN it SHALL add a hyperdx service using image docker.hyperdx.io/hyperdx/hyperdx-all-in-one
3. WHEN the hyperdx service is configured THEN it SHALL expose port 4317 for gRPC, port 4318 for HTTP, and port 8081 for the UI
4. WHEN the hyperdx service is configured THEN it SHALL NOT require additional configuration files (all-in-one design)
5. WHEN monolith and orders-service environment variables are updated THEN they SHALL point to hyperdx instead of zipkin
6. IF docker-compose up is run THEN all services SHALL start successfully and connect to HyperDX

### Requirement 4: Verify HyperDX Built-in Configuration

**User Story:** As a DevOps engineer, I want to verify that HyperDX's built-in configuration receives and processes telemetry data correctly, so that traces are properly visualized in the HyperDX UI.

#### Acceptance Criteria

1. WHEN HyperDX starts THEN it SHALL automatically configure OTLP receiver for both gRPC (4317) and HTTP (4318) protocols
2. WHEN HyperDX is running THEN it SHALL provide a web UI accessible at port 8081 for trace visualization
3. WHEN the application sends trace data THEN it SHALL appear in the HyperDX UI within seconds
4. IF traces are generated THEN they SHALL be queryable and filterable in the HyperDX interface
5. WHEN HyperDX stores trace data THEN it SHALL provide built-in retention and aggregation capabilities

### Requirement 5: Update Documentation and Configuration Files

**User Story:** As a developer, I want updated documentation and configuration examples, so that I understand how to work with the new HyperDX observability platform.

#### Acceptance Criteria

1. WHEN CLAUDE.md is updated THEN it SHALL replace Zipkin references with HyperDX information
2. WHEN CLAUDE.md is updated THEN it SHALL document the HyperDX URLs (http://localhost:8081 for UI, http://localhost:4317 for gRPC, http://localhost:4318 for HTTP)
3. WHEN README.md is updated THEN it SHALL reflect the new observability stack with HyperDX
4. IF a developer reads the documentation THEN they SHALL understand how to access and use HyperDX for tracing and observability

### Requirement 6: Maintain Spring Modulith Observability Features

**User Story:** As a developer, I want Spring Modulith's automatic module tracing to continue working with OpenTelemetry, so that we don't lose module boundary visibility.

#### Acceptance Criteria

1. WHEN the application uses Spring Modulith Starter Insight THEN it SHALL continue to generate Micrometer spans for module interactions
2. WHEN module events are published THEN they SHALL be automatically traced with OpenTelemetry
3. WHEN cross-module API calls occur THEN they SHALL appear in OpenTelemetry traces
4. IF tests are run THEN module boundary tracing SHALL be verified in the telemetry data

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility Principle**: Configuration changes should be isolated to observability-related files
- **Modular Design**: OpenTelemetry integration should not impact existing module boundaries
- **Dependency Management**: Use Maven dependency management to ensure consistent OTel versions across modules
- **Clear Interfaces**: Maintain clear separation between application code and observability infrastructure

### Performance
- **Efficient Protocol**: gRPC protocol SHALL provide lower latency and better throughput compared to HTTP
- **Built-in Processing**: HyperDX SHALL use built-in batch processing to reduce network overhead
- **Sampling**: Maintain current sampling probability (1.0 for development, configurable for production)
- **Resource Usage**: OpenTelemetry integration SHALL not significantly increase application memory or CPU usage

### Security
- **Network Security**: HyperDX SHALL be accessible only within the Docker network (except UI port 8081)
- **No Sensitive Data**: Trace data SHALL not include sensitive information (passwords, tokens, PII)
- **Configuration Security**: HyperDX default configuration SHALL follow security best practices

### Reliability
- **Health Checks**: HyperDX SHALL include built-in health monitoring capabilities
- **Graceful Degradation**: Application SHALL continue functioning if HyperDX is unavailable
- **Data Retention**: HyperDX SHALL provide built-in data retention and storage management

### Usability
- **Developer Experience**: Local development setup SHALL be straightforward with docker-compose
- **Integrated UI**: HyperDX UI SHALL provide immediate visibility into trace data for troubleshooting
- **All-in-One Platform**: HyperDX SHALL provide traces, metrics, and logs in a single integrated interface
