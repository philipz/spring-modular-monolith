# Orders 模組邊界分析

## 分析方法
- 以 `ApplicationModules.verify()` 及 `Documenter` 產生的輸出 (`target/spring-modulith-docs/module-orders.*`) 為基礎盤點模組依賴。
- 透過 `rg` 檢視跨模組 import，確認 Orders 模組的實際引用與被引用狀況。
- 針對關鍵組件逐一檢閱原始碼，評估耦合度與封裝完整性。

## 對外依賴（Outbound）
| 依賴模組 | 主要使用位置 | 說明 |
| --- | --- | --- |
| `catalog::product-api` | `orders/domain/ProductServiceClient.java`, `orders/web/CartController.java` | 取得產品資訊並進行價格驗證，屬於必要的跨模組查詢。 |
| `common::common-cache` | `orders/cache/OrderCacheService.java` | 重用通用快取抽象與錯誤處理器，維持一致的 Hazelcast 行為。 |

> Spring Modulith 圖 (`target/spring-modulith-docs/module-orders.puml`) 僅顯示 Catalog 與 Common 兩個出站依賴，與 `@ApplicationModule` 限制相符。

## 對外曝光（Inbound）
| 對外元件 | 主要消費方 | 說明 |
| --- | --- | --- |
| `OrdersApi` (`orders/OrdersApi.java`) | 其他模組透過 API 單元呼叫下單、查詢訂單 (`OrdersApi` 為 `@Component`)。 |
| `OrderCreatedEvent` (`orders/api/events/OrderCreatedEvent.java`) | Inventory 與 Notifications 模組的事件處理器 (`inventory/OrderEventInventoryHandler.java`, `notifications/OrderEventNotificationHandler.java`) 直接引用。 |

## 耦合觀察
- **耦合度整體良好**：Orders 模組只與 Catalog、Common 互動，且互動集中在 Product API 與快取抽象兩處，負責的功能聚焦於訂單生命週期，內聚度高。
- **事件公開位置**：`OrderCreatedEvent` 已移至 `orders.api.events`，Inventory / Notifications 透過 API 套件取得事件型別，不再依賴 Orders 內部類別。
- **快取設定跨模組引用**：`config/HazelcastConfig.java` 直接指定 `orders.cache.OrderMapStore` 類別名稱，導致 Config 模組依賴 Orders 內部實作。Task 8 亦已規劃將 Orders 專屬 MapStore 配置搬回 Orders 模組，避免反向依賴。
- **模組驗證結果**：`src/test/java/com/sivalabs/bookstore/ModularityTests.java` 的 `modules.verify()` 目前綠燈，顯示未有未授權依賴；然而上述兩項耦合仍需在後續任務調整以達到理想隔離。

## 結論
Orders 模組目前的邊界與依賴關係符合抽離前的預期，僅保留 Catalog 與 Common 兩個外部依賴，內聚度高；需要注意的耦合熱點為事件類別與 Hazelcast MapStore 設定，這些都已列在 Phase 3/4 的待辦事項中。完成 Task 7 與 Task 8 後，可望完全消除對 Orders 內部實作的跨模組引用，屆時即可評估最終抽離風險。EOF
