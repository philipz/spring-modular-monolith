# RabbitMQ Kubernetes Deployment

> **Relevant source files**
> * [k8s/manifests/rabbitmq.yaml](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml)

## Purpose and Scope

This document details the Kubernetes deployment configuration for RabbitMQ as the external event bus in the Spring Modular Monolith system. It covers the Service and Deployment manifests, port configurations, default credentials, and how other services discover and connect to RabbitMQ within the Kubernetes cluster.

For information about RabbitMQ's role in event-driven communication and message routing, see [Messaging with RabbitMQ](/philipz/spring-modular-monolith/6-messaging-with-rabbitmq). For the Docker Compose deployment configuration, see [Docker Compose Deployment](/philipz/spring-modular-monolith/10.1-docker-compose-deployment).

---

## Kubernetes Resources Overview

The RabbitMQ deployment consists of two primary Kubernetes resources defined in [k8s/manifests/rabbitmq.yaml L1-L49](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L1-L49)

:

| Resource Type | Resource Name | Purpose |
| --- | --- | --- |
| `Service` | `spring-modular-monolith-rabbitmq-svc` | Exposes RabbitMQ to other services in the cluster and externally via NodePort |
| `Deployment` | `spring-modular-monolith-rabbitmq-deployment` | Manages the RabbitMQ pod lifecycle with a single replica |

### RabbitMQ Deployment Architecture

```mermaid
flowchart TD

Service["Service<br>spring-modular-monolith-rabbitmq-svc<br>Type: NodePort"]
Deployment["Deployment<br>spring-modular-monolith-rabbitmq-deployment<br>Strategy: Recreate"]
Pod["Pod<br>app: spring-modular-monolith-rabbitmq-pod"]
Container["Container: rabbitmq<br>Image: rabbitmq:4.1.3-management-alpine"]
Monolith["spring-modular-monolith<br>Publishes events to RabbitMQ"]
OrdersSvc["orders-service<br>Publishes events to RabbitMQ"]
AMQPMod["amqp-modulith<br>Consumes events from RabbitMQ"]
Browser["Web Browser<br>Management UI Access"]
K8sNode["Kubernetes Node<br>NodePort: 30091"]

K8sNode --> Service

subgraph subGraph3 ["External Access"]
    Browser
    K8sNode
    Browser --> K8sNode
end

subgraph subGraph2 ["Kubernetes Cluster"]
    Monolith --> Service
    OrdersSvc --> Service
    AMQPMod --> Service

subgraph subGraph1 ["Application Pods"]
    Monolith
    OrdersSvc
    AMQPMod
end

subgraph subGraph0 ["RabbitMQ Resources"]
    Service
    Deployment
    Pod
    Container
    Deployment --> Pod
    Pod --> Container
    Service --> Pod
    Service --> Container
    Service --> Container
end
end
```

**Sources:** [k8s/manifests/rabbitmq.yaml L1-L49](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L1-L49)

---

## Service Configuration

The `spring-modular-monolith-rabbitmq-svc` Service exposes RabbitMQ both internally within the cluster and externally via NodePort.

### Service Specification

[k8s/manifests/rabbitmq.yaml L1-L18](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L1-L18)

 defines the Service with the following configuration:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: spring-modular-monolith-rabbitmq-svc
spec:
  type: NodePort
  selector:
    app: spring-modular-monolith-rabbitmq-pod
  ports: [...]
```

| Field | Value | Description |
| --- | --- | --- |
| `type` | `NodePort` | Exposes the Service on each Node's IP at a static port, enabling external access |
| `selector` | `app: spring-modular-monolith-rabbitmq-pod` | Routes traffic to pods with this label |

### Port Mappings

The Service exposes two ports with different access patterns:

| Port Name | Service Port | Target Port | Node Port | Protocol | Purpose |
| --- | --- | --- | --- | --- | --- |
| `rabbitmq-port-mapping` | 5672 | 5672 | (auto-assigned) | TCP | AMQP protocol for message publishing/consumption |
| `rabbitmq--gui-port-mapping` | 15672 | 15672 | 30091 | TCP | RabbitMQ Management UI for monitoring and administration |

### Internal Service Discovery

Other services in the cluster connect to RabbitMQ using the Service's DNS name:

```
spring-modular-monolith-rabbitmq-svc.default.svc.cluster.local:5672
```

Or the short form within the same namespace:

```
spring-modular-monolith-rabbitmq-svc:5672
```

### Port Configuration Diagram

```mermaid
flowchart TD

Port5672["Port 5672<br>rabbitmq-port-mapping<br>AMQP"]
Port15672["Port 15672<br>rabbitmq--gui-port-mapping<br>Management UI<br>NodePort: 30091"]
ContainerPort5672["containerPort: 5672<br>AMQP Server"]
ContainerPort15672["containerPort: 15672<br>Management UI"]
InternalClient["In-Cluster Services<br>spring-modular-monolith<br>orders-service<br>amqp-modulith"]
ExternalClient["External Browser<br>NodeIP:30091"]

InternalClient --> Port5672
Port5672 --> ContainerPort5672
ExternalClient --> Port15672
Port15672 --> ContainerPort15672

subgraph Clients ["Clients"]
    InternalClient
    ExternalClient
end

subgraph subGraph1 ["Pod Container Ports"]
    ContainerPort5672
    ContainerPort15672
end

subgraph subGraph0 ["Service: spring-modular-monolith-rabbitmq-svc"]
    Port5672
    Port15672
end
```

**Sources:** [k8s/manifests/rabbitmq.yaml L10-L18](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L10-L18)

---

## Deployment Configuration

The `spring-modular-monolith-rabbitmq-deployment` Deployment manages the RabbitMQ pod lifecycle.

### Deployment Strategy

[k8s/manifests/rabbitmq.yaml L20-L29](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L20-L29)

 configures the Deployment with:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-modular-monolith-rabbitmq-deployment
spec:
  selector:
    matchLabels:
      app: spring-modular-monolith-rabbitmq-pod
  strategy:
    type: Recreate
```

| Field | Value | Rationale |
| --- | --- | --- |
| `strategy.type` | `Recreate` | Ensures the old pod is terminated before the new pod is created, preventing port conflicts and ensuring clean state transitions |
| `selector.matchLabels` | `app: spring-modular-monolith-rabbitmq-pod` | Matches pods created by this Deployment |

The `Recreate` strategy is appropriate for stateful services like RabbitMQ where running multiple instances simultaneously during updates could cause consistency issues.

### Container Configuration

[k8s/manifests/rabbitmq.yaml L35-L47](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L35-L47)

 defines the RabbitMQ container:

```yaml
containers:
  - name: rabbitmq
    image: "rabbitmq:4.1.3-management-alpine"
    ports:
      - name: rabbitmq
        containerPort: 5672
      - name: rabbitmq-admin
        containerPort: 15672
    env:
      - name: RABBITMQ_DEFAULT_USER
        value: guest
      - name: RABBITMQ_DEFAULT_PASS
        value: guest
```

| Configuration | Value | Description |
| --- | --- | --- |
| Image | `rabbitmq:4.1.3-management-alpine` | Official RabbitMQ image with management plugin enabled, based on Alpine Linux |
| Container Name | `rabbitmq` | Pod-local container identifier |
| AMQP Port | 5672 | Standard AMQP protocol port for message operations |
| Management Port | 15672 | HTTP port for RabbitMQ Management UI |

### Environment Variables

The Deployment configures RabbitMQ with default credentials:

| Environment Variable | Value | Purpose |
| --- | --- | --- |
| `RABBITMQ_DEFAULT_USER` | `guest` | Default username for RabbitMQ authentication |
| `RABBITMQ_DEFAULT_PASS` | `guest` | Default password for RabbitMQ authentication |

**⚠️ Security Note:** The default `guest`/`guest` credentials are suitable for development and testing environments only. For production deployments, these should be replaced with strong credentials stored in Kubernetes Secrets.

**Sources:** [k8s/manifests/rabbitmq.yaml L35-L47](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L35-L47)

---

## RabbitMQ Management UI Access

The RabbitMQ Management UI provides a web-based interface for monitoring queues, exchanges, bindings, and message rates.

### Accessing the Management UI in Kind

When running on a Kind (Kubernetes in Docker) cluster:

1. **Find the Node IP:** ``` kubectl get nodes -o wide ```
2. **Access the UI:** ```yaml http://<node-ip>:30091 ```
3. **Login Credentials:** * Username: `guest` * Password: `guest`

### Management UI Features

The RabbitMQ Management UI at port 30091 provides:

* **Overview Dashboard**: Connection counts, message rates, queue statistics
* **Connections Tab**: Active AMQP connections from application services
* **Channels Tab**: Open channels for message operations
* **Exchanges Tab**: Configured exchanges for event routing
* **Queues Tab**: Queue depths, consumer counts, message rates
* **Admin Tab**: User management, virtual host configuration

### Service Connection Flow

```mermaid
flowchart TD

NodePort["NodePort 30091<br>External Access Point"]
Service["Service<br>spring-modular-monolith-rabbitmq-svc<br>Port 15672"]
Container["Container: rabbitmq<br>Port 15672<br>Management UI"]
Browser["Web Browser"]

Browser --> NodePort
NodePort --> Service
Service --> Container
Container --> Browser

subgraph Pod ["Pod"]
    Container
end

subgraph subGraph1 ["Service Layer"]
    Service
end

subgraph subGraph0 ["Kubernetes Node"]
    NodePort
end
```

**Sources:** [k8s/manifests/rabbitmq.yaml L14-L18](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L14-L18)

 [k8s/manifests/rabbitmq.yaml L41-L42](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L41-L42)

---

## Integration with Application Services

Application services connect to RabbitMQ using the Service's cluster DNS name.

### Connection Configuration

Services use the following connection string format:

```yaml
amqp://guest:guest@spring-modular-monolith-rabbitmq-svc:5672
```

Or broken down:

| Component | Value |
| --- | --- |
| Protocol | `amqp://` |
| Username | `guest` |
| Password | `guest` |
| Host | `spring-modular-monolith-rabbitmq-svc` (Service DNS name) |
| Port | `5672` |

### Service-to-RabbitMQ Communication Pattern

```mermaid
flowchart TD

Monolith["spring-modular-monolith<br>Environment:<br>SPRING_RABBITMQ_HOST=<br>spring-modular-monolith-rabbitmq-svc"]
OrdersSvc["orders-service<br>Environment:<br>SPRING_RABBITMQ_HOST=<br>spring-modular-monolith-rabbitmq-svc"]
AMQPMod["amqp-modulith<br>Environment:<br>SPRING_RABBITMQ_HOST=<br>spring-modular-monolith-rabbitmq-svc"]
Service["Service<br>spring-modular-monolith-rabbitmq-svc<br>ClusterIP + NodePort<br>Port: 5672"]
Pod["Pod<br>Label: app=spring-modular-monolith-rabbitmq-pod"]
Container["Container: rabbitmq<br>Port 5672<br>Credentials: guest/guest"]

Monolith --> Service
OrdersSvc --> Service
AMQPMod --> Service
Service --> Pod

subgraph subGraph2 ["RabbitMQ Pod"]
    Pod
    Container
    Pod --> Container
end

subgraph subGraph1 ["RabbitMQ Service"]
    Service
end

subgraph subGraph0 ["Application Pods"]
    Monolith
    OrdersSvc
    AMQPMod
end
```

### Connection Details

Services configure RabbitMQ connection via Spring Boot properties:

```
spring.rabbitmq.host=spring-modular-monolith-rabbitmq-svc
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

These properties are typically set via environment variables in the service Deployment manifests:

```yaml
env:
  - name: SPRING_RABBITMQ_HOST
    value: spring-modular-monolith-rabbitmq-svc
  - name: SPRING_RABBITMQ_PORT
    value: "5672"
  - name: SPRING_RABBITMQ_USERNAME
    value: guest
  - name: SPRING_RABBITMQ_PASSWORD
    value: guest
```

**Sources:** [k8s/manifests/rabbitmq.yaml L1-L49](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L1-L49)

---

## Deployment Lifecycle

### Deploying RabbitMQ

Apply the manifest to create the RabbitMQ resources:

```
kubectl apply -f k8s/manifests/rabbitmq.yaml
```

### Verifying the Deployment

Check the Service:

```
kubectl get service spring-modular-monolith-rabbitmq-svc
```

Expected output:

```
NAME                                  TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)                          AGE
spring-modular-monolith-rabbitmq-svc  NodePort   10.96.x.x       <none>        5672:xxxxx/TCP,15672:30091/TCP   1m
```

Check the Deployment:

```
kubectl get deployment spring-modular-monolith-rabbitmq-deployment
```

Expected output:

```
NAME                                       READY   UP-TO-DATE   AVAILABLE   AGE
spring-modular-monolith-rabbitmq-deployment   1/1     1            1           1m
```

Check the Pod:

```
kubectl get pods -l app=spring-modular-monolith-rabbitmq-pod
```

Expected output:

```
NAME                                                      READY   STATUS    RESTARTS   AGE
spring-modular-monolith-rabbitmq-deployment-xxxxx-xxxxx   1/1     Running   0          1m
```

### Viewing RabbitMQ Logs

```
kubectl logs -l app=spring-modular-monolith-rabbitmq-pod -f
```

This streams logs from the RabbitMQ container, showing startup messages, connection events, and queue operations.

**Sources:** [k8s/manifests/rabbitmq.yaml L1-L49](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L1-L49)

---

## Resource Specifications

### Complete Resource Summary

| Aspect | Specification |
| --- | --- |
| **Service** |  |
| Name | `spring-modular-monolith-rabbitmq-svc` |
| Type | `NodePort` |
| AMQP Port | 5672 (TCP) |
| Management Port | 15672 (TCP, NodePort 30091) |
| Selector | `app: spring-modular-monolith-rabbitmq-pod` |
| **Deployment** |  |
| Name | `spring-modular-monolith-rabbitmq-deployment` |
| Strategy | `Recreate` |
| Replicas | 1 (default) |
| Pod Label | `app: spring-modular-monolith-rabbitmq-pod` |
| **Container** |  |
| Name | `rabbitmq` |
| Image | `rabbitmq:4.1.3-management-alpine` |
| AMQP Container Port | 5672 |
| Management Container Port | 15672 |
| Default User | `guest` |
| Default Password | `guest` |

### Image Details

The `rabbitmq:4.1.3-management-alpine` image includes:

* **RabbitMQ Version:** 4.1.3
* **Base Image:** Alpine Linux (minimal footprint)
* **Management Plugin:** Pre-enabled for web UI access
* **Plugins Included:** Management, Management Agent, Web STOMP, Web MQTT
* **Image Size:** Approximately 200MB (Alpine-based)

**Sources:** [k8s/manifests/rabbitmq.yaml L37](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L37-L37)

---

## Persistent Storage Considerations

The current configuration deploys RabbitMQ **without persistent storage**. Messages and queue definitions are stored in-memory and will be lost when the pod is deleted or crashes.

### Implications

| Scenario | Impact |
| --- | --- |
| Pod restart | All messages lost, queues recreated |
| Node failure | All messages lost, queues recreated |
| Deployment update | All messages lost, queues recreated |

### Adding Persistence (Optional)

To add persistence, modify the Deployment to include a `PersistentVolumeClaim`:

```yaml
spec:
  template:
    spec:
      containers:
        - name: rabbitmq
          volumeMounts:
            - name: rabbitmq-data
              mountPath: /var/lib/rabbitmq
      volumes:
        - name: rabbitmq-data
          persistentVolumeClaim:
            claimName: rabbitmq-pvc
```

This requires creating a `PersistentVolumeClaim` with appropriate storage class and capacity.

**Note:** The current configuration is suitable for development and testing environments where message durability is not critical. For production deployments, persistent storage and potentially a StatefulSet (for clustering) should be considered.

**Sources:** [k8s/manifests/rabbitmq.yaml L20-L47](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L20-L47)

---

## Summary

The RabbitMQ Kubernetes deployment provides:

1. **Service Discovery:** Cluster-internal DNS name (`spring-modular-monolith-rabbitmq-svc`) for application services to connect via AMQP
2. **External Access:** NodePort 30091 for accessing the Management UI from outside the cluster
3. **Single Instance:** One replica using `Recreate` strategy to prevent state conflicts
4. **Default Credentials:** `guest`/`guest` for development convenience (should be changed for production)
5. **Management Plugin:** Pre-enabled for monitoring and administration via web UI
6. **No Persistence:** In-memory storage suitable for development; messages are lost on pod restart

For information about how application services publish and consume events through RabbitMQ, see [Event-Driven Architecture](/philipz/spring-modular-monolith/3.3-event-driven-architecture) and [Inter-Module Communication](/philipz/spring-modular-monolith/3.2-inter-module-communication).

**Sources:** [k8s/manifests/rabbitmq.yaml L1-L49](https://github.com/philipz/spring-modular-monolith/blob/30c9bf30/k8s/manifests/rabbitmq.yaml#L1-L49)