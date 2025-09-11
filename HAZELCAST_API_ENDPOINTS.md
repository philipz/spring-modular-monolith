# Hazelcast Cache REST API Endpoints

這些新增的 REST API 端點提供查詢和管理 Hazelcast 快取狀態及**實際快取資料**的功能。

## 端點清單

### 快取資料查詢端點

### 1. 取得快取內容摘要 (Cache Data Summary)
- **路徑**: `GET /api/orders/cache/data`
- **描述**: 取得快取內容摘要，包括快取中的訂單總數、所有 key 值、和前 3 筆範例資料
- **回應範例** (快取啟用時):
```json
{
  "available": true,
  "summary": {
    "totalEntries": 5,
    "orderCount": 5,
    "keys": ["ORDER-001", "ORDER-002", "ORDER-003", "ORDER-004", "ORDER-005"],
    "sampleOrders": [
      {
        "id": 1,
        "orderNumber": "ORDER-001",
        "customer": {...},
        "deliveryAddress": "...",
        "item": {...}
      },
      // ... 最多 3 筆範例資料
    ]
  }
}
```

### 2. 取得所有快取訂單 (Get All Cached Orders)
- **路徑**: `GET /api/orders/cache/orders`
- **描述**: 取得快取中所有訂單的完整資料
- **回應範例**:
```json
{
  "available": true,
  "count": 10,
  "orders": [
    {
      "id": 1,
      "orderNumber": "ORDER-001",
      "customer": {
        "name": "John Doe",
        "email": "john@example.com",
        "phone": "1234567890"
      },
      "deliveryAddress": "123 Main St, City",
      "item": {
        "code": "P100",
        "name": "Product Name",
        "price": 99.99,
        "quantity": 2
      },
      "createdAt": "2024-01-01T10:00:00"
    }
    // ... 其他訂單
  ]
}
```

### 3. 取得快取 Key 清單 (Get Cache Keys)
- **路徑**: `GET /api/orders/cache/orders/keys`
- **描述**: 取得快取中所有訂單號碼（key）
- **回應範例**:
```json
{
  "available": true,
  "count": 5,
  "keys": ["ORDER-001", "ORDER-002", "ORDER-003", "ORDER-004", "ORDER-005"]
}
```

### 4. 分頁取得快取訂單 (Get Paginated Cached Orders)
- **路徑**: `GET /api/orders/cache/orders/paginated?limit=10&offset=0`
- **描述**: 以分頁方式取得快取中的訂單資料
- **查詢參數**:
  - `limit`: 每頁資料筆數（預設: 10）
  - `offset`: 起始位置（預設: 0）
- **回應範例**:
```json
{
  "available": true,
  "orders": [
    // ... 訂單陣列
  ],
  "pagination": {
    "limit": 10,
    "offset": 0,
    "count": 10,
    "total": 25,
    "hasMore": true
  }
}
```

### 5. 取得特定快取訂單 (Get Specific Cached Order)
- **路徑**: `GET /api/orders/cache/orders/{orderNumber}`
- **描述**: 根據訂單號碼取得特定快取訂單
- **回應範例** (找到訂單時):
```json
{
  "available": true,
  "found": true,
  "order": {
    "id": 1,
    "orderNumber": "ORDER-001",
    "customer": {...},
    "deliveryAddress": "...",
    "item": {...}
  }
}
```
- **回應範例** (未找到訂單時):
```json
{
  "available": true,
  "found": false,
  "message": "Order not found in cache"
}
```

### 快取管理端點

### 6. 取得快取資訊 (Cache Information)
- **路徑**: `GET /api/orders/cache/info`  
- **描述**: 取得完整的 Hazelcast 快取資訊，包括統計資料、健康狀態、和斷路器狀態
- **回應範例** (快取啟用時):
```json
{
  "status": "enabled",
  "healthy": true,
  "circuitBreakerOpen": false,
  "statistics": "Orders Cache Statistics:\n  Cache Name: orders-cache\n  Cache Size: 0\n...",
  "circuitBreakerStatus": "Cache Circuit Breaker Status:\n  Circuit State: CLOSED (Cache Active)\n...",
  "healthReport": "=== Cache Health Report ===\n..."
}
```
- **回應範例** (快取停用時):
```json
{
  "status": "disabled",
  "message": "Hazelcast cache is not enabled or not available"
}
```

### 7. 檢查快取健康狀態 (Cache Health Check)
- **路徑**: `GET /api/orders/cache/health`
- **描述**: 檢查 Hazelcast 快取的健康狀態和連線能力
- **回應範例** (健康時):
```json
{
  "status": "UP",
  "healthy": true,
  "circuitBreakerOpen": false,
  "connectivity": true
}
```
- **回應範例** (不健康時):
```json
{
  "status": "DOWN",
  "healthy": false,
  "circuitBreakerOpen": true,
  "connectivity": false
}
```
- **HTTP 狀態碼**: 
  - `200 OK`: 快取健康
  - `503 Service Unavailable`: 快取不健康或無法使用

### 8. 取得快取統計 (Cache Statistics)
- **路徑**: `GET /api/orders/cache/stats`
- **描述**: 取得詳細的快取統計資料和斷路器狀態
- **回應範例**:
```json
{
  "available": true,
  "statistics": "Orders Cache Statistics:\n  Cache Name: orders-cache\n  Cache Size: 5\n  Local Map Stats:\n    Owned Entry Count: 3\n    Backup Entry Count: 2\n    Hits: 15\n    Get Operations: 20\n    Put Operations: 8\n",
  "circuitBreakerStatus": "Cache Circuit Breaker Status:\n  Circuit State: CLOSED (Cache Active)\n  Error Statistics:\n..."
}
```

### 9. 測試快取連線 (Test Cache Connectivity)
- **路徑**: `POST /api/orders/cache/test-connectivity`
- **描述**: 執行快取連線測試，驗證快取是否正常運作
- **回應範例**:
```json
{
  "success": true,
  "message": "Cache connectivity test passed",
  "timestamp": 1704067200000
}
```

### 10. 重置斷路器 (Reset Circuit Breaker)
- **路徑**: `POST /api/orders/cache/reset-circuit-breaker`
- **描述**: 手動重置快取斷路器狀態（謹慎使用）
- **回應範例**:
```json
{
  "success": true,
  "message": "Circuit breaker reset successfully",
  "timestamp": 1704067200000
}
```

## 使用範例

### 使用 curl 檢查快取狀態和資料
```bash
# 快取資料查詢
# 取得快取內容摘要
curl -X GET http://localhost:8080/api/orders/cache/data

# 取得所有快取訂單
curl -X GET http://localhost:8080/api/orders/cache/orders

# 取得快取 key 清單
curl -X GET http://localhost:8080/api/orders/cache/orders/keys

# 分頁取得快取訂單
curl -X GET "http://localhost:8080/api/orders/cache/orders/paginated?limit=5&offset=0"

# 取得特定快取訂單
curl -X GET http://localhost:8080/api/orders/cache/orders/ORDER-123

# 快取管理
# 取得完整快取資訊
curl -X GET http://localhost:8080/api/orders/cache/info

# 檢查快取健康狀態
curl -X GET http://localhost:8080/api/orders/cache/health

# 取得快取統計
curl -X GET http://localhost:8080/api/orders/cache/stats

# 測試快取連線
curl -X POST http://localhost:8080/api/orders/cache/test-connectivity

# 重置斷路器（僅在必要時使用）
curl -X POST http://localhost:8080/api/orders/cache/reset-circuit-breaker
```

## 注意事項

1. **快取停用時**: 當 `bookstore.cache.enabled=false` 或 Hazelcast 未配置時，所有端點會回應適當的錯誤訊息。

2. **斷路器機制**: 當快取發生多次錯誤時，斷路器會自動開啟，此時快取操作會轉向資料庫。

3. **安全性**: 重置斷路器的端點應該謹慎使用，建議僅在維護或測試期間使用。

4. **效能考量**: 大量資料時建議使用分頁端點 `/cache/orders/paginated` 避免記憶體過載。

5. **監控整合**: 這些端點可以與監控系統整合，用於快取健康狀態的即時監控。

6. **資料格式**: 所有訂單資料都會轉換為 DTO 格式，隱藏內部實體的敏感資訊。

## 錯誤處理

所有端點都包含適當的錯誤處理：
- 當快取服務不可用時，回應相應的錯誤訊息
- 當發生內部錯誤時，回應 HTTP 500 狀態碼
- 詳細的錯誤資訊會記錄在應用程式日誌中