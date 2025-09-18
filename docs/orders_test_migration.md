# 測試遷移到 Orders 模組的規劃

## 分析結果

經過詳細分析，發現以下 orders 相關測試需要評估是否遷移到獨立的 orders 模組：

### 1. 需要遷移的測試文件

**主要測試類別**：
- `OrdersCacheIntegrationTests.java` - 訂單快取整合測試
- `OrderCacheServiceTests.java` - 訂單快取服務單元測試
- `OrderMapStoreTests.java` - 訂單 MapStore 單元測試
- `OrderRestControllerTests.java` - 訂單 REST API 測試
- `CartUtilTests.java` - 購物車工具類測試

**支援配置**：
- `OrdersCacheTestConfig.java` - 訂單快取測試配置

### 2. 遷移策略

**階段一：核心測試遷移**
1. 遷移純 orders 模組內部測試：
   - `OrderCacheServiceTests.java` → `orders/src/test/java/com/sivalabs/bookstore/orders/cache/`
   - `OrderMapStoreTests.java` → `orders/src/test/java/com/sivalabs/bookstore/orders/cache/`
   - `CartUtilTests.java` → `orders/src/test/java/com/sivalabs/bookstore/orders/web/`

**階段二：API 測試適配**
2. 修改跨模組依賴的測試：
   - `OrderRestControllerTests.java` - 需要適配 `ProductApi` 依賴
   - 將測試中的 `@MockitoBean ProductApi` 改為通過 API 呼叫

**階段三：整合測試處理**
3. 處理複雜整合測試：
   - `OrdersCacheIntegrationTests.java` - 目前已被 `@Disabled`
   - 需要重新設計為適合微服務架構的測試

**階段四：測試配置遷移**
4. 遷移測試支援配置：
   - `OrdersCacheTestConfig.java` → `orders/src/test/java/com/sivalabs/bookstore/orders/config/`

### 3. 修改要點

**依賴處理**：
- 移除對 `catalog.api.ProductApi` 的直接依賴
- 改為通過 HTTP 客戶端測試跨服務呼叫
- 加入必要的測試容器配置

**配置調整**：
- 更新 `@ApplicationModuleTest` 為適合獨立應用的註解
- 調整測試屬性配置
- 確保測試隔離性

**模組邊界**：
- 確保測試只使用 orders 模組的公開 API
- 移除任何違反模組邊界的測試程式碼

### 4. 保留在主專案的測試

**跨模組整合測試**：
- 測試 orders 模組與其他模組互動的測試應保留在主專案
- 涉及完整業務流程的端到端測試

### 5. 實施順序

1. 先遷移純單元測試（無外部依賴）
2. 再處理有依賴的 API 測試
3. 最後處理複雜的整合測試
4. 驗證所有測試在 orders 模組中正常運行
5. 從主專案中移除已遷移的測試

## 目前狀態

- ✅ `OrderMapStoreTests.java` - 已存在於主專案 orders 模組中，無需遷移
- ⏳ 其他測試檔案待評估具體遷移需求
- ⚠️ `OrdersCacheIntegrationTests.java` 目前被停用，需重新設計

## 建議

這個計畫確保了測試的完整性，同時保持了模組邊界的清晰度。建議先從純單元測試開始遷移，逐步處理更複雜的整合測試。