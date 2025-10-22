# Orders 模組邊界分析（2025-10 現況）

## 方法
- 以 `ModularityTests` (`ApplicationModules.verify()`) 驗證實際模組依賴。
- 檢視 `target/spring-modulith-docs/module-orders.*` 產生的圖表。
- 逐一審閱 `orders` 模組公開類別與其 import，以確認依賴流向。

## 對外依賴（Outbound）
| 依賴模組 | 主要使用元件 | 說明 |
| --- | --- | --- |
| `catalog` (`ProductApi`) | `orders/domain/ProductServiceClient`、`CartRestController` | 建立/驗證訂單時讀取產品資訊；透過公開 API 而非直接存取 repository。 |
| `common` (`cache`, `events`) | `orders/cache/OrderMapStore`、`orders/config/HazelcastOrderCacheConfig`、`orders/api/events` | 使用共用的 MapStore helper (`SpringAwareMapStoreConfig`) 與錯誤處理；事件型別放在 `orders.api.events` 讓其他模組可訂閱。 |
| `config` (基礎設施 bean) | 依賴 `ManagedChannel`、`CacheProperties` 等 bean | `config` 模組僅提供通用基礎設施（Hazelcast instance、gRPC channel），不造成反向依賴。 |

> 先前的耦合點（`Config` 直接引用 `OrderMapStore`）已由 `orders/config/HazelcastOrderCacheConfig` 解決，現在改為 orders 模組自行註冊 `MapConfig`，`config/HazelcastConfig` 只透過 `ObjectProvider<MapConfig>` 聚合。

## 對外公開（Inbound）
| 公開元件 | 消費方 | 說明 |
| --- | --- | --- |
| `OrdersApi` 介面 + `OrdersApiService` | 其他模組透過 Spring bean 呼叫（例如未來的 checkout 流程） | 將訂單建立與查詢流程封裝成公開 API。 |
| REST 控制器 (`OrdersRestController`, `CartRestController`) | 外部 HTTP 客戶端、Next.js storefront | 暴露 `/api/orders/**`、`/api/cart/**`。 |
| gRPC 服務 (`OrdersGrpcService`) | `orders-service` 或其他 gRPC 客戶端 | 以 proto 定義的訊息/服務為單位，配合 `GrpcMessageMapper`。 |
| 事件 (`OrderCreatedEvent`) | `inventory`、`notifications` 模組 | 事件類別位於 `orders.api.events`，避免載入 Orders 內部實作。 |

## 實作內聚與封裝狀態
- **資料存取**：`orders/domain` 下的 `OrderRepository`、`OrderService` 僅被 orders 模組內部使用，未見跨模組引用。
- **快取**：Orders 模組自行提供 `OrderMapStore`、`HazelcastOrderCacheConfig`，與 `config/HazelcastConfig` 的界線清晰。
- **外部服務**：`OrdersGrpcClient` 依賴 `ManagedChannel`（由 `config/GrpcClientConfig` 提供），並透過 `OrdersRemoteClient` 介面讓 REST 控制器與內部服務保持解耦。
- **測試**：`src/test/java/com/sivalabs/bookstore/orders/...` 內含 MapStore、Cache health 等測試；`ModularityTests` 維持綠燈，顯示未有未授權依賴。

## 風險觀察
- **Cart 模式限制**：目前僅支援單一品項，若要擴充需調整 `Cart`、`CartDto` 與 MapStore 關聯。
- **gRPC 相依配置**：`OrdersGrpcClient` 依賴外部服務時若未啟動 `orders-service` 需確保 `bookstore.grpc.client.target` 指回本地 (`localhost:9091`)；相關設定已在 README 強調。

## 結論
Orders 模組對外僅依賴 Catalog 公開 API 與 Common 的共用基礎設施，公開面向則透過 API、REST、gRPC 及事件四種途徑提供。Hazelcast 設定與快取 MapStore 已完全內聚於模組內，未偵測到跨模組的內部實作引用。整體邊界符合 Spring Modulith 預期，可支援後續抽離 orders-service 或演進 gRPC 拆分的需求。
