# Spring Modulith模組抽取完整實戰指南

Spring Modulith作為企業級模組化架構的領先解決方案，為開發者提供了從單體應用向模組化系統演進的完整路徑。基於最新的1.4.x版本和企業實戰經驗，本指南提供了涵蓋技術實作、最佳實務、監控維護的全方位策略，讓團隊能夠安全、高效地實現系統現代化升級。

## 模組邊界識別與依賴分析策略

Spring Modulith採用**包結構驅動的自動發現機制**，主應用程式包的每個直接子包被自動識別為應用模組。企業可通過`spring.modulith.detection-strategy=explicitly-annotated`啟用明確註解策略，精確控制模組邊界。

### 核心依賴分析實作

```java
@Test
void analyzeModuleDependencies() {
    ApplicationModules modules = ApplicationModules.of(Application.class);
    
    // 自動驗證架構合規性
    modules.verify();
    
    // 生成架構文檔
    new Documenter(modules)
        .writeModulesAsPlantUml()           // UML關係圖
        .writeIndividualModulesAsPlantUml() // 個別模組圖
        .writeModuleCanvases();             // 模組畫布

    // 分析模組耦合度
    modules.stream().forEach(module -> {
        System.out.println("模組: " + module.getName());
        System.out.println("對外依賴: " + module.getEfferentCoupling());
        System.out.println("被依賴數: " + module.getAfferentCoupling());
    });
}
```

**循環依賴檢測**透過內建的ArchUnit規則實現自動化驗證，強制禁止模組間循環依賴。當檢測到違規時，開發者需要透過**事件驅動重構**或**共享內核提取**策略解決依賴問題。

### 進階模組配置策略

```java
// 明確依賴聲明
@ApplicationModule(
    allowedDependencies = {"order", "shared::api"}, 
    displayName = "庫存管理模組"
)
package com.example.ecommerce.inventory;

// Named Interface暴露特定包
@NamedInterface("api")
package com.example.ecommerce.order.api;
```

## Database per Service模式完整實作

企業級資料庫分離需要採用**三階段漸進式策略**：單一資料庫單一Schema（程式碼組織）、單一資料庫多Schema（中度解耦）、多資料庫（完全隔離）。

### 多資料源配置實作

```java
@Configuration
@EnableJpaRepositories(
    basePackages = {"com.example.command.domain", "org.springframework.modulith.events.jpa"},
    entityManagerFactoryRef = "commandEntityManagerFactory",
    transactionManagerRef = "commandTransactionManager"
)
public class CommandJpaConfig {
    
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.command")
    public DataSourceProperties commandDataSourceProperties() {
        return new DataSourceProperties();
    }
    
    @Primary
    @Bean
    public DataSource commandDataSource() {
        return commandDataSourceProperties()
            .initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
    }
}
```

**跨模組資料一致性**透過事件驅動機制實現：

```java
@Service
@Transactional
public class ProductService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void updateProduct(Product product) {
        productRepository.save(product);
        eventPublisher.publishEvent(
            new ProductUpdatedEvent(product.getId(), product.getName())
        );
    }
}

// 跨模組同步
@ApplicationModuleListener
public void on(ProductUpdatedEvent event) {
    localProductCache.update(event.getProductId(), event.getProductName());
}
```

## 事件驅動架構核心實作

### 完整事件發布訂閱機制

```java
// 領域事件定義
public record OrderCompleted(String orderId, String customerId, BigDecimal amount) {}

// 事件發布者
@Service
@Transactional
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;
    
    public void completeOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        order.markAsCompleted();
        orderRepository.save(order);
        
        eventPublisher.publishEvent(
            new OrderCompleted(order.getId(), order.getCustomerId(), order.getAmount())
        );
    }
}

// 跨模組事件監聽
@Service
public class InventoryService {
    @ApplicationModuleListener
    @Async
    public void on(OrderCompleted event) {
        log.info("處理訂單完成事件: {}", event.orderId());
        updateInventoryForOrder(event.orderId());
    }
}
```

### 事件外部化到Kafka

```java
// 事件外部化配置
@Externalized("orders.OrderCompleted::#{customerId()}")
public record OrderCompleted(String orderId, String customerId, BigDecimal amount) {}
```

```yaml
# 事件持久化配置
spring:
  modulith:
    events:
      jdbc-schema-initialization:
        enabled: true
      republish-outstanding-events-on-restart: true
      completion-mode: UPDATE
```

## Saga Pattern分散式事務實作

企業級系統需要處理跨模組的分散式事務，Saga Pattern提供了**編舞式協調**的優雅解決方案：

```java
// Saga協調器實作
@Service
public class OrderSagaOrchestrator {
    private final ApplicationEventPublisher eventPublisher;
    private final SagaRepository sagaRepository;
    
    @ApplicationModuleListener
    public void on(OrderCreated event) {
        String sagaId = UUID.randomUUID().toString();
        
        OrderSaga saga = new OrderSaga(sagaId, event.orderId(), event.amount());
        sagaRepository.save(saga);
        
        eventPublisher.publishEvent(
            new OrderSagaStarted(sagaId, event.orderId(), event.amount())
        );
    }
    
    @ApplicationModuleListener
    public void on(PaymentCompleted event) {
        OrderSaga saga = sagaRepository.findBySagaId(event.sagaId());
        saga.markPaymentCompleted();
        
        eventPublisher.publishEvent(
            new ReserveInventoryCommand(event.sagaId(), saga.getOrderId())
        );
    }
    
    @ApplicationModuleListener
    public void on(PaymentFailed event) {
        OrderSaga saga = sagaRepository.findBySagaId(event.sagaId());
        saga.markAsFailed(event.reason());
        
        // 執行補償操作
        eventPublisher.publishEvent(new CancelOrderCommand(saga.getOrderId()));
    }
}
```

## Strangler Fig Pattern漸進式重構

**Netflix和Amazon的成功經驗**表明，漸進式重構是大型系統現代化的最安全路徑。Strangler Fig Pattern透過**並行運行**和**智慧路由**實現零停機時間的系統演進。

### 實作策略核心

```java
@Component
public class FeatureToggleService {
    @Value("${features.new-module.enabled:false}")
    private boolean newModuleEnabled;
    
    @Value("${features.new-module.rollout-percentage:0}")
    private int rolloutPercentage;
    
    public boolean shouldUseNewModule(String userId) {
        if (!newModuleEnabled) return false;
        
        int hash = userId.hashCode() % 100;
        return Math.abs(hash) < rolloutPercentage;
    }
}
```

**階段性實施路線圖**：
- **第一階段**：建立門面層和路由機制
- **第二階段**：新模組開發和A/B測試
- **第三階段**：逐步增加流量分配比例
- **第四階段**：舊系統下線和清理

## 企業級監控與可觀測性

### 完整監控堆疊配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,modulith
  endpoint:
    health:
      show-details: always
    modulith:
      enabled: true
      
spring:
  modulith:
    observability:
      enabled: true
```

**關鍵監控維度**包括模組間通訊延遲（目標P95 < 50ms）、模組錯誤率（目標 < 0.01%）、記憶體使用效率（目標60-80%）。

### OpenTelemetry零代碼追蹤

```java
@Service
public class ModulePerformanceService {
    private final Counter moduleInvocationCounter;
    private final Timer moduleResponseTime;
    
    @EventListener
    @Async
    public void recordModuleMetrics(ModuleInteractionEvent event) {
        moduleInvocationCounter.increment(
            Tags.of("module", event.getModuleName(),
                   "status", event.getStatus().toString())
        );
    }
}
```

## 韌性設計與故障處理

### Circuit Breaker Pattern實作

```java
@Service
public class ModuleInteractionService {
    
    @CircuitBreaker(name = "moduleA", fallbackMethod = "fallbackResponse")
    @Retry(name = "moduleA")
    @TimeLimiter(name = "moduleA")
    public CompletableFuture<String> callModuleA(String request) {
        return CompletableFuture.supplyAsync(() -> {
            return externalModuleClient.processRequest(request);
        });
    }
    
    public CompletableFuture<String> fallbackResponse(String request, Exception ex) {
        return CompletableFuture.completedFuture("服務暫時不可用，請稍後再試");
    }
}
```

**企業級韌性配置**：
```yaml
resilience4j:
  circuitbreaker:
    instances:
      moduleA:
        registerHealthIndicator: true
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

## CI/CD與部署最佳實務

### 模組化建構策略

```yaml
# GitHub Actions工作流
name: Modulith CI/CD Pipeline
jobs:
  module-detection:
    outputs:
      changed-modules: ${{ steps.changes.outputs.modules }}
    steps:
    - uses: dorny/paths-filter@v2
      with:
        filters: |
          usermodule:
            - 'src/main/java/com/example/user/**'
          ordermodule:
            - 'src/main/java/com/example/order/**'
            
  build-and-test:
    strategy:
      matrix:
        module: ${{ fromJSON(needs.module-detection.outputs.changed-modules) }}
    steps:
    - name: Build Module
      run: ./gradlew :${{ matrix.module }}:build
    - name: Integration Test
      run: ./gradlew :${{ matrix.module }}:integrationTest
```

### Kubernetes藍綠部署

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: spring-modulith-app
spec:
  strategy:
    blueGreen:
      activeService: spring-modulith-active
      prePromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: spring-modulith-preview.default.svc.cluster.local
```

## 優質GitHub專案參考

### 完整業務實作範例

**sivaprasadreddy/spring-modular-monolith** 提供了最完整的電商系統實作，展示Catalog、Orders、Inventory、Notifications四個核心模組的獨立設計，包含真正的資料隔離和事件驅動通訊，整合RabbitMQ進行外部消息發布。

**xsreality/spring-modulith-with-ddd** 透過4個branch展示漸進式重構的完整過程，結合DDD原則和六角架構，從分層架構到模組化系統的演進，每個階段都有詳細的技術決策記錄。

### 監控與可觀測性

**blueswen/spring-boot-observability** 提供最完整的觀測性實作，整合Traces（Tempo + OpenTelemetry）、Metrics（Prometheus + Micrometer）、Logs（Loki + Logback）三大支柱，使用OpenTelemetry Java Agent實現零代碼追蹤。

## 成功關鍵指標與企業價值

**技術指標**：部署頻率提升至每週2-3次、變更前置時間< 2小時、部署成功率> 99%、MTTR < 30分鐘、P95回應時間< 100ms。

**業務價值**：功能發布速度提升50%、開發效率提升30%、系統可用性達99.9%、維護成本降低40%。

**Netflix經驗總結**顯示，系統思維導向和敏捷性優化是成功的關鍵，最小化流程複雜度同時最大化演進能力，實現部署頻率從每月一次提升到每天多次，MTTR從數小時縮短到數分鐘。

## 結論

Spring Modulith為企業提供了一個成熟可靠的模組化架構解決方案，特別適合需要清晰架構邊界但不想承擔微服務複雜性的團隊。透過**嚴格遵循DDD原則**進行模組劃分、**建立完善的架構治理流程**、**投資自動化測試和監控**，以及**培養團隊架構意識**，企業可以建構出既具備良好模組化特性又保持操作簡便性的高品質系統。

成功的核心策略是**小步快跑、持續監控、快速反饋、及時調整**，每一步都要有明確的成功標準和回滾機制，確保業務連續性的同時實現技術架構的現代化升級。這種漸進式方法既降低了技術風險，又保證了業務價值的持續交付。