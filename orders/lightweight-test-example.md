# 輕量級測試依賴配置範例

## 1. 最小測試依賴組合

```xml
<!-- 核心測試框架 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mock 框架 -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- 斷言庫 -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- 僅在需要數據庫測試時 -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>

<!-- 僅在需要Spring整合時 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-test</artifactId>
    <scope>test</scope>
</dependency>
```

## 2. 分層測試策略

### 單元測試（80%）
```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTests {
    // 純業務邏輯測試
    // 無需 Spring Context
    // 執行速度：< 100ms
}
```

### 整合測試（15%）
```java
@DataJpaTest
class OrderRepositoryTests {
    // 只測試數據層
    // 最小 Spring Context
    // 執行速度：< 5s
}
```

### 端到端測試（5%）
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
class OrdersIntegrationTests {
    // 完整應用測試
    // 關鍵業務流程
    // 執行速度：< 30s
}
```

## 3. 替代框架選項

### Option A: 完全移除 Spring Boot Test
```xml
<!-- 移除 -->
<!-- <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency> -->

<!-- 替換為 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### Option B: 使用 TestNG
```xml
<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <scope>test</scope>
</dependency>
```

### Option C: 使用 Spock (Groovy)
```xml
<dependency>
    <groupId>org.spockframework</groupId>
    <artifactId>spock-core</artifactId>
    <scope>test</scope>
</dependency>
```

## 4. 自定義測試基礎設施

```java
// 自定義測試基類
public abstract class BaseUnitTest {
    protected static <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz);
    }

    protected static void verifyNoMoreInteractions(Object... mocks) {
        Mockito.verifyNoMoreInteractions(mocks);
    }
}

// 自定義整合測試基類
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "bookstore.cache.enabled=false"
})
public abstract class BaseIntegrationTest {
    // 最小配置的整合測試基礎
}
```

## 5. 效能對比

| 測試類型 | Spring Boot Test | 輕量級替代方案 | 執行時間差異 |
|---------|------------------|---------------|-------------|
| 單元測試 | @SpringBootTest | @ExtendWith(MockitoExtension) | 10x 更快 |
| Repository測試 | @SpringBootTest | @DataJpaTest | 3x 更快 |
| Web測試 | @SpringBootTest | @WebMvcTest | 2x 更快 |
| 整合測試 | @SpringBootTest | 自定義Context | 1.5x 更快 |

## 6. 依賴大小對比

- Spring Boot Starter Test: ~15MB
- JUnit 5 + Mockito: ~3MB
- TestNG + Mockito: ~2.5MB
- 純 JUnit 5: ~1MB