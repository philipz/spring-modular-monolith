# 在 Spring Boot Modulith 3.5.5 中整合 OpenTelemetry 的技術報告

## 導言

Spring Boot Modulith 3.5.5 讓開發人員可以在單一 Java 應用程式中定義清晰的模組邊界，並利用 Micrometer 的可觀測性 API 自動追蹤模組間的呼叫、事件發佈與訂閱。可觀測性需求通常涵蓋 **Tracing、Metrics、Logging** 三大類，建議採用 **OpenTelemetry (OTel)** 作為統一的資料格式，並透過 **OTel Collector** 將資料轉發至後端（如 Jaeger、Prometheus、Loki、Grafana Cloud 等）。本文以 Spring Boot Modulith 3.5.5 為基礎，說明如何在 Docker Compose 環境中整合 OTel（同時支援 gRPC port 4317 與 HTTP port 4318）。

## 一、導入步驟與必要依賴

### 1. 建置與依賴管理

1. **匯入 Maven BOM**：為了避免版本衝突，將 opentelemetry‑instrumentation‑bom 和 spring‑modulith‑bom 加入 dependencyManagement。開啟 spring‑modulith‑observability 時會使用這些版本來對齊依賴。
2. **模組化 Starter**：引入 spring‑modulith‑starter‑insight 以攔截模組 API 的呼叫並建立 Micrometer span。NashTech Blog 指出，此 Starter 會將所有模組的 API 裝飾成 AOP 代理，當模組間呼叫時會產生 Micrometer span，可以透過 Zipkin 等工具觀察[[1]](https://blog.nashtechglobal.com/building-modern-monoliths-with-spring-modulith/#:~:text=The%20interaction%20between%20application%20modules,invocations%20and%20generate%20these%20spans)。
3. **Tracing 依賴**：使用 Micrometer Tracing 搭配 OTel 匯出器。依賴包括：
4. spring‑boot‑starter‑actuator（提供 Actuator 端點與管理屬性）。
5. io.micrometer:micrometer‑tracing‑bridge‑otel（橋接 Micrometer Observation 與 OpenTelemetry[[2]](https://docs.spring.io/spring-boot/reference/actuator/tracing.html#:~:text=Now%20we%20have%20to%20add,the%20following%20dependencies)）。
6. io.opentelemetry:opentelemetry‑exporter‑otlp（OTLP gRPC/HTTP 匯出器）。
7. **Metrics 依賴**：io.micrometer:micrometer‑registry‑otlp，Spring Boot 也提供 management.otlp.metrics.export.\* 屬性；預設導出至 localhost:4318/v1/metrics，可透過 management.otlp.metrics.export.url 或環境變數 OTEL\_EXPORTER\_OTLP\_METRICS\_ENDPOINT 來覆寫[[3]](https://docs.micrometer.io/micrometer/reference/implementations/otlp.html#:~:text=override%20the%20default%20configuration%20through,OtlpConfig)。
8. **Logging 依賴**：使用 io.opentelemetry.instrumentation:opentelemetry‑logback‑appender 或直接使用 opentelemetry‑spring‑boot‑starter。Spring 官方部落格指出，當使用 OTel Spring Boot starter 時，Starter 在記錄系統初始化後會自動將 OTel Logback appender 加入，不需要手動配置[[4]](https://opentelemetry.io/blog/2024/spring-starter-stable/#:~:text=One%20example%20we%20heavily%20improved,is%20the%20Logback%20instrumentation)。
9. **Spring Modulith 1.4+ 支援**：Spring 模組化 1.4 版將可觀測性從舊有 tracing API 遷移至 Micrometer Observation，並在發布事件時自動產生 counter，以及提供 ModulithEventMetricsCustomizer SPI 用來自訂事件計數器[[5]](https://spring.io/blog/2025/05/28/spring-modulith-1-4-1-3-6-and-1-2-13-released#:~:text=,1068)。若需要細粒度控制事件計數器或標籤，可注入 ModulithEventMetricsCustomizer 並使用 ModulithEventMetrics API[[6]](https://docs.spring.io/spring-modulith/docs/1.4.0-M3/api/org/springframework/modulith/observability/ModulithEventMetrics.html#:~:text=public%20interface%20ModulithEventMetrics)。

### 2. 版本相依表

| 模組 | 推薦版本 | 說明 |
| --- | --- | --- |
| Spring Boot Modulith | 3.5.5 | 使用 Spring Boot 3.5.5；官方說明指出此版本包含錯誤修正和依賴升級[[7]](https://spring.io/blog/2025/08/21/spring-boot-3-5-5-available-now#:~:text=Releases%20%20,)。 |
| Spring Modulith Starter Insight | 1.4.x | 攔截模組 API 呼叫產生 Micrometer span[[1]](https://blog.nashtechglobal.com/building-modern-monoliths-with-spring-modulith/#:~:text=The%20interaction%20between%20application%20modules,invocations%20and%20generate%20these%20spans)。 |
| Micrometer Tracing Bridge OTel | 1.3.x | 橋接 Observation API 與 OTel[[2]](https://docs.spring.io/spring-boot/reference/actuator/tracing.html#:~:text=Now%20we%20have%20to%20add,the%20following%20dependencies)。 |
| OpenTelemetry Exporter OTLP | 1.33.x | 支援 gRPC (4317) 或 HTTP protobuf (4318) 協定[[8]](https://opentelemetry.io/docs/languages/java/configuration/#:~:text=System%20property%20Description%20Default%20%60otel.,1)。 |
| Micrometer Registry OTLP | 1.13.x | 將 Metrics 傳送至 Collector[[3]](https://docs.micrometer.io/micrometer/reference/implementations/otlp.html#:~:text=override%20the%20default%20configuration%20through,OtlpConfig)。 |
| OpenTelemetry Logback Appender 或 OpenTelemetry Spring Boot Starter | 最新穩定 | 送出結構化日誌至 Collector；建議搭配 spring‑boot‑starter‑logging 使用[[4]](https://opentelemetry.io/blog/2024/spring-starter-stable/#:~:text=One%20example%20we%20heavily%20improved,is%20the%20Logback%20instrumentation)。 |

### 3. 建置 dockerized Spring Boot 模板

1. **Dockerfile**：使用 OpenJDK 21 slim 基礎映像，複製 fat Jar，設定 Java options（例如 JAVA\_TOOL\_OPTIONS=-Dotel.service.name=bookstore -Dotel.resource.attributes=environment=local），並暴露 port 8080。
2. **docker-compose.yml**：包含 application、otel-collector、以及觀測平台（例如 Jaeger、Prometheus、Grafana）。設定網絡並映射 port 4317 (gRPC) 與 4318 (HTTP)。

## 二、Tracing 整合與模組邊界

### 1. Spring Modulith 中的自動追蹤

Spring Modulith 的 spring-modulith-starter-insight 會自動攔截模組 API 方法呼叫並生成 Micrometer span；呼叫被視為跨模組 invocations 時會顯示在觀測平台。NashTech Blog 說明，模組間互動會產生 Micrometer span，可在 Zipkin 等工具中可視化[[1]](https://blog.nashtechglobal.com/building-modern-monoliths-with-spring-modulith/#:~:text=The%20interaction%20between%20application%20modules,invocations%20and%20generate%20these%20spans)。

### 2. 自訂事件和跨模組呼叫的追蹤

1. **使用 Application Events 傳遞訊息**：模組間利用 Spring events 解耦，ApplicationEventPublisher 會在事件發佈前建立 span，事件 listener 在另一個模組處理時會利用相同 trace ID 以串聯流程。
2. **觀察者 API**：可直接使用 Micrometer Observation API 產生自定義 span。ObservationRegistry 預設會使用 Micrometer Tracing 和 OTel SpanExporter。以下範例說明如何在服務方法中建立 observation：

@Service
public class InventoryService {
 private final ObservationRegistry observationRegistry;
 private final ApplicationEventPublisher publisher;

 public InventoryService(ObservationRegistry registry, ApplicationEventPublisher publisher) {
 this.observationRegistry = registry;
 this.publisher = publisher;
 }

 public void deductStock(String productId, int amount) {
 Observation.createNotStarted("inventory.deduct", observationRegistry)
 .contextualName("Deduct Stock")
 .highCardinalityKeyValue("product.id", productId)
 .observe(() -> {
 // 核心邏輯
 // ... 更新庫存資料庫
 // 發佈事件
 publisher.publishEvent(new StockDeductedEvent(productId, amount));
 });
 }
}

1. **事件訂閱端監聽**：事件 listener 會接收相同 trace ID，從而建立同一條追蹤。將 listener 標記為 @Async 時仍可透過跨執行緒的 trace 連結。如下：

@Component
public class NotificationListener {
 @EventListener
 public void onStockDeducted(StockDeductedEvent event) {
 // 自動取得 trace context 並建立子 Span
 // 發送通知，例如電子郵件
 }
}

1. **自定義 Span 和 baggage**：可藉由注入 io.micrometer.tracing.Tracer 手動開始 span 或設置 baggage。Spring Boot tracing 文檔提供範例，透過 Tracer.currentSpan() 取得當前 span，並可向其中加入 metadata[[9]](https://docs.spring.io/spring-boot/reference/actuator/tracing.html#:~:text=Integration%20with%20Micrometer%20Observation)。

### 3. 事件計數器與 Metrics 自動化

Spring Modulith 1.4 起在事件發佈時會自動建立 Counter 指標；官方 release notes 指出新增 ModulithEventMetrics API，並可透過 ModulithEventMetricsCustomizer 自訂 Counter（例如加入標籤）[[5]](https://spring.io/blog/2025/05/28/spring-modulith-1-4-1-3-6-and-1-2-13-released#:~:text=,1068)[[6]](https://docs.spring.io/spring-modulith/docs/1.4.0-M3/api/org/springframework/modulith/observability/ModulithEventMetrics.html#:~:text=public%20interface%20ModulithEventMetrics)。若需要調整事件計數器名稱或標籤，可定義一個 bean：

@Configuration
class EventMetricsConfig implements ModulithEventMetricsCustomizer {
 @Override
 public void customize(ModulithEventMetrics metrics) {
 // 為 StockDeductedEvent 增加 tag 'module'
 metrics.customize(StockDeductedEvent.class, (event, builder) -> {
 builder.tag("module", "inventory");
 });
 }
}

此項自定義會在計數器建立時加入標籤，並由 Micrometer Registry 導出至 OTel Collector。由於是 Counter 指標，因此適用於儀表板顯示事件流量。

## 三、Metrics 整合

### 1. Micrometer Registry OTLP

Micrometer 提供 micrometer-registry-otlp。Spring Boot 參考文檔指出可透過 management.otlp.metrics.export.url 設定指標端點，預設為 localhost:4318/v1/metrics[[3]](https://docs.micrometer.io/micrometer/reference/implementations/otlp.html#:~:text=override%20the%20default%20configuration%20through,OtlpConfig)。也可以使用環境變數 OTEL\_EXPORTER\_OTLP\_METRICS\_ENDPOINT 或 OTEL\_EXPORTER\_OTLP\_ENDPOINT 來覆寫[[3]](https://docs.micrometer.io/micrometer/reference/implementations/otlp.html#:~:text=override%20the%20default%20configuration%20through,OtlpConfig)。

### 2. 為 REST API 與商業邏輯建立指標

1. **自動指標**：Spring Boot Actuator 自動提供 HTTP request latency（http.server.requests）和 JVM 指標。這些指標會自動透過 Micrometer OTLP Registry 導出。
2. **自訂 Timer / Counter**：可使用 Micrometer API 建立 Timer。例如統計產品查詢時長：

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class ProductService {
 private final Timer productQueryTimer;

 public ProductService(MeterRegistry registry) {
 this.productQueryTimer = Timer.builder("product.query.time")
 .description("Time to fetch product details")
 .tag("module", "product")
 .register(registry);
 }

 public Product findProduct(String id) {
 return productQueryTimer.record(() -> {
 // 查詢資料庫
 return productRepository.findById(id).orElseThrow();
 });
 }
}

1. **日誌指標**：Micrometer 提供 LogbackMetrics；將其註冊至 MeterRegistry 可自動統計不同日誌等級的訊息數量[[10]](https://docs.micrometer.io/micrometer/reference/reference/logging.html#:~:text=Micrometer%20can%20add%20metrics%20to,different%20loggers)。此指標可以評估 log 量。

### 3. Metrics 導出配置

**application.yaml 範例：**

spring:
 application:
 name: bookstore

# 啟用模組化追蹤
management:
 tracing:
 enabled: true
 sampling:
 probability: 1.0 # 全部請求都進行追蹤
 otlp:
 metrics:
 export:
 url: http://otel-collector:4318/v1/metrics # 導出至 Collector HTTP
 tracing:
 endpoint: http://otel-collector:4318/v1/traces # 導出 trace
 protocol: http # 使用 HTTP/protobuf，若需 gRPC 則設置 grpc

 endpoints:
 web:
 exposure:
 include: health,info,prometheus,modulith

# 自定義事件計數器 (如使用上面的自定義類)

此設定會將 Actuator metrics 和事件計數器透過 OTLP HTTP port 4318 導出。

## 四、Log 整合

### 1. Logback 透過 OpenTelemetry 導出

* **Spring Boot starter**：opentelemetry-spring-boot-starter（包含 OTel Logger）會在應用啟動時自動向 Logback 注入 OpenTelemetryAppender[[4]](https://opentelemetry.io/blog/2024/spring-starter-stable/#:~:text=One%20example%20we%20heavily%20improved,is%20the%20Logback%20instrumentation)。該 appender 會從 Logger 取得 trace context，並以 OTLP 格式傳送日誌到 Collector。
* **環境變數配置**：OpenTelemetry Java SDK 指出 OTEL\_EXPORTER\_OTLP\_LOGS\_ENDPOINT 設定 logs 接收端點；預設 protocol 為 gRPC（4317），可通過 OTEL\_EXPORTER\_OTLP\_PROTOCOL 指定 http/protobuf[[8]](https://opentelemetry.io/docs/languages/java/configuration/#:~:text=System%20property%20Description%20Default%20%60otel.,1)。要啟用 log export，需要設置 OTEL\_LOGS\_EXPORTER=otlp；若未設定，日誌不會被導出。
* **結構化日誌格式**：若希望 log 包含追蹤 ID，可在 application.properties 中加入 logging.pattern.correlation 來顯示 traceId 與 spanId[[2]](https://docs.spring.io/spring-boot/reference/actuator/tracing.html#:~:text=Now%20we%20have%20to%20add,the%20following%20dependencies)。

### 2. logback-spring.xml 範例（僅當未使用 starter 時）

<configuration>
 <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
 <appender name="OTEL" class="io.opentelemetry.instrumentation.logback.appender.v1\_0.OpenTelemetryAppender">
 <!-- resourceAttributes 用於指定服務名、環境等 -->
 <resourceAttributes>
 service.name=bookstore
 deployment.environment=local
 </resourceAttributes>
 <endpoint>http://otel-collector:4318/v1/logs</endpoint> <!-- HTTP 協定 -->
 <!-- 若想使用 gRPC，設置 grpc port 4317 並去掉 /v1/logs -->
 </appender>
 <root level="INFO">
 <appender-ref ref="OTEL"/>
 </root>
</configuration>

### 3. 設定環境變數

在 docker compose 檔中，可以為應用服務加入以下環境變數：

environment:
 OTEL\_SERVICE\_NAME: bookstore
 OTEL\_RESOURCE\_ATTRIBUTES: deployment.environment=local
 OTEL\_EXPORTER\_OTLP\_ENDPOINT: http://otel-collector:4317 # 主要端點
 OTEL\_EXPORTER\_OTLP\_PROTOCOL: grpc
 OTEL\_EXPORTER\_OTLP\_TRACES\_ENDPOINT: http://otel-collector:4318/v1/traces
 OTEL\_EXPORTER\_OTLP\_METRICS\_ENDPOINT: http://otel-collector:4318/v1/metrics
 OTEL\_EXPORTER\_OTLP\_LOGS\_ENDPOINT: http://otel-collector:4318/v1/logs
 OTEL\_LOGS\_EXPORTER: otlp

OTEL\_EXPORTER\_OTLP\_ENDPOINT 為通用端點（預設 gRPC），若配置 signal‑specific endpoint，就會覆寫預設[[8]](https://opentelemetry.io/docs/languages/java/configuration/#:~:text=System%20property%20Description%20Default%20%60otel.,1)。此範例同時透過 gRPC 與 HTTP 送出，Collector 必須同時打開 port 4317 和 4318。

## 五、Docker Compose 中的 OTel Collector 配置

下面提供一個簡易的 docker-compose.yml 範例，包含 Spring Boot 應用與 OTel Collector，Collector 同時監聽 gRPC(4317) 與 HTTP(4318)，並將 trace、metrics、logs 導出至控制台（可替換為 Jaeger、Prometheus 等）：

version: "3.9"
services:
 otel-collector:
 image: otel/opentelemetry-collector-contrib:0.100.0
 command: ["--config", "/etc/otel-collector-config.yaml"]
 ports:
 - "4317:4317" # gRPC
 - "4318:4318" # HTTP
 volumes:
 - ./otel-collector-config.yaml:/etc/otel-collector-config.yaml

 app:
 build: . # Dockerfile 會打包 Spring Boot 模組
 depends\_on: [otel-collector]
 environment:
 OTEL\_SERVICE\_NAME: bookstore
 OTEL\_RESOURCE\_ATTRIBUTES: deployment.environment=local
 OTEL\_EXPORTER\_OTLP\_ENDPOINT: http://otel-collector:4317
 OTEL\_EXPORTER\_OTLP\_TRACES\_ENDPOINT: http://otel-collector:4318/v1/traces
 OTEL\_EXPORTER\_OTLP\_METRICS\_ENDPOINT: http://otel-collector:4318/v1/metrics
 OTEL\_EXPORTER\_OTLP\_LOGS\_ENDPOINT: http://otel-collector:4318/v1/logs
 OTEL\_EXPORTER\_OTLP\_PROTOCOL: grpc
 OTEL\_LOGS\_EXPORTER: otlp

對應的 otel-collector-config.yaml：

receivers:
 otlp:
 protocols:
 grpc: # 接收 gRPC 於 4317
 http: # 接收 HTTP/Protobuf 於 4318
processors:
 batch: {}
exporters:
 logging:
 loglevel: debug
 otlphttp: # 若要再轉送至其他 Collector 或後端
 endpoint: http://tempo:4318 # 例：轉送 traces 至 Grafana Tempo
service:
 pipelines:
 traces:
 receivers: [otlp]
 processors: [batch]
 exporters: [logging, otlphttp]
 metrics:
 receivers: [otlp]
 processors: [batch]
 exporters: [logging, otlphttp]
 logs:
 receivers: [otlp]
 processors: [batch]
 exporters: [logging, otlphttp]

此配置示範 Collector 同時接收 gRPC 及 HTTP 協定[[11]](https://opentelemetry.io/blog/2024/collecting-otel-compliant-java-logs-from-files/#:~:text=Configure%20the%20Collector%20to%20ingest,the%20OTLP%2FJSON%20logs)。實際環境可將 otlphttp exporter 改成 Prometheus remote\_write、Jaeger、Loki 等。

## 六、模組程式碼範例（保持解耦）

以下範例展示如何在 Spring Modulith 模組中使用事件進行解耦，並透過 Observation API 捕捉可觀測性資料。模組目錄假設為 com.example.order (Order 模組) 和 com.example.inventory (Inventory 模組)。

### 1. 事件類別

// com.example.inventory.event
public record StockDeductedEvent(String productId, int amount) {}

### 2. InventoryService – 發佈事件並建立 Observation

@Service
class InventoryService {
 private final ObservationRegistry observationRegistry;
 private final ApplicationEventPublisher publisher;

 InventoryService(ObservationRegistry observationRegistry, ApplicationEventPublisher publisher) {
 this.observationRegistry = observationRegistry;
 this.publisher = publisher;
 }

 public void deductStock(String productId, int amount) {
 Observation.createNotStarted("inventory.deduct", observationRegistry)
 .contextualName("Deduct Stock")
 .lowCardinalityKeyValue("module", "inventory")
 .highCardinalityKeyValue("product.id", productId)
 .observe(() -> {
 // 更新庫存
 // 發佈事件通知其他模組
 publisher.publishEvent(new StockDeductedEvent(productId, amount));
 });
 }
}

### 3. Order 模組 – 監聽事件

@Component
class OrderListener {
 @EventListener
 public void handleStockDeducted(StockDeductedEvent event) {
 // 此處自動繼承 trace context
 // 處理後續業務，如更新訂單狀態或通知使用者
 }
}

### 4. 模組測試與結構驗證

Spring Modulith 提供 ApplicationModules 來驗證模組依賴；以下為簡易單元測試：

class ModularityTests {
 private final ApplicationModules modules = ApplicationModules.of(BookstoreApplication.class);

 @Test
 void verifiesModularStructure() {
 modules.verify();
 }
}

當出現循環依賴時，測試會失敗，可協助開發人員調整模組架構，且在可觀測性層面可以透過 trace 知悉循環呼叫位置[[12]](https://www.makariev.com/blog/enhance-observability-spring-boot-microservices-with-micrometer-open-telemetry-and-spring-modulith-starter-insight/#:~:text=We%E2%80%99ve%20implemented%20a%20new%20,triggering%20modular%20structure%20verification%20errors)。

## 七、常見錯誤與除錯方式

| 問題 | 排查建議 |
| --- | --- |
| **資料未出現在 Collector** | 確認應用程序的環境變數 OTEL\_EXPORTER\_OTLP\_\* 是否正確設定並指向 Collector。注意 gRPC 與 HTTP 協定路徑不同；HTTP 必須包含 /v1/{signal}[[8]](https://opentelemetry.io/docs/languages/java/configuration/#:~:text=System%20property%20Description%20Default%20%60otel.,1)。 |
| **沒有 trace ID/ span ID 出現在日誌中** | 檢查是否在 application.properties 設定 logging.pattern.correlation=  $$$$  [[2]](https://docs.spring.io/spring-boot/reference/actuator/tracing.html#:~:text=Now%20we%20have%20to%20add,the%20following%20dependencies)，或使用 OTel Spring Boot Starter，它會自動將 correlation 加入 MDC。 |
| **模組調用沒有產生 span** | 確認已引入 spring‑modulith‑starter‑insight，且 Actuator tracing 啟用。若模組間以事件解耦，需透過 Observation API 手動包裹業務邏輯（如前述 Observation.createNotStarted 範例）。 |
| **事件計數器未出現** | Spring Modulith 1.4 以後才自動記錄事件計數[[5]](https://spring.io/blog/2025/05/28/spring-modulith-1-4-1-3-6-and-1-2-13-released#:~:text=,1068)；確認版本並檢查 MeterRegistry 中是否出現 CrossModuleEventCounterFactory。如要調整計數器標籤，實作 ModulithEventMetricsCustomizer。 |
| **Collector gRPC/HTTP 端口衝突** | 確認 docker compose 中沒有其他服務佔用 port 4317 或 4318。 |
| **日誌導出過多導致性能下降** | 日誌導出仍屬實驗特性[[13]](https://opentelemetry.io/blog/2024/collecting-otel-compliant-java-logs-from-files/#:~:text=OTel%20JSON%20format%20%28aka%20OTLP%2FJSON%29,Log4j%20are%20optional%20but%20recommended)；可降低 log 等級或僅針對錯誤級別導出，並在 Collector 中加入過濾器。 |

## 結論

本文示範如何在 **Spring Boot Modulith 3.5.5** 應用中整合 **OpenTelemetry** 的 **Tracing、Metrics、Logging**。透過引入 spring‑modulith‑starter‑insight、Micrometer Tracing Bridge OTel、OTLP 匯出器與 Micrometer Registry OTLP，可自動在模組邊界和事件機制中建立 span 與 counter[[1]](https://blog.nashtechglobal.com/building-modern-monoliths-with-spring-modulith/#:~:text=The%20interaction%20between%20application%20modules,invocations%20and%20generate%20these%20spans)[[5]](https://spring.io/blog/2025/05/28/spring-modulith-1-4-1-3-6-and-1-2-13-released#:~:text=,1068)。設定 application.yaml 與環境變數後，應用的 trace、metrics 與 logs 就可同時透過 gRPC（port 4317）與 HTTP（port 4318）送至 OTel Collector，再轉發至其他觀測平台。搭配 Docker Compose，可快速在本地環境驗證可觀測性。透過自訂事件計數器與 Observation API，開發人員能為核心業務邏輯建立精確的指標與 span，並保持模組化與解耦。EOF

[[1]](https://blog.nashtechglobal.com/building-modern-monoliths-with-spring-modulith/#:~:text=The%20interaction%20between%20application%20modules,invocations%20and%20generate%20these%20spans) Building modern monoliths with Spring Modulith - NashTech Blog

<https://blog.nashtechglobal.com/building-modern-monoliths-with-spring-modulith/>

[[2]](https://docs.spring.io/spring-boot/reference/actuator/tracing.html#:~:text=Now%20we%20have%20to%20add,the%20following%20dependencies) [[9]](https://docs.spring.io/spring-boot/reference/actuator/tracing.html#:~:text=Integration%20with%20Micrometer%20Observation) Tracing :: Spring Boot

<https://docs.spring.io/spring-boot/reference/actuator/tracing.html>

[[3]](https://docs.micrometer.io/micrometer/reference/implementations/otlp.html#:~:text=override%20the%20default%20configuration%20through,OtlpConfig) Micrometer OTLP :: Micrometer

<https://docs.micrometer.io/micrometer/reference/implementations/otlp.html>

[[4]](https://opentelemetry.io/blog/2024/spring-starter-stable/#:~:text=One%20example%20we%20heavily%20improved,is%20the%20Logback%20instrumentation) The OpenTelemetry Spring Boot starter is now stable | OpenTelemetry

<https://opentelemetry.io/blog/2024/spring-starter-stable/>

[[5]](https://spring.io/blog/2025/05/28/spring-modulith-1-4-1-3-6-and-1-2-13-released#:~:text=,1068) Spring Modulith 1.4 GA, 1.3.6, and 1.2.13 released

<https://spring.io/blog/2025/05/28/spring-modulith-1-4-1-3-6-and-1-2-13-released>

[[6]](https://docs.spring.io/spring-modulith/docs/1.4.0-M3/api/org/springframework/modulith/observability/ModulithEventMetrics.html#:~:text=public%20interface%20ModulithEventMetrics) ModulithEventMetrics (Spring Modulith 1.4.0-M3 API)

<https://docs.spring.io/spring-modulith/docs/1.4.0-M3/api/org/springframework/modulith/observability/ModulithEventMetrics.html>

[[7]](https://spring.io/blog/2025/08/21/spring-boot-3-5-5-available-now#:~:text=Releases%20%20,) Spring Boot 3.5.5 available now

<https://spring.io/blog/2025/08/21/spring-boot-3-5-5-available-now>

[[8]](https://opentelemetry.io/docs/languages/java/configuration/#:~:text=System%20property%20Description%20Default%20%60otel.,1) Configure the SDK | OpenTelemetry

<https://opentelemetry.io/docs/languages/java/configuration/>

[[10]](https://docs.micrometer.io/micrometer/reference/reference/logging.html#:~:text=Micrometer%20can%20add%20metrics%20to,different%20loggers) Logging Metrics Instrumentation :: Micrometer

<https://docs.micrometer.io/micrometer/reference/reference/logging.html>

[[11]](https://opentelemetry.io/blog/2024/collecting-otel-compliant-java-logs-from-files/#:~:text=Configure%20the%20Collector%20to%20ingest,the%20OTLP%2FJSON%20logs) [[13]](https://opentelemetry.io/blog/2024/collecting-otel-compliant-java-logs-from-files/#:~:text=OTel%20JSON%20format%20%28aka%20OTLP%2FJSON%29,Log4j%20are%20optional%20but%20recommended) Collecting OpenTelemetry-compliant Java logs from files | OpenTelemetry

<https://opentelemetry.io/blog/2024/collecting-otel-compliant-java-logs-from-files/>

[[12]](https://www.makariev.com/blog/enhance-observability-spring-boot-microservices-with-micrometer-open-telemetry-and-spring-modulith-starter-insight/#:~:text=We%E2%80%99ve%20implemented%20a%20new%20,triggering%20modular%20structure%20verification%20errors) Enhancing Observability in Spring Boot Microservices with Micrometer, OpenTelemetry, and Spring Modulith Starter Insight

<https://www.makariev.com/blog/enhance-observability-spring-boot-microservices-with-micrometer-open-telemetry-and-spring-modulith-starter-insight/>
