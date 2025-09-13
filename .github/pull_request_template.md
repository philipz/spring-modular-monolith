## 變更摘要
簡述此次變更內容與目的

## 問題描述
解決什麼問題？關聯 issue：#

## 主要變更
- [ ] 程式碼變更
- [ ] 設定檔調整
- [ ] 文件更新

**影響模組**：`orders` / `catalog` / `inventory` / `notifications` / `common`

## 測試方式
```bash
# 執行的測試指令
./mvnw clean verify
```

**測試場景**：
- [ ] 正常流程
- [ ] 邊界條件（如：快取關閉、資料庫斷線）
- [ ] 錯誤處理

## 檢查清單
- [ ] 程式碼已格式化 (`./mvnw spotless:apply`)
- [ ] 所有測試通過 (`./mvnw clean verify`)
- [ ] 無跨模組違規（遵守 Spring Modulith 邊界）
- [ ] 無敏感資訊外洩

## 部署注意事項
- **資料庫異動**：無 / Liquibase migration 檔案
- **設定變更**：無 / `bookstore.cache.*` 相關設定
- **監控指標**：無 / 新增指標說明

