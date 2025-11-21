# Development Guide

> **Relevant source files**
> * [.github/workflows/maven.yml](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml)
> * [.sdkmanrc](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.sdkmanrc)
> * [Taskfile.yml](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/Taskfile.yml)
> * [renovate.json](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/renovate.json)
> * [src/main/java/com/sivalabs/bookstore/catalog/domain/ProductService.java](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/catalog/domain/ProductService.java)

This document provides comprehensive guidelines for developers working on the Spring Modular Monolith codebase. It covers the development workflow, build processes, testing strategies, code quality standards, and tooling configuration.

For detailed information about specific topics:

* Building and running tests: see [Building and Testing](/philipz/spring-modular-monolith/11.1-building-and-testing)
* Code formatting and quality tools: see [Code Quality and Formatting](/philipz/spring-modular-monolith/11.2-code-quality-and-formatting)
* Writing integration tests: see [Integration Testing Strategies](/philipz/spring-modular-monolith/11.3-integration-testing-strategies)
* Creating new modules: see [Adding New Modules](/philipz/spring-modular-monolith/11.4-adding-new-modules)

For deployment and running the application locally, see [Getting Started](/philipz/spring-modular-monolith/2-getting-started).

---

## Prerequisites and Development Environment

### Required Software Versions

The project enforces specific versions of development tools to ensure consistency across development environments:

| Tool | Required Version | Configuration Source |
| --- | --- | --- |
| Java | 21 (Temurin distribution) | [.sdkmanrc L1](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.sdkmanrc#L1-L1) |
| Maven | 3.9.11 | [.sdkmanrc L2](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.sdkmanrc#L2-L2) |
| Docker | Latest stable | Required for Testcontainers and local deployment |
| Task | Latest | Optional, for simplified task execution |

**SDKMAN Configuration**: The project includes an `.sdkmanrc` file that automatically configures the correct Java and Maven versions when using SDKMAN:

```
sdk env install
sdk env
```

**Maven Wrapper**: The project includes Maven wrapper (`mvnw`/`mvnw.cmd`) to ensure consistent Maven execution without requiring system-wide installation. The GitHub Actions workflow makes the wrapper executable before use: [.github/workflows/maven.yml L40-L41](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L40-L41)

**Sources**: [.sdkmanrc L1-L3](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.sdkmanrc#L1-L3)

 [.github/workflows/maven.yml L33-L38](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L33-L38)

---

## Development Workflow Overview

```mermaid
flowchart TD

LocalDev["Developer Workstation"]
IDE["IDE<br>(IntelliJ IDEA, VS Code)"]
Task["Task Runner<br>Taskfile.yml"]
MvnWrapper["Maven Wrapper<br>mvnw / mvnw.cmd"]
Format["spotless:apply<br>Code Formatting<br>Palantir Java Format"]
Compile["compile<br>Java Compilation<br>+ protobuf:compile"]
Test["test<br>Unit Tests<br>JUnit 5"]
Verify["verify<br>Integration Tests<br>+ Testcontainers"]
Package["package<br>JAR Creation"]
BuildImage["spring-boot:build-image<br>Docker Image"]
DockerCompose["Docker Compose<br>compose.yml"]
Kind["Kind Cluster<br>k8s/manifests/"]
GHA["GitHub Actions<br>maven.yml workflow"]
DependencySnap["Dependency Submission<br>GitHub Advanced Security"]

IDE --> Task
IDE --> MvnWrapper
Task --> Format
Task --> DockerCompose
Task --> Kind
BuildImage --> DockerCompose
BuildImage --> Kind
MvnWrapper --> Format
MvnWrapper --> Verify
LocalDev -->|"git push"| GHA
GHA --> MvnWrapper
MvnWrapper --> DependencySnap

subgraph CI/CD ["CI/CD"]
    GHA
    DependencySnap
end

subgraph subGraph3 ["Deployment Targets"]
    DockerCompose
    Kind
end

subgraph subGraph2 ["Build and Quality"]
    Format
    Compile
    Test
    Verify
    Package
    BuildImage
    Format --> Compile
    Compile --> Test
    Test --> Verify
    Verify --> Package
    Package --> BuildImage
end

subgraph subGraph1 ["Task Execution Layer"]
    Task
    MvnWrapper
    Task --> MvnWrapper
end

subgraph subGraph0 ["Development Environment"]
    LocalDev
    IDE
    LocalDev --> IDE
end
```

**Workflow Description**:

1. **Local Development**: Developers write code in their IDE and use either Task or Maven wrapper directly
2. **Code Formatting**: `spotless:apply` formats code according to Palantir Java Format standards
3. **Build Pipeline**: Maven executes phases sequentially (compile → test → verify → package)
4. **Image Creation**: `spring-boot:build-image` creates OCI-compliant Docker images using Cloud Native Buildpacks
5. **Local Deployment**: Docker Compose or Kind deploys the built images for testing
6. **CI/CD**: GitHub Actions runs the same Maven build on push/PR and submits dependency snapshots

**Sources**: [Taskfile.yml L1-L64](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/Taskfile.yml#L1-L64)

 [.github/workflows/maven.yml L1-L48](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L1-L48)

---

## Task-Based Development Commands

The project uses [Taskfile](https://taskfile.dev) to provide platform-independent task execution. The `Taskfile.yml` abstracts differences between Windows and Unix-like systems.

### Task Configuration

```mermaid
flowchart TD

KindCreate["task kind_create<br>→ kind-cluster.sh create"]
KindDestroy["task kind_destroy<br>→ kind-cluster.sh destroy"]
K8sDeploy["task k8s_deploy<br>→ kind load + kubectl apply"]
K8sUndeploy["task k8s_undeploy<br>→ kubectl delete"]
GOOS["GOOS Variable<br>OS detection"]
MVNW["MVNW<br>mvnw.cmd (Windows)<br>./mvnw (Unix)"]
SLEEP["SLEEP_CMD<br>timeout (Windows)<br>sleep (Unix)"]
Default["task default<br>→ task test"]
Test["task test<br>deps: format<br>→ mvnw clean verify"]
Format["task format<br>→ mvnw spotless:apply"]
BuildImage["task build_image<br>→ mvnw spring-boot:build-image"]
Start["task start<br>deps: build_image<br>→ docker compose up"]
Stop["task stop<br>→ docker compose stop + rm"]
Restart["task restart<br>→ stop + sleep + start"]

GOOS --> MVNW
GOOS --> SLEEP
MVNW --> Test
MVNW --> Format
MVNW --> BuildImage
BuildImage -->|"dependency"| Start
SLEEP -->|"used by"| Restart

subgraph subGraph3 ["Deployment Tasks"]
    Start
    Stop
    Restart
end

subgraph subGraph2 ["Primary Tasks"]
    Default
    Test
    Format
    BuildImage
    Format -->|"dependency"| Test
end

subgraph subGraph1 ["Platform-Specific Commands"]
    MVNW
    SLEEP
end

subgraph subGraph0 ["Platform Detection"]
    GOOS
end

subgraph subGraph4 ["Kubernetes Tasks"]
    KindCreate
    KindDestroy
    K8sDeploy
    K8sUndeploy
end
```

### Common Task Commands

| Command | Description | Dependencies |
| --- | --- | --- |
| `task` or `task default` | Runs format + test | `task test` |
| `task test` | Formats code and runs full test suite | `task format` |
| `task format` | Applies Spotless code formatting | None |
| `task build_image` | Builds Docker image with Spring Boot | None |
| `task start` | Builds image and starts Docker Compose stack | `task build_image` |
| `task stop` | Stops and removes Docker Compose containers | None |
| `task restart` | Full restart with 5-second delay | `task stop`, `task start` |
| `task kind_create` | Creates local Kind Kubernetes cluster | None |
| `task kind_destroy` | Destroys Kind cluster | None |
| `task k8s_deploy` | Loads image to Kind and deploys manifests | None |
| `task k8s_undeploy` | Removes Kubernetes resources | None |

**Platform Abstraction**: The Taskfile detects the operating system and selects the appropriate commands:

* **Maven Wrapper**: [Taskfile.yml L5](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/Taskfile.yml#L5-L5)  selects `mvnw.cmd` on Windows, `./mvnw` otherwise
* **Sleep Command**: [Taskfile.yml L6](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/Taskfile.yml#L6-L6)  uses `timeout` on Windows, `sleep` on Unix
* **Compose File**: [Taskfile.yml L7](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/Taskfile.yml#L7-L7)  references `compose.yml` consistently

**Sources**: [Taskfile.yml L1-L64](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/Taskfile.yml#L1-L64)

---

## Maven Build Process

### Build Command Structure

The Maven wrapper provides the core build functionality. The typical development build command is:

```
./mvnw clean verify
```

This executes the following phases:

1. **clean**: Removes `target/` directory
2. **validate**: Validates project structure
3. **compile**: Compiles main source code and generates Protocol Buffers
4. **test**: Runs unit tests with JUnit 5
5. **package**: Creates JAR file
6. **verify**: Runs integration tests with Testcontainers

### Maven Lifecycle Phases

```mermaid
flowchart TD

Clean["clean<br>Remove target/"]
Validate["validate<br>Check project structure"]
GenerateProto["protobuf:compile<br>Generate Java from .proto"]
CompileSrc["compile<br>Compile src/main/java"]
CompileTest["test-compile<br>Compile src/test/java"]
UnitTest["test<br>Run @Test<br>Surefire Plugin"]
IntTest["integration-test<br>Run @IntegrationTest<br>Failsafe Plugin"]
Verify["verify<br>Validate integration tests<br>passed"]
Package["package<br>Create JAR"]
BuildImage["spring-boot:build-image<br>Create Docker image<br>(optional)"]

Validate --> GenerateProto
CompileTest --> UnitTest
UnitTest --> Package
Package --> IntTest

subgraph Packaging ["Packaging"]
    Package
    BuildImage
    Package -->|"separate goal"| BuildImage
end

subgraph Testing ["Testing"]
    UnitTest
    IntTest
    Verify
    IntTest --> Verify
end

subgraph Compilation ["Compilation"]
    GenerateProto
    CompileSrc
    CompileTest
    GenerateProto --> CompileSrc
    CompileSrc --> CompileTest
end

subgraph Pre-Build ["Pre-Build"]
    Clean
    Validate
    Clean --> Validate
end
```

**Key Maven Goals**:

| Goal | Purpose | When Used |
| --- | --- | --- |
| `clean` | Delete build artifacts | Before fresh build |
| `compile` | Compile Java source | Automatic in build |
| `test` | Run unit tests | Automatic in verify |
| `verify` | Run integration tests | Full test execution |
| `package` | Create JAR | Before deployment |
| `spring-boot:build-image` | Build Docker image | Local/CI deployment |
| `spotless:apply` | Format code | Before commit |
| `protobuf:compile` | Generate gRPC stubs | Automatic in compile |

**CI Build Command**: GitHub Actions uses the no-transfer-progress flag for cleaner logs:

```
./mvnw -ntp verify
```

The `-ntp` flag suppresses progress output during dependency downloads: [.github/workflows/maven.yml L44](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L44-L44)

**Sources**: [.github/workflows/maven.yml L40-L44](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L40-L44)

 [Taskfile.yml L16](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/Taskfile.yml#L16-L16)

---

## Continuous Integration Pipeline

### GitHub Actions Workflow

The project uses GitHub Actions for continuous integration. The workflow is triggered on:

* **Push events** to `main` and `package-by-*` branches (excluding documentation changes)
* **Pull requests** to `main` affecting Java source or build configuration

```mermaid
flowchart TD

PushMain["git push to main"]
PushFeature["git push to package-by-*"]
PullRequest["Pull Request to main"]
IgnoreDocs["Ignore:<br>README.md, LICENSE,<br>Taskfile.yml, k8s/**"]
IncludeSrc["Include:<br>pom.xml, src/**,<br>maven.yml"]
Checkout["actions/checkout@v5<br>Clone repository"]
SetupJava["actions/setup-java@v5<br>Java 21 Temurin<br>Maven cache"]
MakeExec["chmod +x mvnw<br>Make wrapper executable"]
Build["./mvnw -ntp verify<br>Compile + Test + Verify"]
DepSnap["maven-dependency-submission-action@v5<br>Submit dependencies<br>(push only)"]
TestResults["Test Results<br>JUnit XML"]
Coverage["Test Coverage<br>Jacoco Reports"]
SecurityScan["Dependency Graph<br>GitHub Security"]

PushMain --> IgnoreDocs
PushFeature --> IgnoreDocs
PullRequest --> IncludeSrc
IgnoreDocs --> Checkout
IncludeSrc --> Checkout
Build --> TestResults
Build --> Coverage
DepSnap --> SecurityScan

subgraph Outputs ["Outputs"]
    TestResults
    Coverage
    SecurityScan
end

subgraph subGraph2 ["Build Job (ubuntu-latest)"]
    Checkout
    SetupJava
    MakeExec
    Build
    DepSnap
    Checkout --> SetupJava
    SetupJava --> MakeExec
    MakeExec --> Build
    Build -->|"on push only"| DepSnap
end

subgraph subGraph1 ["Path Filters"]
    IgnoreDocs
    IncludeSrc
end

subgraph subGraph0 ["Trigger Events"]
    PushMain
    PushFeature
    PullRequest
end
```

**Workflow Configuration Details**:

1. **Path Exclusions** (push events): [.github/workflows/maven.yml L8-L15](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L8-L15) * `.gitignore`, `.sdkmanrc`, `README.md`, `LICENSE` * `Taskfile.yml`, `renovate.json` * `k8s/**` (Kubernetes manifests)
2. **Path Inclusions** (pull requests): [.github/workflows/maven.yml L19-L23](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L19-L23) * `pom.xml` and nested POM files * `src/**` (all source code) * `.github/workflows/maven.yml` (workflow itself)
3. **Java Setup**: [.github/workflows/maven.yml L33-L38](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L33-L38) * Java 21 from Eclipse Temurin distribution * Maven dependency caching enabled * Required for builds and tests
4. **Permissions**: [.github/workflows/maven.yml L28-L29](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L28-L29) * `contents: write` required for dependency submission to GitHub's dependency graph
5. **Dependency Submission**: [.github/workflows/maven.yml L46-L48](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L46-L48) * Only runs on push events (not PRs) * Submits Maven dependency tree to GitHub Advanced Security * Enables Dependabot security alerts

**Sources**: [.github/workflows/maven.yml L1-L48](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L1-L48)

---

## Development Best Practices

### Code Organization Patterns

The codebase follows Spring Modulith conventions with specific patterns visible in service implementations:

**1. Optional Dependency Injection**: Services gracefully handle missing dependencies, particularly for caching infrastructure: [src/main/java/com/sivalabs/bookstore/catalog/domain/ProductService.java L24](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/catalog/domain/ProductService.java#L24-L24)

```
ProductService(ProductRepository repo, 
               @Autowired(required = false) ProductCacheService productCacheService) {
    this.repo = repo;
    this.productCacheService = productCacheService;
    // Log cache availability status
}
```

**2. Circuit Breaker Awareness**: Services check cache availability before use: [src/main/java/com/sivalabs/bookstore/catalog/domain/ProductService.java L40-L42](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/catalog/domain/ProductService.java#L40-L42)

```java
private boolean isCacheAvailable() {
    return productCacheService != null && !productCacheService.isCircuitBreakerOpen();
}
```

**3. Fallback Patterns**: Services implement graceful degradation when cache is unavailable: [src/main/java/com/sivalabs/bookstore/catalog/domain/ProductService.java L54-L89](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/catalog/domain/ProductService.java#L54-L89)

```
// Try cache first if available
if (isCacheAvailable()) {
    Optional<ProductEntity> cachedProduct = productCacheService.findByProductCode(code);
    if (cachedProduct.isPresent()) {
        return cachedProduct;
    }
}
// Fallback to database query
Optional<ProductEntity> product = repo.findByCode(code);
```

**4. Defensive Exception Handling**: Cache operations are wrapped in try-catch to prevent failures from affecting business logic: [src/main/java/com/sivalabs/bookstore/catalog/domain/ProductService.java L64-L69](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/catalog/domain/ProductService.java#L64-L69)

**Sources**: [src/main/java/com/sivalabs/bookstore/catalog/domain/ProductService.java L1-L90](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/src/main/java/com/sivalabs/bookstore/catalog/domain/ProductService.java#L1-L90)

---

## Dependency Management

### Renovate Configuration

The project uses Renovate for automated dependency updates. Configuration is minimal, extending recommended defaults: [renovate.json L1-L6](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/renovate.json#L1-L6)

```json
{
    "$schema": "https://docs.renovatebot.com/renovate-schema.json",
    "extends": [
        "config:recommended"
    ]
}
```

Renovate automatically:

* Creates pull requests for dependency updates
* Groups related updates together
* Respects semantic versioning rules
* Triggers GitHub Actions builds for validation

### Dependency Submission

The GitHub Actions workflow submits the complete Maven dependency tree to GitHub's dependency graph on every push to main: [.github/workflows/maven.yml L46-L48](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L46-L48)

 This enables:

* **Security Alerts**: Dependabot alerts for known vulnerabilities
* **Dependency Insights**: Visual dependency graph in repository
* **Supply Chain Security**: Track transitive dependencies
* **License Compliance**: Identify dependency licenses

**Sources**: [renovate.json L1-L6](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/renovate.json#L1-L6)

 [.github/workflows/maven.yml L46-L48](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L46-L48)

---

## Quick Reference

### Essential Commands

```markdown
# Format and test (full verification)
task test

# Format code only
task format

# Build Docker image
task build_image

# Start full stack locally
task start

# Restart services (useful after code changes)
task restart

# Maven commands (direct)
./mvnw clean verify
./mvnw spotless:apply
./mvnw spring-boot:build-image -DskipTests
```

### Debugging Tips

1. **Cache Issues**: Check Hazelcast circuit breaker status in logs
2. **Test Failures**: Review Testcontainers logs in `target/` directory
3. **Build Failures**: Ensure Java 21 is active (`java -version`)
4. **Format Issues**: Run `task format` before committing
5. **Docker Issues**: Check Docker daemon is running for image builds

### Related Documentation

* For detailed testing strategies: see [Integration Testing Strategies](/philipz/spring-modular-monolith/11.3-integration-testing-strategies)
* For code formatting rules: see [Code Quality and Formatting](/philipz/spring-modular-monolith/11.2-code-quality-and-formatting)
* For creating new modules: see [Adding New Modules](/philipz/spring-modular-monolith/11.4-adding-new-modules)
* For deployment procedures: see [Docker Compose Deployment](/philipz/spring-modular-monolith/10.1-docker-compose-deployment)
* For running locally: see [Running Locally with Docker Compose](/philipz/spring-modular-monolith/2.2-running-locally-with-docker-compose)

**Sources**: [Taskfile.yml L1-L64](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/Taskfile.yml#L1-L64)

 [.github/workflows/maven.yml L1-L48](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.github/workflows/maven.yml#L1-L48)

 [.sdkmanrc L1-L3](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/.sdkmanrc#L1-L3)