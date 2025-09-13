# spring-modular-monolith
An e-commerce application following Modular Monolith architecture using [Spring Modulith](https://spring.io/projects/spring-modulith).
The goal of this application is to demonstrate various features of Spring Modulith with a practical application.

![bookstore-modulith.png](docs/bookstore-modulith.png)

This application follows modular monolith architecture with the following modules:

* **Common:** This module contains the code that is shared by all modules, including distributed caching with Hazelcast.
* **Catalog:** This module manages the catalog of products and store data in `catalog` schema.
* **Orders:** This module implements the order management and store the data in `orders` schema.
* **Inventory:** This module implements the inventory management and store the data in `inventory` schema.
* **Notifications:** This module handles the events published by other modules and sends notifications to the interested parties.

**Distributed Caching:**
* The application uses **Hazelcast** for distributed caching to improve performance across all modules.
* **Hazelcast Management Center** provides real-time monitoring and management of cache clusters.
* Cache configurations include TTL (Time To Live), eviction policies, and backup strategies for high availability.

**Goals:**
* Implement each module as independently as possible.
* Prefer event-driven communication instead of direct module dependency wherever applicable.
* Store data managed by each module in an isolated manner by using different schema or database.
* Each module should be testable by loading only module-specific components.

**Module communication:**

* **Common** module is an OPEN module that can be used by other modules.
* **Orders** module invokes the **Catalog** module public API to validate the order details
* When an Order is successfully created, **Orders** module publishes **"OrderCreatedEvent"**
* The **"OrderCreatedEvent"** will also be published to external broker like RabbitMQ. Other applications may consume and process those events.
* **Inventory** module consumes "OrderCreatedEvent" and updates the stock level for the products.
* **Notifications** module consumes "OrderCreatedEvent" and sends an order confirmation email to the customer.

## Prerequisites

**Required Tools:**
- Java 21+ (recommended 24, tested)
- Docker and Docker Compose
- Maven Wrapper (included)
- Task runner
- IDE: Recommended [IntelliJ IDEA](https://www.jetbrains.com/idea/)

**Installation Guide:**

Install JDK and related tools using [SDKMAN](https://sdkman.io/):

```shell
$ curl -s "https://get.sdkman.io" | bash
$ source "$HOME/.sdkman/bin/sdkman-init.sh"
$ sdk install java 24.0.1-tem
$ sdk install maven
```

Install Task runner:

```shell
$ brew install go-task
# or
$ go install github.com/go-task/task/v3/cmd/task@latest
```

Verify installation:

```shell
$ java -version
$ docker info
$ docker compose version
$ task --version
```

## Quick Start

### 1. Docker Compose (Recommended)

```shell
# Build and start all services
$ task start

# Stop services
$ task stop

# Restart (rebuild images)
$ task restart
```

### 2. Local Development

```shell
# Run tests
$ task test

# Format code
$ task format

# Build Docker image
$ task build_image

# Local run (requires external PostgreSQL and RabbitMQ)
$ ./mvnw spring-boot:run
```

### 3. Running Without Docker

Provide PostgreSQL and RabbitMQ locally, override via environment variables:

```shell
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bookstore
export SPRING_RABBITMQ_HOST=localhost
./mvnw spring-boot:run
```

### Application URLs

**Local Development:**
- Application: http://localhost:8080
- Actuator: http://localhost:8080/actuator
- Modulith Info: http://localhost:8080/actuator/modulith
- RabbitMQ Admin: http://localhost:15672 (guest/guest)
- Zipkin Tracing: http://localhost:9411
- Hazelcast Management Center: http://localhost:38080

## Kubernetes Deployment

### Install Required Tools

```shell
$ brew install kubectl
$ brew install kind
```

Documentation:
- [kubectl Installation Guide](https://kubernetes.io/docs/tasks/tools/)
- [kind Installation Guide](https://kind.sigs.k8s.io/docs/user/quick-start/)

### Deployment Steps

```shell
# Create KinD cluster
$ task kind_create

# Deploy app to K8s cluster
$ task k8s_deploy

# Undeploy app
$ task k8s_undeploy

# Destroy KinD cluster
$ task kind_destroy
```

**K8s Environment URLs:**
- Application: http://localhost:30090
- RabbitMQ Admin: http://localhost:30091 (guest/guest)
- Zipkin Tracing: http://localhost:30092
- Hazelcast Management Center: http://localhost:30093

## Development Guide

### Code Structure

```
src/main/java/com/sivalabs/bookstore/
├── common/          # Shared module (open module)
├── catalog/         # Product catalog module
├── orders/          # Order management module
├── inventory/       # Inventory management module
├── notifications/   # Notification module
└── config/          # Configuration files
```

### Database Migration

- Location: `src/main/resources/db/migration/`
- Uses Liquibase for version control
- Each module uses independent schema

### Module Boundary Guidelines

- Respect `@ApplicationModule` defined boundaries
- Cross-module access requires explicit APIs (e.g., `catalog.ProductApi`)
- Inter-module communication prioritizes event-driven patterns

### Troubleshooting

**Port Conflicts:**
- Stop conflicting services or modify port mappings in `compose.yml`

**Hazelcast/Cache Issues:**
- Tune `bookstore.cache.*` settings
- Check `/actuator/health` and `/actuator/modulith`

**Tracing Not Visible:**
- Confirm Zipkin service is running
- Verify tracing endpoint configuration
