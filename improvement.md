# Improvement Plan

## Hazelcast & Cache Configuration
- Set lazy initial load for MapStore to avoid early `loadAllKeys()`:
  - File: `src/main/java/com/sivalabs/bookstore/config/HazelcastConfig.java`
  - Add: `inventoryMapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.LAZY)`
- Health checks: avoid `size()` (triggers MapStore). Use local stats:
  - File: `src/main/java/com/sivalabs/bookstore/config/HealthConfig.java`
  - Replace `*.size()` with `*.getLocalMapStats().getOwnedEntryCount()` for orders/products/inventory.
- Unify MapStore wiring pattern:
  - Prefer `@SpringAware` + `SpringManagedContext` for all MapStores. Remove `@Component` from `ProductMapStore`/`OrderMapStore` or disable their MapStore until needed.
  - Files: `catalog/cache/ProductMapStore.java`, `orders/cache/OrderMapStore.java`.
- Externalize inventory TTL instead of hardcoding 1800s:
  - File: `config/CacheProperties.java` add `private int inventoryTimeToLiveSeconds = 1800;`
  - Use it in `HazelcastConfig` when configuring `inventory-cache`.

## Cache API & Data Model
- 避免 `values()` 全表掃描：新增索引快取
  - Define `IMap<String, Long> inventoryByProductCode` (key: productCode, value: inventoryId).
  - Update `InventoryCacheService.findByProductCode()` 先查索引再 `get(id)`；維護寫入/更新時的雙向快取。
- 批次查詢 Repository：
  - `ProductRepository#findByCodeIn(Collection<String> codes)`
  - `OrderRepository#findByOrderNumberIn(Collection<String> orderNumbers)`
  - 調整 MapStore `loadAll()` 優先使用批次查詢。

## Health & Observability
- `HealthConfig.testBasicOperations()` 避免寫入；改為可關閉的開關或僅讀取型檢查。
- 聚合統計使用本地統計（hits/gets/puts/ownedEntryCount），避免引發 MapStore。

## Modulith 邊界
- 為各模組補齊 `package-info.java`：`@ApplicationModule`，並用 `@NamedInterface` 明確公開 API/model。
- 僅透過公開介面跨模組呼叫（維持 orders 依賴 `catalog`、`common::common-cache` 的模式）。

## Build & CI
- 對齊 JDK 版本：
  - File: `.github/workflows/maven.yml` → `java-version: 21`（與 `pom.xml` 的 `java.version=21` 一致）。
- 外部化 Liquibase 連線：
  - `pom.xml` 的 Liquibase plugin 連線資訊改為 Maven Profile 或環境變數；執行期改用 `spring.liquibase.*`。

## 錯誤處理與日誌
- MapStore 在 Repository 未就緒時的訊息降級至 DEBUG；僅非預期例外保留 WARN/ERROR。
- 視環境調整 `CacheErrorHandler` 門檻（`FAILURE_THRESHOLD`、復原時間）以降低噪音。

## 建議實作順序（安全漸進）
1) 對齊 CI JDK 版本與 Liquibase 外部化。
2) Hazelcast：啟用 `InitialLoadMode.LAZY`。
3) HealthConfig：移除 `size()`、改用本地統計；健康檢查改為只讀或可關閉。
4) 調整 MapStore 日誌等級，確保不因啟動時序製造 WARN。
5) 新增 `inventoryByProductCode` 索引快取與雙向維護。
6) 加入 Repository 批次查詢並改良 `loadAll()`。
7) 外部化 inventory TTL；補齊 Modulith `package-info.java`/`@NamedInterface`。

---
如需，我可以直接提交上述變更的精準 PR（含小範圍單元測試與文件更新）。
