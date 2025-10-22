# Orders 模組資料封裝與所有權分析（2025-10 更新）

## 資料來源
- Liquibase changelog (`src/main/resources/db/migration/`) 中的 `V4__orders_create_orders_table.sql` 與 `V5__orders_add_orders_data.sql` 只在 `orders` schema 下建立/初始化資料。
- Orders 模組內僅曝光 DTO (`orders.api.*`) 與事件物件 (`orders.api.events.OrderCreatedEvent`)，未向外公開 JPA entity。

## 存取範圍
| 層級 | 位置 | 說明 |
| --- | --- | --- |
| Repository | `orders/domain/OrderRepository` | 只被同模組的服務與 MapStore 使用。 |
| 服務 | `orders/domain/OrderService` | 建立訂單、發佈事件、委派快取刷新。 |
| REST/gRPC | `orders/web/**`, `orders/grpc/**` | 透過 DTO 與 Mapper 封裝 domain 實體。 |
| 模組 API | `orders/api/OrdersApi` | 供其他模組呼叫，回傳 DTO / View。 |

外部模組（inventory、notifications）僅透過 `OrderCreatedEvent` 取得所需資料；未見直接查詢 `orders.orders` 表或 JPA 實體的情況。

## Hazelcast 與快取
- `orders/cache/OrderMapStore` 與 `orders/config/HazelcastOrderCacheConfig` 均位於 Orders 模組內，並透過 `SpringAwareMapStoreConfig` 將 MapStore 註冊給全域 Hazelcast。
- `config/HazelcastConfig` 不再引用 Orders 內部類別，僅透過 `ObjectProvider<MapConfig>` 聚合各模組提供的設定。
- Session 與 cart 狀態透過 Hazelcast Spring Session (`BOOKSTORE_SESSION`) 處理，與訂單資料隔離。

## 事件與整合
- `OrderCreatedEvent` 位於 `orders.api.events`，payload 有限於建立庫存與通知所需資料。
- Inventory/Notifications 在各自模組內訂閱事件；未見跨 schema 直接讀取訂單資料的情況。

## 觀察
- **所有權清晰**：訂單資料的 schema、repository、快取均封裝在 orders 模組，對外只透過 API/事件。
- **快取/序列設定**：`OrderMapStore` 使用 lazy load + 批次查詢（`findByOrderNumberIn`），並在錯誤處理上考量啟動期。
- **資料型態**：`orders.orders` 將價格儲存為 `numeric`（由 Liquibase changelog 定義）；如未來抽離成獨立服務，序號 `order_id_seq` 可直接帶出。

## 建議
1. 若未來計畫支援多筆商品，多項欄位需改寫（目前 cart 只允許一筆 item）。
2. 持續監控 `OrderMapStore` 的例外處理（目前以 WARN/DEBUG 區分啟動期 vs. 正常期）。
3. 如果 orders-service 完全外部化，可將 `orders` schema 遷移出去並透過 gRPC/REST 回填，monolith 端保持 API/事件契約不變。
