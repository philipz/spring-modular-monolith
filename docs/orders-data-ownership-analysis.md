# Orders 模組資料封裝與所有權分析

## 分析概要
- **資料來源**：Liquibase 定義 (`src/main/resources/db/migration/V4__orders_create_orders_table.sql`、`V5__orders_add_orders_data.sql`) 僅在 `orders` schema 建立 `orders` 表與 `order_id_seq` 序號，未見其他 schema 直接操作 Orders 資料。
- **持久層封裝**：`OrderRepository`、`OrderService` 等資料存取元件僅存在於 Orders 模組內，模組外僅透過 `OrdersApi` 或事件 (`OrderCreatedEvent`) 存取訂單資訊。
- **外部依賴**：Orders 模組於建立訂單時只透過 `ProductServiceClient` 查 Catalog API，不直接讀寫他模組資料表；其他模組亦未直接查詢 `orders.orders` 表，僅透過事件或快取索引。

## 主要資料結構
| Schema.Table | 說明 | 欄位重點 |
| --- | --- | --- |
| `orders.orders` | 訂單主檔 | `id` (序號 `orders.order_id_seq`)、`order_number` (唯一值)、客戶與商品欄位、`status`、時間戳記 |

## 存取路徑
- **JPA 與服務層**：`OrderService` 負責訂單 CRUD 與事件發佈，`OrderRepository` 僅被 Orders 模組內部 (服務與 MapStore) 呼叫。
- **公開 API**：`OrdersApiService` 將控制器原有邏輯集中，外部模組只能透過 `OrdersApi` 取得 DTO/視圖或建立訂單。
- **模組外存取**：Inventory/Notifications 透過 `OrderCreatedEvent` 回應，未直接觸及資料表。

## 觀察到的風險/耦合點
| 項目 | 說明 |
| --- | --- |
| Hazelcast 設定跨模組引用 | `config/HazelcastConfig` 直接引用 `com.sivalabs.bookstore.orders.cache.OrderMapStore`，造成 Config → Orders 的反向依賴。需在 Task 8 將 MapStore 設定移至 Orders 模組或採 factory/export 方式解除耦合。 |
| 事件公開位置 | `OrderCreatedEvent` 已移至 `orders.api.events`，Inventory / Notifications 透過 API 事件存取，避免載入 Orders 內部實作。 |
| 金額欄位型態 | `product_price` 目前為 `text`，若未來獨立資料庫建議改為數值型態並定義幣別欄位。 |

## 結論與建議
1. Orders 模組對 `orders` schema 擁有完整資料所有權，外部呼叫皆經由 API 或事件完成，符合拆分前的封裝要求。
2. 應續推進 Task 7/8，以移除 Hazelcast MapStore 與事件的跨模組引用，確保待抽離時 Config 與其他模組不再依賴 Orders 內部型別。
3. 規劃獨立服務時，留意資料型態改善與序號初始化策略 (`order_id_seq`) 是否需要調整以避免與舊系統衝突。
