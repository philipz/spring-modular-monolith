# Spring Modulith + OpenTelemetry 整合比較報告

## 一、概述
本文件比較兩種在 **Spring Modulith 3.5.5** 環境中導入 **OpenTelemetry (OTel)** 的方式：
1. **Spring Modulith Starter Insight**
2. **Paketo Buildpacks（OpenTelemetry Java Agent）**

並探討兩者在可觀測性深度、導入複雜度、維運彈性與模組語義支持上的優缺點。

---

## 二、對比總覽

| 面向 | Spring Modulith Starter Insight | Paketo Buildpacks（OTel Java Agent） |
|---|---|---|
| 主要機制 | Spring Boot/Modulith 原生 Starter + Micrometer | Buildpacks 注入 OTel Java Agent |
| 與 Modulith 貼合度 | ⭐⭐⭐⭐ 模組與事件層級追蹤 | ⭐⭐ 泛用插樁，無模組語義 |
| 功能覆蓋 | Traces、Metrics、Logs（支援 OTLP 匯出） | 自動插樁 Traces、Metrics、Logs |
| 導入複雜度 | 中等：需修改程式設定 | 低：零改碼、環境變數設定即可 |
| 移植性 | 限 Spring Boot | 適用多語言與框架 |
| 控制粒度 | 可程式化控制 span、event | 以環境變數為主 |
| 適用場景 | 模組化應用、事件導向架構 | 多語言、快速普及觀測基線 |
| 潛在風險 | 重複插樁、需對程式修改 | 無法呈現模組語義、噪音高 |

---

## 三、導入方式

### A. Spring Modulith Starter Insight
**步驟：**
1. 新增依賴：`spring-modulith-starter-insight`。
2. 啟用 Actuator 與 Micrometer OTLP 匯出。
3. 設定 `application.yml`：

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
    metrics:
      export:
        url: http://otel-collector:4318/v1/metrics
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

4. 若需使用 gRPC 匯出：
   ```yaml
   management.otlp.tracing.endpoint: grpc://otel-collector:4317
   ```

5. 在模組事件中（`@ApplicationModuleListener`）加入自訂 span 或屬性以標註模組邏輯。

---

### B. Paketo Buildpacks（OTel Java Agent）
**步驟：**
1. 使用 Spring Boot Plugin 或 `pack` 建立映像：
   ```bash
   ./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=myapp
   ```
2. 啟用 OTel Buildpack 並設定環境變數：

```bash
BPL_OPENTELEMETRY_ENABLED=true
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_TRACES_EXPORTER=otlp
OTEL_METRICS_EXPORTER=otlp
OTEL_LOGS_EXPORTER=otlp
```

3. 若需同時支援 HTTP (4318)：
   ```bash
   OTEL_EXPORTER_OTLP_TRACES_PROTOCOL=http/protobuf
   OTEL_EXPORTER_OTLP_METRICS_PROTOCOL=http/protobuf
   ```

---

## 四、可觀測性覆蓋比較

| 指標 | Starter Insight | Buildpacks Agent |
|---|---|---|
| HTTP / DB / Messaging 自動追蹤 | ✅（Spring Boot starter 自帶） | ✅（Agent 插樁廣泛） |
| 模組邊界 / 事件流 | ✅ 強 | ❌ 弱 |
| 自訂屬性 / Span 命名 | ✅ 精確可控 | ⚙️ 可自訂但不直觀 |
| Metrics 匯出 | ✅ Micrometer → OTLP | ✅ OTel Exporter |
| Logs 匯出 | ✅ Logback OTLP Appender | ✅ 透過 Agent 或 Collector |
| 跨語言一致性 | ❌ 僅 Java/Spring | ✅ 高 |

---

## 五、常見風險與排錯建議

| 問題 | 原因 | 解法 |
|---|---|---|
| 雙重插樁（重複 spans） | 同時使用 Starter + Agent | 停用其中一方的特定 instrumentations |
| 無法連線 Collector | Endpoint、協定不符 | 確認 Collector `otlp` receiver 設定 |
| 高噪音 / 太多 spans | 預設攔截太廣 | 調整 `OTEL_TRACES_SAMPLER` 或關閉特定 instrumentation |
| 資料未出現於後端 | Exporter protocol 錯誤 | 同時測試 4317 (gRPC) / 4318 (HTTP) |

---

## 六、選型建議

- **若專案已採 Spring Modulith 並注重模組邊界與事件流觀測** → **選用 Spring Modulith Starter Insight**
- **若目標是快速在 Docker/K8s 層級統一導入 OpenTelemetry 且需跨語言支援** → **選用 Paketo Buildpacks + Java Agent**
- **混合方案**：
  - 以 Starter Insight 為主觀測模組內邏輯。
  - 用 Agent 捕捉外部依賴（HTTP、JDBC）。
  - 在 Agent 層停用重疊的自動化插樁。

---

## 七、Docker Compose 範例 (OTEL Collector)

```yaml
version: "3.8"
services:
  otel-collector:
    image: otel/opentelemetry-collector-contrib:latest
    command: ["--config=/etc/otel/config.yaml"]
    ports:
      - "4317:4317"   # gRPC
      - "4318:4318"   # HTTP
    volumes:
      - ./otel-config.yaml:/etc/otel/config.yaml
```

`otel-config.yaml` 內設定：

```yaml
receivers:
  otlp:
    protocols:
      grpc:
      http:

exporters:
  logging:
    loglevel: info
  prometheus:
    endpoint: "0.0.0.0:9464"

service:
  pipelines:
    traces:
      receivers: [otlp]
      exporters: [logging]
    metrics:
      receivers: [otlp]
      exporters: [prometheus]
```

---

## 八、結論

| 導入面 | Starter Insight | Buildpacks Agent |
|---|---|---|
| 導入難度 | 中 | 低 |
| 程式修改 | 需要少量修改 | 無需修改 |
| 模組語義支持 | 強 | 弱 |
| 維運一致性 | 較分散 | 高度統一 |
| 最佳用途 | Spring Modulith 模組可觀測性 | 平台級觀測普及 |

---

**推薦策略：**
> Spring Modulith 專案採用 Starter Insight 為主，必要時輔以 Buildpacks Agent 以統一跨應用觀測，並確保 Collector 同時開啟 4317 / 4318 以支援多協定輸入。
