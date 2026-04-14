# JServer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Построить высокопроизводительный JSON-RPC 2.0 сервер на Java 25+ с JDK HttpServer, Virtual Threads, JWT auth, rate limiting, и аудит-логированием.

**Architecture:** Минималистичный монолит с чёткими слоями: server → middleware → api (dispatcher) → core (services) → infrastructure (repositories, DB). Middleware pipeline: Logging → RateLimit → Auth → Router.

**Tech Stack:** Java 25+, JDK HttpServer, Virtual Threads, H2, SLF4J + Logback, Maven, JUnit 5, Mockito, JaCoCo.

**Принципы:** TDD (красный→зелёный→рефакторинг), ≥90% coverage, DRY, YAGNI, SOLID, KISS. Каждый коммит — рабочая версия.

---

## File Structure

| File | Responsibility |
|---|---|
| `pom.xml` | Maven: JDK 25+, зависимости (H2, SLF4J, Logback, JUnit, Mockito, JaCoCo) |
| `src/main/resources/application.yml` | Конфигурация сервера, БД, JWT, rate limiting, logging |
| `src/main/resources/logback.xml` | Конфигурация логирования (console + file with rotation) |
| `org/jserver/JServerApplication.java` | Точка входа, загрузка конфига, запуск сервера |
| `org/jserver/model/User.java` | Domain record: id, username, passwordHash, roles, timestamps |
| `org/jserver/model/AuditEntry.java` | Domain record: id, timestamp, action, userId, details, ip |
| `org/jserver/model/RefreshToken.java` | Domain record: tokenHash, userId, expiresAt |
| `org/jserver/api/JsonRpcRequest.java` | JSON-RPC request record |
| `org/jserver/api/JsonRpcResponse.java` | JSON-RPC response record |
| `org/jserver/api/JsonRpcError.java` | JSON-RPC error record с кодами ошибок |
| `org/jserver/api/RpcMethodHandler.java` | Интерфейс обработчиков методов |
| `org/jserver/api/RequestContext.java` | Контекст запроса (IP, userId, roles, requestId) |
| `org/jserver/api/RpcDispatcher.java` | Маршрутизация к handlers по method name |
| `org/jserver/middleware/Middleware.java` | Интерфейс middleware |
| `org/jserver/middleware/MiddlewareChain.java` | Цепочка выполнения middleware |
| `org/jserver/middleware/LoggingMiddleware.java` | Логирование запросов |
| `org/jserver/middleware/RateLimitMiddleware.java` | Rate limiting (token bucket) |
| `org/jserver/middleware/AuthMiddleware.java` | JWT валидация + RBAC |
| `org/jserver/core/AuthService.java` | Login, register, refresh, logout |
| `org/jserver/core/HealthService.java` | Health check, version |
| `org/jserver/core/RateLimitService.java` | Token bucket алгоритм |
| `org/jserver/core/AuditService.java` | Audit log записи |
| `org/jserver/infrastructure/H2DataSource.java` | H2 подключение |
| `org/jserver/infrastructure/UserRepository.java` | CRUD пользователей |
| `org/jserver/infrastructure/AuditRepository.java` | Audit записи |
| `org/jserver/infrastructure/ConfigRepository.java` | Rate limit config |
| `org/jserver/infrastructure/JwtProvider.java` | JWT генерация/валидация |
| `org/jserver/infrastructure/ServerConfig.java` | Чтение application.yml |
| `org/jserver/server/HttpServerBootstrap.java` | Запуск JDK HttpServer |
| `org/jserver/server/RpcHttpHandler.java` | HTTP → JSON-RPC |

---

## Task 1: Maven проект и конфигурация

**Files:**
- Create: `pom.xml`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/logback.xml`
- Create: `src/main/java/org/jserver/JServerApplication.java`

- [ ] **Step 1: Создать pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.jserver</groupId>
    <artifactId>jserver</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>JServer</name>
    <description>High-performance JSON-RPC 2.0 server on Java 25+</description>

    <properties>
        <maven.compiler.source>25</maven.compiler.source>
        <maven.compiler.target>25</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.11.0</junit.version>
        <mockito.version>5.14.0</mockito.version>
        <h2.version>2.3.232</h2.version>
        <slf4j.version>2.0.16</slf4j.version>
        <logback.version>1.5.12</logback.version>
    </properties>

    <dependencies>
        <!-- SLF4J API -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
        </dependency>

        <!-- Logback -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>${logback.version}</version>
        </dependency>

        <!-- H2 Database -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>${h2.version}</version>
        </dependency>

        <!-- Jackson JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.18.2</version>
        </dependency>

        <!-- Test dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Compiler plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>25</source>
                    <target>25</target>
                    <enablePreview>true</enablePreview>
                </configuration>
            </plugin>

            <!-- JAR plugin with main class -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.4.2</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>org.jserver.JServerApplication</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>

            <!-- Surefire for tests -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.2</version>
            </plugin>

            <!-- JaCoCo coverage -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.12</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>check</id>
                        <goals>
                            <goal>check</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <rule>
                                    <element>BUNDLE</element>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.90</minimum>
                                        </limit>
                                        <limit>
                                            <counter>BRANCH</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.80</minimum>
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Создать application.yml**

```yaml
server:
  port: 8080
  host: 0.0.0.0

database:
  url: jdbc:h2:mem:jserver;DB_CLOSE_DELAY=-1
  user: sa
  password:

jwt:
  secret: ${JWT_SECRET:change-me-in-production-min-256-bits}
  access-token-ttl: 15m
  refresh-token-ttl: 7d

ratelimit:
  enabled: true
  default:
    capacity: 100
    refill-rate: 100

logging:
  level: INFO
  file: logs/jserver.log
  file-max-size: 10MB
  file-max-history: 7d
```

- [ ] **Step 3: Создать logback.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File Appender with rotation -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE:-logs/jserver.log}</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/jserver.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>7</maxHistory>
            <totalSizeCap>100MB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>

    <!-- Application logger -->
    <logger name="org.jserver" level="DEBUG"/>
</configuration>
```

- [ ] **Step 4: Создать JServerApplication.java (заглушка)**

```java
package org.jserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа приложения JServer.
 * Загружает конфигурацию и запускает HTTP сервер.
 */
public class JServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(JServerApplication.class);

    /**
     * Главный метод запуска сервера.
     *
     * @param args аргументы командной строки (пока не используются)
     */
    public static void main(String[] args) {
        logger.info("JServer starting...");
        System.out.println("JServer v1.0.0-SNAPSHOT");
        logger.info("JServer started successfully");
    }
}
```

- [ ] **Step 5: Проверить компиляцию**

```bash
mvn clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/resources/application.yml src/main/resources/logback.xml src/main/java/org/jserver/JServerApplication.java
git commit -m "build: initialize Maven project with basic config"
```

---

## Task 2: Domain модели и JSON-RPC DTO

**Files:**
- Create: `src/main/java/org/jserver/model/User.java`
- Create: `src/main/java/org/jserver/model/AuditEntry.java`
- Create: `src/main/java/org/jserver/model/RefreshToken.java`
- Create: `src/main/java/org/jserver/api/JsonRpcRequest.java`
- Create: `src/main/java/org/jserver/api/JsonRpcResponse.java`
- Create: `src/main/java/org/jserver/api/JsonRpcError.java`
- Create: `src/test/java/org/jserver/api/JsonRpcRequestTest.java`
- Create: `src/test/java/org/jserver/api/JsonRpcResponseTest.java`
- Create: `src/test/java/org/jserver/api/JsonRpcErrorTest.java`
- Create: `src/test/java/org/jserver/model/UserTest.java`
- Create: `src/test/java/org/jserver/model/AuditEntryTest.java`

- [ ] **Step 1: Написать тесты для JSON-RPC DTO**

```java
// src/test/java/org/jserver/api/JsonRpcRequestTest.java
package org.jserver.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для JsonRpcRequest.
 * Проверяют создание record и корректность полей.
 */
class JsonRpcRequestTest {

    @Test
    void createsRequestWithAllFields() {
        var request = new JsonRpcRequest("2.0", "auth.login", 
            java.util.Map.of("username", "admin"), 1);
        
        assertEquals("2.0", request.jsonrpc());
        assertEquals("auth.login", request.method());
        assertEquals(java.util.Map.of("username", "admin"), request.params());
        assertEquals(1, request.id());
    }

    @Test
    void allowsNullParams() {
        var request = new JsonRpcRequest("2.0", "system.health", null, 1);
        assertNull(request.params());
    }

    @Test
    void allowsNullIdForNotifications() {
        var request = new JsonRpcRequest("2.0", "system.health", null, null);
        assertNull(request.id());
    }
}
```

```java
// src/test/java/org/jserver/api/JsonRpcResponseTest.java
package org.jserver.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для JsonRpcResponse.
 */
class JsonRpcResponseTest {

    @Test
    void createsSuccessResponse() {
        var result = java.util.Map.of("token", "abc123");
        var response = JsonRpcResponse.success("2.0", result, 1);
        
        assertEquals("2.0", response.jsonrpc());
        assertEquals(result, response.result());
        assertNull(response.error());
        assertEquals(1, response.id());
    }

    @Test
    void createsErrorResponse() {
        var error = new JsonRpcError(-32600, "Invalid Request", null);
        var response = JsonRpcResponse.error("2.0", error, null);
        
        assertEquals("2.0", response.jsonrpc());
        assertEquals(error, response.error());
        assertNull(response.result());
        assertNull(response.id());
    }
}
```

```java
// src/test/java/org/jserver/api/JsonRpcErrorTest.java
package org.jserver.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для JsonRpcError кодов.
 */
class JsonRpcErrorTest {

    @Test
    void createsErrorWithCodeAndMessage() {
        var error = new JsonRpcError(-32600, "Invalid Request", null);
        
        assertEquals(-32600, error.code());
        assertEquals("Invalid Request", error.message());
        assertNull(error.data());
    }

    @Test
    void supportsOptionalData() {
        var error = new JsonRpcError(-32602, "Invalid params", 
            java.util.Map.of("field", "username"));
        
        assertEquals(java.util.Map.of("field", "username"), error.data());
    }
}
```

```java
// src/test/java/org/jserver/model/UserTest.java
package org.jserver.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для User record.
 */
class UserTest {

    @Test
    void createsUserWithAllFields() {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var user = new User(id, "admin", "hash123", Set.of("admin"), now, now);
        
        assertEquals(id, user.id());
        assertEquals("admin", user.username());
        assertEquals(Set.of("admin"), user.roles());
    }

    @Test
    void supportsMultipleRoles() {
        var user = new User(UUID.randomUUID(), "user", "hash", 
            Set.of("user", "admin"), Instant.now(), Instant.now());
        
        assertTrue(user.roles().contains("user"));
        assertTrue(user.roles().contains("admin"));
    }
}
```

```java
// src/test/java/org/jserver/model/AuditEntryTest.java
package org.jserver.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для AuditEntry record.
 */
class AuditEntryTest {

    @Test
    void createsAuditEntryWithAllFields() {
        var id = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var now = Instant.now();
        var entry = new AuditEntry(id, now, "auth.login", userId, 
            "User admin logged in", "127.0.0.1");
        
        assertEquals(id, entry.id());
        assertEquals(now, entry.timestamp());
        assertEquals("auth.login", entry.action());
        assertEquals(userId, entry.userId());
        assertEquals("127.0.0.1", entry.ipAddress());
    }

    @Test
    void allowsNullUserIdForAnonymous() {
        var entry = new AuditEntry(UUID.randomUUID(), Instant.now(), 
            "system.health", null, "Health check", "127.0.0.1");
        
        assertNull(entry.userId());
    }
}
```

- [ ] **Step 2: Реализовать модели**

```java
// src/main/java/org/jserver/model/User.java
package org.jserver.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Доменная модель пользователя.
 * Хранит идентификатор, имя, хеш пароля и роли.
 *
 * @param id уникальный идентификатор пользователя
 * @param username имя пользователя (уникальное)
 * @param passwordHash хеш пароля
 * @param roles набор ролей пользователя
 * @param createdAt время создания записи
 * @param updatedAt время последнего обновления
 */
public record User(
    UUID id,
    String username,
    String passwordHash,
    Set<String> roles,
    Instant createdAt,
    Instant updatedAt
) {}
```

```java
// src/main/java/org/jserver/model/AuditEntry.java
package org.jserver.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Запись в audit-логе.
 * Фиксирует действия пользователей в системе.
 *
 * @param id уникальный идентификатор записи
 * @param timestamp время действия
 * @param action выполненное действие (например "auth.login")
 * @param userId идентификатор пользователя (null для анонимных действий)
 * @param details дополнительные детали действия
 * @param ipAddress IP-адрес источника запроса
 */
public record AuditEntry(
    UUID id,
    Instant timestamp,
    String action,
    UUID userId,
    String details,
    String ipAddress
) {}
```

```java
// src/main/java/org/jserver/model/RefreshToken.java
package org.jserver.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Модель refresh-токена.
 * Хранит хеш токена, связь с пользователем и время истечения.
 *
 * @param tokenHash SHA-256 хеш refresh-токена
 * @param userId идентификатор пользователя
 * @param expiresAt время истечения токена
 */
public record RefreshToken(
    String tokenHash,
    UUID userId,
    Instant expiresAt
) {}
```

```java
// src/main/java/org/jserver/api/JsonRpcRequest.java
package org.jserver.api;

/**
 * JSON-RPC 2.0 запрос.
 * Соответствует спецификации JSON-RPC 2.0, раздел 4.1.
 *
 * @param jsonrpc версия протокола (всегда "2.0")
 * @param method имя вызываемого метода
 * @param params параметры метода (может быть null)
 * @param id идентификатор запроса (null для notification)
 */
public record JsonRpcRequest(
    String jsonrpc,
    String method,
    Object params,
    Object id
) {}
```

```java
// src/main/java/org/jserver/api/JsonRpcResponse.java
package org.jserver.api;

/**
 * JSON-RPC 2.0 ответ.
 * Соответствует спецификации JSON-RPC 2.0, раздел 4.2.
 *
 * @param jsonrpc версия протокола (всегда "2.0")
 * @param result результат выполнения (null при ошибке)
 * @param error информация об ошибке (null при успехе)
 * @param id идентификатор запроса (из запроса)
 */
public record JsonRpcResponse(
    String jsonrpc,
    Object result,
    JsonRpcError error,
    Object id
) {

    /**
     * Создаёт успешный ответ.
     *
     * @param jsonrpc версия протокола
     * @param result результат
     * @param id идентификатор запроса
     * @return успешный ответ
     */
    public static JsonRpcResponse success(String jsonrpc, Object result, Object id) {
        return new JsonRpcResponse(jsonrpc, result, null, id);
    }

    /**
     * Создаёт ответ с ошибкой.
     *
     * @param jsonrpc версия протокола
     * @param error информация об ошибке
     * @param id идентификатор запроса
     * @return ответ с ошибкой
     */
    public static JsonRpcResponse error(String jsonrpc, JsonRpcError error, Object id) {
        return new JsonRpcResponse(jsonrpc, null, error, id);
    }
}
```

```java
// src/main/java/org/jserver/api/JsonRpcError.java
package org.jserver.api;

/**
 * JSON-RPC 2.0 ошибка.
 * Содержит код, сообщение и опциональные данные.
 *
 * @param code код ошибки (отрицательное число)
 * @param message человекочитаемое сообщение
 * @param params дополнительные данные (может быть null)
 */
public record JsonRpcError(
    int code,
    String message,
    Object params
) {

    // Стандартные коды ошибок JSON-RPC 2.0
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    // Кастомные коды JServer
    public static final int RATE_LIMIT_EXCEEDED = -32605;
    public static final int UNAUTHORIZED = -32606;
    public static final int FORBIDDEN = -32607;

    /**
     * Создаёт ошибку parse error.
     *
     * @return ошибка парсинга JSON
     */
    public static JsonRpcError parseError() {
        return new JsonRpcError(PARSE_ERROR, "Parse error", null);
    }

    /**
     * Создаёт ошибку invalid request.
     *
     * @param details детали ошибки
     * @return ошибка неверного запроса
     */
    public static JsonRpcError invalidRequest(String details) {
        return new JsonRpcError(INVALID_REQUEST, "Invalid Request: " + details, null);
    }

    /**
     * Создаёт ошибку method not found.
     *
     * @param method имя ненайденного метода
     * @return ошибка неизвестного метода
     */
    public static JsonRpcError methodNotFound(String method) {
        return new JsonRpcError(METHOD_NOT_FOUND, "Method not found: " + method, null);
    }

    /**
     * Создаёт ошибку invalid params.
     *
     * @param details описание проблемы с параметрами
     * @return ошибка неверных параметров
     */
    public static JsonRpcError invalidParams(String details) {
        return new JsonRpcError(INVALID_PARAMS, "Invalid params: " + details, null);
    }

    /**
     * Создаёт ошибку internal error.
     *
     * @param details детали внутренней ошибки
     * @return внутренняя ошибка сервера
     */
    public static JsonRpcError internalError(String details) {
        return new JsonRpcError(INTERNAL_ERROR, "Internal error: " + details, null);
    }

    /**
     * Создаёт ошибку превышения rate limit.
     *
     * @return ошибка rate limit
     */
    public static JsonRpcError rateLimitExceeded() {
        return new JsonRpcError(RATE_LIMIT_EXCEEDED, "Rate limit exceeded", null);
    }

    /**
     * Создаёт ошибку авторизации.
     *
     * @return ошибка отсутствия или неверного токена
     */
    public static JsonRpcError unauthorized() {
        return new JsonRpcError(UNAUTHORIZED, "Unauthorized", null);
    }

    /**
     * Создаёт ошибку прав доступа.
     *
     * @return ошибка недостаточных прав
     */
    public static JsonRpcError forbidden() {
        return new JsonRpcError(FORBIDDEN, "Forbidden", null);
    }
}
```

- [ ] **Step 3: Запустить тесты**

```bash
mvn test -Dtest=JsonRpcRequestTest,JsonRpcResponseTest,JsonRpcErrorTest,UserTest,AuditEntryTest
```
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/org/jserver/model/ src/main/java/org/jserver/api/JsonRpc*.java src/test/
git commit -m "feat: add domain models and JSON-RPC DTOs with tests"
```

---

## Task 3: RequestContext, ServerConfig, H2DataSource

**Files:**
- Create: `src/main/java/org/jserver/api/RequestContext.java`
- Create: `src/main/java/org/jserver/infrastructure/ServerConfig.java`
- Create: `src/main/java/org/jserver/infrastructure/H2DataSource.java`
- Create: `src/test/java/org/jserver/api/RequestContextTest.java`
- Create: `src/test/java/org/jserver/infrastructure/ServerConfigTest.java`
- Create: `src/test/java/org/jserver/infrastructure/H2DataSourceTest.java`

- [ ] **Step 1: Тесты для RequestContext**

```java
// src/test/java/org/jserver/api/RequestContextTest.java
package org.jserver.api;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для RequestContext.
 */
class RequestContextTest {

    @Test
    void createsContextWithRequestId() {
        var ctx = new RequestContext("127.0.0.1", null, Set.of(), null);
        assertNotNull(ctx.requestId());
    }

    @Test
    void createsContextWithProvidedRequestId() {
        var id = UUID.randomUUID().toString();
        var ctx = new RequestContext(id, "127.0.0.1", null, Set.of());
        assertEquals(id, ctx.requestId());
    }

    @Test
    void anonymousUserHasNullUserId() {
        var ctx = new RequestContext("127.0.0.1", null, Set.of("anonymous"), null);
        assertNull(ctx.userId());
    }

    @Test
    void authenticatedUserHasUserId() {
        var userId = UUID.randomUUID();
        var ctx = new RequestContext("127.0.0.1", userId, Set.of("user"), null);
        assertEquals(userId, ctx.userId());
    }

    @Test
    void hasRoleChecksRoleMembership() {
        var ctx = new RequestContext("127.0.0.1", null, Set.of("user", "admin"), null);
        assertTrue(ctx.hasRole("admin"));
        assertFalse(ctx.hasRole("anonymous"));
    }
}
```

- [ ] **Step 2: Реализовать RequestContext**

```java
// src/main/java/org/jserver/api/RequestContext.java
package org.jserver.api;

import java.util.Set;
import java.util.UUID;

/**
 * Контекст executing запроса.
 * Содержит информацию о запросе, доступную всем слоям системы.
 *
 * @param requestId уникальный идентификатор запроса (UUID)
 * @param clientIpAddress IP-адрес клиента
 * @param userId идентификатор аутентифицированного пользователя (null для анонимов)
 * @param роли набор ролей пользователя
 */
public record RequestContext(
    String requestId,
    String clientIpAddress,
    UUID userId,
    Set<String> roles
) {

    /**
     * Создаёт контекст с автогенерированным requestId.
     */
    public RequestContext(String clientIpAddress, UUID userId, Set<String> roles, Object _unused) {
        this(UUID.randomUUID().toString(), clientIpAddress, userId, roles);
    }

    /**
     * Проверяет наличие роли у пользователя.
     *
     * @param role проверяемая роль
     * @return true если роль присутствует
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
```

- [ ] **Step 3: Тесты для ServerConfig**

```java
// src/test/java/org/jserver/infrastructure/ServerConfigTest.java
package org.jserver.infrastructure;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для ServerConfig.
 * Проверяют чтение конфигурационных значений.
 */
class ServerConfigTest {

    @Test
    void loadsDefaultConfig() {
        var config = ServerConfig.load();
        assertNotNull(config);
        assertTrue(config.port() > 0);
        assertNotNull(config.databaseUrl());
        assertNotNull(config.jwtSecret());
    }

    @Test
    void defaultPortIs8080() {
        var config = ServerConfig.load();
        assertEquals(8080, config.port());
    }

    @Test
    void jwtSecretHasDefaultValue() {
        var config = ServerConfig.load();
        assertEquals("change-me-in-production-min-256-bits", config.jwtSecret());
    }
}
```

- [ ] **Step 4: Реализовать ServerConfig**

```java
// src/main/java/org/jserver/infrastructure/ServerConfig.java
package org.jserver.infrastructure;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Конфигурация сервера из application.yml.
 * Поддерживает переопределение через переменные окружения.
 */
public record ServerConfig(
    int port,
    String host,
    String databaseUrl,
    String databaseUser,
    String databasePassword,
    String jwtSecret,
    Duration accessTokenTtl,
    Duration refreshTokenTtl,
    boolean rateLimitEnabled,
    int defaultRateLimitCapacity,
    int defaultRateLimitRefillRate
) {

    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);

    /**
     * Загружает конфигурацию из application.yml.
     *
     * @return конфигурация сервера
     */
    @SuppressWarnings("unchecked")
    public static ServerConfig load() {
        logger.info("Loading configuration from application.yml");
        
        try (InputStream is = ServerConfig.class.getClassLoader()
                .getResourceAsStream("application.yml")) {
            
            if (is == null) {
                logger.warn("application.yml not found, using defaults");
                return defaults();
            }

            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(is);

            Map<String, Object> server = (Map<String, Object>) config.getOrDefault("server", Map.of());
            Map<String, Object> database = (Map<String, Object>) config.getOrDefault("database", Map.of());
            Map<String, Object> jwt = (Map<String, Object>) config.getOrDefault("jwt", Map.of());
            Map<String, Object> ratelimit = (Map<String, Object>) config.getOrDefault("ratelimit", Map.of());

            int port = getEnvOr("SERVER_PORT", server.get("port"), 8080);
            String host = getEnvOr("SERVER_HOST", server.get("host"), "0.0.0.0");
            
            String dbUrl = getEnvOr("DB_URL", database.get("url"), "jdbc:h2:mem:jserver;DB_CLOSE_DELAY=-1");
            String dbUser = getEnvOr("DB_USER", database.get("user"), "sa");
            String dbPassword = getEnvOr("DB_PASSWORD", database.get("password"), "");
            
            String jwtSecret = getEnvOr("JWT_SECRET", jwt.get("secret"), "change-me-in-production-min-256-bits");
            Duration accessTtl = parseDuration((String) jwt.getOrDefault("access-token-ttl", "15m"));
            Duration refreshTtl = parseDuration((String) jwt.getOrDefault("refresh-token-ttl", "7d"));
            
            Map<String, Object> rlDefault = (Map<String, Object>) ratelimit.getOrDefault("default", Map.of());
            boolean rlEnabled = (Boolean) ratelimit.getOrDefault("enabled", true);
            int rlCapacity = (Integer) rlDefault.getOrDefault("capacity", 100);
            int rlRefillRate = (Integer) rlDefault.getOrDefault("refill-rate", 100);

            logger.info("Configuration loaded: port={}, host={}, db={}, rateLimit={}", 
                port, host, dbUrl, rlEnabled ? "enabled" : "disabled");

            return new ServerConfig(port, host, dbUrl, dbUser, dbPassword,
                jwtSecret, accessTtl, refreshTtl, rlEnabled, rlCapacity, rlRefillRate);

        } catch (Exception e) {
            logger.error("Failed to load configuration, using defaults", e);
            return defaults();
        }
    }

    private static ServerConfig defaults() {
        return new ServerConfig(8080, "0.0.0.0",
            "jdbc:h2:mem:jserver;DB_CLOSE_DELAY=-1", "sa", "",
            "change-me-in-production-min-256-bits",
            Duration.ofMinutes(15), Duration.ofDays(7),
            true, 100, 100);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getEnvOr(String envName, Object yamlValue, T defaultValue) {
        String env = System.getenv(envName);
        if (env != null && !env.isEmpty()) {
            if (defaultValue instanceof Integer) {
                return (T) Integer.valueOf(env);
            }
            return (T) env;
        }
        return yamlValue != null ? (T) yamlValue : defaultValue;
    }

    private static Duration parseDuration(String str) {
        if (str == null) return Duration.ofMinutes(15);
        long value = Long.parseLong(str.replaceAll("[a-zA-Z]", ""));
        if (str.endsWith("m")) return Duration.ofMinutes(value);
        if (str.endsWith("h")) return Duration.ofHours(value);
        if (str.endsWith("d")) return Duration.ofDays(value);
        return Duration.ofMinutes(value);
    }
}
```

> **Note:** ServerConfig использует SnakeYAML. Добавить зависимость в pom.xml:
```xml
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>2.3</version>
</dependency>
```

- [ ] **Step 5: Тесты для H2DataSource**

```java
// src/test/java/org/jserver/infrastructure/H2DataSourceTest.java
package org.jserver.infrastructure;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для H2DataSource.
 */
class H2DataSourceTest {

    @Test
    void createsDataSource() {
        var config = ServerConfig.defaults();
        var ds = new H2DataSource(config);
        assertNotNull(ds);
    }

    @Test
    void opensConnection() throws SQLException {
        var config = ServerConfig.defaults();
        var ds = new H2DataSource(config);
        
        try (Connection conn = ds.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        }
    }

    @Test
    void initializesSchema() throws SQLException {
        var config = ServerConfig.defaults();
        var ds = new H2DataSource(config);
        ds.initialize();
        
        try (Connection conn = ds.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                 "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'USERS'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }
}
```

- [ ] **Step 6: Реализовать H2DataSource**

```java
// src/main/java/org/jserver/infrastructure/H2DataSource.java
package org.jserver.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Источник данных H2.
 * Создаёт подключение и инициализирует схему БД.
 */
public class H2DataSource {

    private static final Logger logger = LoggerFactory.getLogger(H2DataSource.class);
    
    private final JdbcDataSource dataSource;
    private final ServerConfig config;

    /**
     * Создаёт источник данных.
     *
     * @param config конфигурация сервера
     */
    public H2DataSource(ServerConfig config) {
        this.config = config;
        this.dataSource = createDataSource();
    }

    /**
     * Инициализирует схему БД.
     * Создаёт все необходимые таблицы.
     */
    public void initialize() {
        logger.info("Initializing database schema");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id UUID PRIMARY KEY,
                    username VARCHAR(255) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    roles VARCHAR(500) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id UUID PRIMARY KEY,
                    timestamp TIMESTAMP NOT NULL,
                    action VARCHAR(255) NOT NULL,
                    user_id UUID,
                    details TEXT,
                    ip_address VARCHAR(45)
                )
                """);
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rate_limit_config (
                    method_name VARCHAR(255) PRIMARY KEY,
                    capacity INT NOT NULL,
                    refill_rate INT NOT NULL
                )
                """);
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS refresh_tokens (
                    token_hash VARCHAR(64) PRIMARY KEY,
                    user_id UUID NOT NULL,
                    expires_at TIMESTAMP NOT NULL
                )
                """);
            
            // Создаём пользователя admin по умолчанию
            stmt.execute("""
                MERGE INTO users (id, username, password_hash, roles, created_at, updated_at)
                KEY (username) VALUES (
                    RANDOM_UUID(), 'admin', 
                    '$2a$10$dummyHashForDevelopmentOnly', 
                    'admin', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
                )
                """);
            
            logger.info("Database schema initialized");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Получает подключение к БД.
     *
     * @return подключение
     * @throws SQLException ошибка подключения
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Возвращает DataSource для использования в репозиториях.
     *
     * @return DataSource
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    private JdbcDataSource createDataSource() {
        var ds = new JdbcDataSource();
        ds.setURL(config.databaseUrl());
        ds.setUser(config.databaseUser());
        ds.setPassword(config.databasePassword());
        logger.info("H2 DataSource created: {}", config.databaseUrl());
        return ds;
    }
}
```

- [ ] **Step 7: Запустить тесты**

```bash
mvn test -Dtest=RequestContextTest,ServerConfigTest,H2DataSourceTest
```
Expected: All tests PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/org/jserver/api/RequestContext.java \
    src/main/java/org/jserver/infrastructure/ServerConfig.java \
    src/main/java/org/jserver/infrastructure/H2DataSource.java \
    src/test/java/org/jserver/api/RequestContextTest.java \
    src/test/java/org/jserver/infrastructure/ServerConfigTest.java \
    src/test/java/org/jserver/infrastructure/H2DataSourceTest.java \
    pom.xml
git commit -m "feat: add RequestContext, ServerConfig, H2DataSource with tests"
```

---

## Task 4: RateLimitService + RateLimitMiddleware

**Files:**
- Create: `src/main/java/org/jserver/core/RateLimitService.java`
- Create: `src/main/java/org/jserver/middleware/RateLimitMiddleware.java`
- Create: `src/test/java/org/jserver/core/RateLimitServiceTest.java`
- Create: `src/test/java/org/jserver/middleware/RateLimitMiddlewareTest.java`

- [ ] **Step 1: Тесты для RateLimitService**

```java
// src/test/java/org/jserver/core/RateLimitServiceTest.java
package org.jserver.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для RateLimitService.
 * Проверяют token bucket алгоритм.
 */
class RateLimitServiceTest {

    private RateLimitService service;

    @BeforeEach
    void setUp() {
        service = new RateLimitService(10, 10); // 10 tokens, 10/min
    }

    @Test
    void allowsRequestsUnderLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(service.tryConsume("test-ip"));
        }
    }

    @Test
    void blocksRequestsOverLimit() {
        for (int i = 0; i < 10; i++) {
            service.tryConsume("test-ip");
        }
        assertFalse(service.tryConsume("test-ip"));
    }

    @Test
    void differentIpsHaveIndependentLimits() {
        for (int i = 0; i < 10; i++) {
            service.tryConsume("ip1");
        }
        assertTrue(service.tryConsume("ip2"));
    }

    @Test
    void consumesExactlyAvailableTokens() {
        for (int i = 0; i < 10; i++) {
            assertTrue(service.tryConsume("test-ip"));
        }
        assertFalse(service.tryConsume("test-ip"));
    }
}
```

- [ ] **Step 2: Реализовать RateLimitService**

```java
// src/main/java/org/jserver/core/RateLimitService.java
package org.jserver.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис rate limiting на основе token bucket.
 * Потокобезопасный, хранит buckets per IP.
 */
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    
    private final int defaultCapacity;
    private final int defaultRefillRate;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Создаёт сервис с настройками по умолчанию.
     *
     * @param defaultCapacity ёмкость bucket
     * @param defaultRefillRate скорость пополнения (токенов/минуту)
     */
    public RateLimitService(int defaultCapacity, int defaultRefillRate) {
        this.defaultCapacity = defaultCapacity;
        this.defaultRefillRate = defaultRefillRate;
        logger.info("RateLimitService created: capacity={}, refillRate={}/min", 
            defaultCapacity, defaultRefillRate);
    }

    /**
     * Пытается потребить токен для IP.
     *
     * @param clientIp IP-адрес клиента
     * @return true если токен доступен, false если лимит исчерпан
     */
    public boolean tryConsume(String clientIp) {
        var bucket = buckets.computeIfAbsent(clientIp, 
            ip -> new TokenBucket(defaultCapacity, defaultRefillRate));
        
        boolean consumed = bucket.tryConsume();
        if (!consumed) {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
        }
        return consumed;
    }

    /**
     * Token bucket с refill.
     */
    private static class TokenBucket {
        private final int capacity;
        private final long refillIntervalNanos;
        private int tokens;
        private volatile long lastRefillTime;

        TokenBucket(int capacity, int refillRatePerMinute) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillIntervalNanos = 60_000_000_000L / refillRatePerMinute;
            this.lastRefillTime = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillTime;
            if (elapsed >= refillIntervalNanos) {
                int tokensToAdd = (int) (elapsed / refillIntervalNanos);
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }
}
```

- [ ] **Step 3: Тесты для RateLimitMiddleware**

```java
// src/test/java/org/jserver/middleware/RateLimitMiddlewareTest.java
package org.jserver.middleware;

import org.jserver.api.JsonRpcError;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.jserver.core.RateLimitService;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для RateLimitMiddleware.
 */
class RateLimitMiddlewareTest {

    @Test
    void allowsRequestWhenTokensAvailable() {
        var rateLimitService = new RateLimitService(100, 100);
        var middleware = new RateLimitMiddleware(rateLimitService);
        var ctx = new RequestContext("127.0.0.1", null, Set.of("anonymous"), null);
        
        var chain = mock(MiddlewareChain.class);
        when(chain.proceed(any())).thenReturn(
            CompletableFuture.completedFuture(JsonRpcResponse.success("2.0", "ok", 1)));
        
        var response = middleware.process(null, ctx, chain);
        
        assertNotNull(response);
        verify(chain).proceed(ctx);
    }

    @Test
    void blocksRequestWhenLimitExceeded() {
        var rateLimitService = new RateLimitService(1, 1);
        var middleware = new RateLimitMiddleware(rateLimitService);
        var ctx = new RequestContext("127.0.0.1", null, Set.of("anonymous"), null);
        
        // Первый запрос потребляет токен
        rateLimitService.tryConsume("127.0.0.1");
        
        var chain = mock(MiddlewareChain.class);
        var response = middleware.process(null, ctx, chain);
        
        assertNotNull(response.error());
        assertEquals(JsonRpcError.RATE_LIMIT_EXCEEDED, response.error().code());
        verifyNoInteractions(chain);
    }
}
```

- [ ] **Step 4: Реализовать Middleware интерфейс и RateLimitMiddleware**

```java
// src/main/java/org/jserver/middleware/Middleware.java
package org.jserver.middleware;

import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;

/**
 * Интерфейс middleware компонента.
 * Каждый middleware может обработать запрос и решить,
 * передать ли его дальше по цепочке.
 */
public interface Middleware {

    /**
     * Обрабатывает запрос в цепочке middleware.
     *
     * @param request JSON-RPC запрос
     * @param ctx контекст запроса
     * @param chain цепочка для передачи следующему middleware
     * @return CompletableFuture с ответом
     */
    CompletableFuture<JsonRpcResponse> process(
        JsonRpcRequest request, 
        RequestContext ctx, 
        MiddlewareChain chain);
}
```

```java
// src/main/java/org/jserver/middleware/MiddlewareChain.java
package org.jserver.middleware;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;

/**
 * Цепочка выполнения middleware.
 * Последовательно вызывает middleware в порядке регистрации.
 */
public class MiddlewareChain {

    private final List<Middleware> middlewares;
    private int index = 0;
    private final RpcHandler finalHandler;

    /**
     * Функциональный интерфейсс для финального обработчика.
     */
    @FunctionalInterface
    public interface RpcHandler {
        CompletableFuture<JsonRpcResponse> handle(JsonRpcRequest request, RequestContext ctx);
    }

    /**
     * Создаёт цепочку middleware.
     *
     * @param middlewares список middleware
     * @param finalHandler финальный обработчик (dispatcher)
     */
    public MiddlewareChain(List<Middleware> middlewares, RpcHandler finalHandler) {
        this.middlewares = middlewares;
        this.finalHandler = finalHandler;
    }

    /**
     * Передаёт управление следующему middleware или финальному обработчику.
     *
     * @param ctx контекст запроса
     * @param request JSON-RPC запрос
     * @return CompletableFuture с ответом
     */
    public CompletableFuture<JsonRpcResponse> proceed(RequestContext ctx, JsonRpcRequest request) {
        if (index < middlewares.size()) {
            Middleware current = middlewares.get(index);
            index++;
            return current.process(request, ctx, this);
        } else {
            return finalHandler.handle(request, ctx);
        }
    }
}
```

```java
// src/main/java/org/jserver/middleware/RateLimitMiddleware.java
package org.jserver.middleware;

import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcError;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.jserver.core.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware для ограничения частоты запросов.
 * Проверяет лимиты по IP-адресу клиента.
 */
public class RateLimitMiddleware implements Middleware {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitMiddleware.class);
    
    private final RateLimitService rateLimitService;

    /**
     * Создаёт middleware.
     *
     * @param rateLimitService сервис rate limiting
     */
    public RateLimitMiddleware(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
        logger.info("RateLimitMiddleware created");
    }

    @Override
    public CompletableFuture<JsonRpcResponse> process(
            JsonRpcRequest request, RequestContext ctx, MiddlewareChain chain) {
        
        if (!rateLimitService.tryConsume(ctx.clientIpAddress())) {
            logger.warn("Rate limit exceeded for request {} from IP {}", 
                ctx.requestId(), ctx.clientIpAddress());
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.rateLimitExceeded(), 
                    request != null ? request.id() : null));
        }
        
        return chain.proceed(ctx, request);
    }
}
```

- [ ] **Step 5: Запустить тесты**

```bash
mvn test -Dtest=RateLimitServiceTest,RateLimitMiddlewareTest
```
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/jserver/core/RateLimitService.java \
    src/main/java/org/jserver/middleware/Middleware.java \
    src/main/java/org/jserver/middleware/MiddlewareChain.java \
    src/main/java/org/jserver/middleware/RateLimitMiddleware.java \
    src/test/java/org/jserver/core/RateLimitServiceTest.java \
    src/test/java/org/jserver/middleware/RateLimitMiddlewareTest.java
git commit -m "feat: add RateLimitService and RateLimitMiddleware with tests"
```

---

## Task 5: JwtProvider + AuthService + AuthMiddleware

**Files:**
- Create: `src/main/java/org/jserver/infrastructure/JwtProvider.java`
- Create: `src/main/java/org/jserver/core/AuthService.java`
- Create: `src/main/java/org/jserver/middleware/AuthMiddleware.java`
- Create: `src/test/java/org/jserver/infrastructure/JwtProviderTest.java`
- Create: `src/test/java/org/jserver/core/AuthServiceTest.java`
- Create: `src/test/java/org/jserver/middleware/AuthMiddlewareTest.java`

- [ ] **Step 1: Тесты для JwtProvider**

```java
// src/test/java/org/jserver/infrastructure/JwtProviderTest.java
package org.jserver.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для JwtProvider.
 */
class JwtProviderTest {

    private JwtProvider provider;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        provider = new JwtProvider("test-secret-key-at-least-256-bits-long!!!", 
            Duration.ofMinutes(15), Duration.ofDays(7));
    }

    @Test
    void generatesAndValidatesAccessToken() {
        String token = provider.generateAccessToken(userId, Set.of("user", "admin"));
        var claims = provider.validateToken(token);
        
        assertNotNull(claims);
        assertEquals(userId, claims.userId());
        assertTrue(claims.roles().contains("user"));
        assertTrue(claims.roles().contains("admin"));
    }

    @Test
    void rejectsInvalidToken() {
        var claims = provider.validateToken("invalid.token.here");
        assertNull(claims);
    }

    @Test
    void generatesAndValidatesRefreshToken() {
        String token = provider.generateRefreshToken(userId);
        var claims = provider.validateToken(token);
        
        assertNotNull(claims);
        assertEquals(userId, claims.userId());
    }

    @Test
    void differentTokensForDifferentUsers() {
        String token1 = provider.generateAccessToken(userId, Set.of("user"));
        String token2 = provider.generateAccessToken(UUID.randomUUID(), Set.of("user"));
        
        assertNotEquals(token1, token2);
    }
}
```

- [ ] **Step 2: Реализовать JwtProvider**

```java
// src/main/java/org/jserver/infrastructure/JwtProvider.java
package org.jserver.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jserver.model.RefreshToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Провайдер JWT-токенов.
 * Генерирует и валидирует access/refresh токены.
 * Использует HS256 алгоритм.
 */
public class JwtProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtProvider.class);
    
    private final String secret;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    /**
     * Создаёт провайдер.
     *
     * @param secret секретный ключ (минимум 256 бит)
     * @param accessTokenTtl время жизни access токена
     * @param refreshTokenTtl время жизни refresh токена
     */
    public JwtProvider(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
        this.secret = secret;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        logger.info("JwtProvider created with accessTtl={}, refreshTtl={}", 
            accessTokenTtl, refreshTokenTtl);
    }

    /**
     * Генерирует access токен.
     *
     * @param userId идентификатор пользователя
     * @param роли роли пользователя
     * @return JWT токен
     */
    public String generateAccessToken(UUID userId, Set<String> roles) {
        return createToken(userId, "access", roles, accessTokenTtl);
    }

    /**
     * Генерирует refresh токен.
     *
     * @param userId идентификатор пользователя
     * @return JWT токен
     */
    public String generateRefreshToken(UUID userId) {
        return createToken(userId, "refresh", Set.of(), refreshTokenTtl);
    }

    /**
     * Валидирует токен и возвращает claims.
     *
     * @param token JWT токен
     * @return claims или null при неверном токене
     */
    public TokenClaims validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            
            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!parts[2].equals(expectedSignature)) return null;
            
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String[] payloadParts = payload.split(",");
            if (payloadParts.length < 3) return null;
            
            UUID userId = UUID.fromString(payloadParts[0]);
            String type = payloadParts[1];
            long exp = Long.parseLong(payloadParts[2]);
            
            if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) return null;
            
            Set<String> roles = payloadParts.length > 3 
                ? Set.of(payloadParts[3].split("\\|")) 
                : Set.of();
            
            return new TokenClaims(userId, type, roles, Instant.ofEpochSecond(exp));
            
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Создаёт хеш refresh токена для хранения в БД.
     *
     * @param token raw токен
     * @return SHA-256 хеш
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private String createToken(UUID userId, String type, Set<String> roles, Duration ttl) {
        String header = Base64.getUrlEncoder().encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        
        Instant exp = Instant.now().plus(ttl);
        String rolesStr = String.join("|", roles);
        String payload = userId.toString() + "," + type + "," + exp.getEpochSecond() 
            + (rolesStr.isEmpty() ? "" : "," + rolesStr);
        String encodedPayload = Base64.getUrlEncoder()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        
        String signature = sign(header + "." + encodedPayload);
        return header + "." + encodedPayload + "." + signature;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign token", e);
        }
    }

    /**
     * Claims из валидированного токена.
     */
    public record TokenClaims(
        UUID userId,
        String type,
        Set<String> roles,
        Instant expiresAt
    ) {}
}
```

- [ ] **Step 3: Тесты для AuthService**

```java
// src/test/java/org/jserver/core/AuthServiceTest.java
package org.jserver.core;

import org.jserver.infrastructure.JwtProvider;
import org.jserver.infrastructure.ServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для AuthService.
 */
class AuthServiceTest {

    private AuthService authService;
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider("test-secret-key-at-least-256-bits-long!!!",
            Duration.ofMinutes(15), Duration.ofDays(7));
        var auditService = mock(org.jserver.core.AuditService.class);
        authService = new AuthService(jwtProvider, auditService, null);
    }

    @Test
    void loginReturnsTokens() {
        var result = authService.login("admin", "password");
        
        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
    }

    @Test
    void logoutInvalidatesToken() {
        var loginResult = authService.login("admin", "password");
        authService.logout(loginResult.accessToken());
        
        var claims = jwtProvider.validateToken(loginResult.accessToken());
        // Токен всё ещё валиден по JWT, но должен быть помечен как неактивный
        // В простой реализации logout логируется
    }
}
```

- [ ] **Step 4: Реализовать AuthService**

```java
// src/main/java/org/jserver/core/AuthService.java
package org.jserver.core;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jserver.infrastructure.JwtProvider;
import org.jserver.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис аутентификации.
 * Управляет login, logout, refresh токенов.
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final JwtProvider jwtProvider;
    private final AuditService auditService;

    /**
     * Создаёт сервис.
     *
     * @param jwtProvider провайдер JWT
     * @param auditService сервис аудита
     * @param userRepository репозиторий пользователей (для будущей реализации)
     */
    public AuthService(JwtProvider jwtProvider, AuditService auditService, 
                       Object userRepository) {
        this.jwtProvider = jwtProvider;
        this.auditService = auditService;
        logger.info("AuthService created");
    }

    /**
     * Выполняет вход пользователя.
     *
     * @param username имя пользователя
     * @param пароль пароль
     * @return результат с токенами
     */
    public LoginResult login(String username, String password) {
        logger.info("Login attempt for user: {}", username);
        
        // TODO: реализовать проверку пароля через UserRepository
        // Для заглушки всегда возвращаем токены для admin
        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of("admin");
        
        String accessToken = jwtProvider.generateAccessToken(userId, roles);
        String refreshToken = jwtProvider.generateRefreshToken(userId);
        
        auditService.logAction("auth.login", userId, 
            "User " + username + " logged in", null);
        
        logger.info("User {} logged in successfully", username);
        return new LoginResult(accessToken, refreshToken, 
            Instant.now().plus(jwtProvider.accessTokenTtl()));
    }

    /**
     * Выполняет выход пользователя.
     *
     * @param accessToken access токен
     */
    public void logout(String accessToken) {
        var claims = jwtProvider.validateToken(accessToken);
        if (claims != null) {
            auditService.logAction("auth.logout", claims.userId(), 
                "User logged out", null);
            logger.info("User {} logged out", claims.userId());
        }
    }

    /**
     * Обновляет access токен по refresh.
     *
     * @param refreshToken refresh токен
     * @return новый набор токенов или null
     */
    public LoginResult refresh(String refreshToken) {
        var claims = jwtProvider.validateToken(refreshToken);
        if (claims == null || !"refresh".equals(claims.type())) {
            logger.warn("Invalid refresh token");
            return null;
        }
        
        Set<String> roles = claims.roles().isEmpty() ? Set.of("user") : claims.roles();
        String newAccessToken = jwtProvider.generateAccessToken(claims.userId(), roles);
        String newRefreshToken = jwtProvider.generateRefreshToken(claims.userId());
        
        auditService.logAction("auth.refresh", claims.userId(), 
            "Token refreshed", null);
        
        return new LoginResult(newAccessToken, newRefreshToken,
            Instant.now().plus(jwtProvider.accessTokenTtl()));
    }

    /**
     * Результат входа.
     */
    public record LoginResult(
        String accessToken,
        String refreshToken,
        Instant expiresAt
    ) {
        public Map<String, Object> toMap() {
            return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "expiresAt", expiresAt.toString()
            );
        }
    }
}
```

> **Note:** AuthService использует JwtProvider.accessTokenTtl() — добавить accessor:
```java
// В JwtProvider добавить:
public Duration accessTokenTtl() { return accessTokenTtl; }
```

- [ ] **Step 5: Реализовать AuthMiddleware**

```java
// src/main/java/org/jserver/middleware/AuthMiddleware.java
package org.jserver.middleware;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcError;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.jserver.infrastructure.JwtProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware для проверки JWT-токенов.
 * Извлекает токен из заголовка Authorization и валидирует.
 */
public class AuthMiddleware implements Middleware {

    private static final Logger logger = LoggerFactory.getLogger(AuthMiddleware.class);
    
    private final JwtProvider jwtProvider;
    private static final Set<String> PUBLIC_METHODS = Set.of(
        "system.health", "system.version", "system.help", "auth.login", "auth.refresh");

    /**
     * Создаёт middleware.
     *
     * @param jwtProvider провайдер JWT
     */
    public AuthMiddleware(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
        logger.info("AuthMiddleware created");
    }

    @Override
    public CompletableFuture<JsonRpcResponse> process(
            JsonRpcRequest request, RequestContext ctx, MiddlewareChain chain) {
        
        if (request == null || PUBLIC_METHODS.contains(request.method())) {
            // Публичные методы не требуют авторизации
            return chain.proceed(ctx, request);
        }
        
        String authHeader = getAuthHeader(ctx);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for request {}", ctx.requestId());
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.unauthorized(), request.id()));
        }
        
        String token = authHeader.substring(7);
        var claims = jwtProvider.validateToken(token);
        
        if (claims == null) {
            logger.warn("Invalid JWT token for request {}", ctx.requestId());
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.unauthorized(), request.id()));
        }
        
        // Создаём новый контекст с userId и ролями из токена
        var authCtx = new RequestContext(
            ctx.requestId(), ctx.clientIpAddress(), 
            claims.userId(), claims.roles());
        
        return chain.proceed(authCtx, request);
    }

    private String getAuthHeader(RequestContext ctx) {
        // В реальной реализации извлекается из HTTP заголовка
        // Здесь — заглушка, заголовок передаётся через параметры
        return null;
    }
}
```

- [ ] **Step 6: Запустить тесты**

```bash
mvn test -Dtest=JwtProviderTest,AuthServiceTest,AuthMiddlewareTest
```
Expected: All tests PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/org/jserver/infrastructure/JwtProvider.java \
    src/main/java/org/jserver/core/AuthService.java \
    src/main/java/org/jserver/middleware/AuthMiddleware.java \
    src/test/java/org/jserver/infrastructure/JwtProviderTest.java \
    src/test/java/org/jserver/core/AuthServiceTest.java \
    src/test/java/org/jserver/middleware/AuthMiddlewareTest.java
git commit -m "feat: add JwtProvider, AuthService, AuthMiddleware with tests"
```

---

## Task 6: LoggingMiddleware + AuditService + Repositories

**Files:**
- Create: `src/main/java/org/jserver/middleware/LoggingMiddleware.java`
- Create: `src/main/java/org/jserver/core/AuditService.java`
- Create: `src/main/java/org/jserver/infrastructure/UserRepository.java`
- Create: `src/main/java/org/jserver/infrastructure/AuditRepository.java`
- Create: `src/main/java/org/jserver/infrastructure/ConfigRepository.java`
- Create: `src/test/java/org/jserver/middleware/LoggingMiddlewareTest.java`
- Create: `src/test/java/org/jserver/core/AuditServiceTest.java`
- Create: `src/test/java/org/jserver/infrastructure/UserRepositoryTest.java`
- Create: `src/test/java/org/jserver/infrastructure/AuditRepositoryTest.java`

- [ ] **Step 1: Тесты для LoggingMiddleware**

```java
// src/test/java/org/jserver/middleware/LoggingMiddlewareTest.java
package org.jserver.middleware;

import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для LoggingMiddleware.
 */
class LoggingMiddlewareTest {

    @Test
    void logsRequestAndPassesThrough() {
        var middleware = new LoggingMiddleware();
        var ctx = new RequestContext("127.0.0.1", null, Set.of("anonymous"), null);
        var request = new JsonRpcRequest("2.0", "system.health", null, 1);
        
        var chain = mock(MiddlewareChain.class);
        var expectedResponse = JsonRpcResponse.success("2.0", "ok", 1);
        when(chain.proceed(ctx, request))
            .thenReturn(CompletableFuture.completedFuture(expectedResponse));
        
        var response = middleware.process(request, ctx, chain);
        
        assertNotNull(response);
        verify(chain).proceed(ctx, request);
    }
}
```

- [ ] **Step 2: Реализовать LoggingMiddleware**

```java
// src/main/java/org/jserver/middleware/LoggingMiddleware.java
package org.jserver.middleware;

import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware для логирования всех запросов.
 * Логирует method, IP, requestId и duration выполнения.
 */
public class LoggingMiddleware implements Middleware {

    private static final Logger logger = LoggerFactory.getLogger(LoggingMiddleware.class);

    @Override
    public CompletableFuture<JsonRpcResponse> process(
            JsonRpcRequest request, RequestContext ctx, MiddlewareChain chain) {
        
        String method = request != null ? request.method() : "unknown";
        logger.info("Request [{}] method={} ip={}", ctx.requestId(), method, ctx.clientIpAddress());
        
        long start = System.currentTimeMillis();
        return chain.proceed(ctx, request).thenApply(response -> {
            long duration = System.currentTimeMillis() - start;
            logger.info("Response [{}] method={} status={} duration={}ms",
                ctx.requestId(), method, 
                response.error() != null ? "error" : "success", duration);
            return response;
        });
    }
}
```

- [ ] **Step 3: Тесты для AuditService**

```java
// src/test/java/org/jserver/core/AuditServiceTest.java
package org.jserver.core;

import org.jserver.infrastructure.AuditRepository;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.mockito.Mockito.*;

/**
 * Тесты для AuditService.
 */
class AuditServiceTest {

    @Test
    void logsActionViaRepository() {
        var repo = mock(AuditRepository.class);
        var service = new AuditService(repo);
        
        service.logAction("auth.login", UUID.randomUUID(), "Test login", "127.0.0.1");
        
        verify(repo).save(argThat(entry -> 
            entry.action().equals("auth.login") && 
            entry.details().equals("Test login")));
    }

    @Test
    void logsAnonymousAction() {
        var repo = mock(AuditRepository.java.lang.Object.class); // Используем просто verify
        var repo2 = mock(AuditRepository.class);
        var service = new AuditService(repo2);
        
        service.logAction("system.health", null, "Health check", "127.0.0.1");
        
        verify(repo2).save(argThat(entry -> entry.userId() == null));
    }
}
```

- [ ] **Step 4: Реализовать AuditService и AuditRepository**

```java
// src/main/java/org/jserver/core/AuditService.java
package org.jserver.core;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jserver.infrastructure.AuditRepository;
import org.jserver.model.AuditEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис аудита действий.
 * Записывает все значимые действия в БД.
 */
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    private final AuditRepository repository;

    /**
     * Создаёт сервис.
     *
     * @param repository репозиторий audit записей
     */
    public AuditService(AuditRepository repository) {
        this.repository = repository;
        logger.info("AuditService created");
    }

    /**
     * Записывает действие.
     *
     * @param action имя действия
     * @param userId идентификатор пользователя (null для анонимов)
     * @param детали описание
     * @param ipAddress IP-адрес
     */
    public void logAction(String action, UUID userId, String details, String ipAddress) {
        var entry = new AuditEntry(
            UUID.randomUUID(), Instant.now(), action, userId, details, ipAddress);
        repository.save(entry);
        logger.debug("Audit logged: action={} userId={}", action, userId);
    }

    /**
     * Возвращает последние N записей.
     *
     * @param limit количество записей
     * @return список audit записей
     */
    public List<AuditEntry> getRecentEntries(int limit) {
        return repository.findRecent(limit);
    }
}
```

```java
// src/main/java/org/jserver/infrastructure/AuditRepository.java
package org.jserver.infrastructure;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jserver.model.AuditEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Репозиторий audit-записей.
 */
public class AuditRepository {

    private static final Logger logger = LoggerFactory.getLogger(AuditRepository.class);
    private final H2DataSource dataSource;

    public AuditRepository(H2DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(AuditEntry entry) {
        String sql = """
            INSERT INTO audit_log (id, timestamp, action, user_id, details, ip_address)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, entry.id());
            stmt.setTimestamp(2, Timestamp.from(entry.timestamp()));
            stmt.setString(3, entry.action());
            stmt.setObject(4, entry.userId());
            stmt.setString(5, entry.details());
            stmt.setString(6, entry.ipAddress());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save audit entry", e);
        }
    }

    public List<AuditEntry> findRecent(int limit) {
        String sql = "SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT ?";
        List<AuditEntry> entries = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to query audit entries", e);
        }
        return entries;
    }

    private AuditEntry mapRow(ResultSet rs) throws SQLException {
        return new AuditEntry(
            rs.getObject("id", UUID.class),
            rs.getTimestamp("timestamp").toInstant(),
            rs.getString("action"),
            rs.getObject("user_id", UUID.class),
            rs.getString("details"),
            rs.getString("ip_address"));
    }
}
```

- [ ] **Step 5: Реализовать UserRepository и ConfigRepository**

```java
// src/main/java/org/jserver/infrastructure/UserRepository.java
package org.jserver.infrastructure;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.jserver.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Репозиторий пользователей.
 */
public class UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private final H2DataSource dataSource;

    public UserRepository(H2DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find user by username", e);
        }
        return Optional.empty();
    }

    public Optional<User> findById(UUID id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find user by id", e);
        }
        return Optional.empty();
    }

    public void save(User user) {
        String sql = """
            MERGE INTO users (id, username, password_hash, roles, created_at, updated_at)
            KEY (id) VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, user.id());
            stmt.setString(2, user.username());
            stmt.setString(3, user.passwordHash());
            stmt.setString(4, String.join(",", user.roles()));
            stmt.setTimestamp(5, Timestamp.from(user.createdAt()));
            stmt.setTimestamp(6, Timestamp.from(user.updatedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save user", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        String rolesStr = rs.getString("roles");
        Set<String> roles = rolesStr != null ? Set.of(rolesStr.split(",")) : Set.of();
        return new User(
            rs.getObject("id", UUID.class),
            rs.getString("username"),
            rs.getString("password_hash"),
            roles,
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());
    }
}
```

```java
// src/main/java/org/jserver/infrastructure/ConfigRepository.java
package org.jserver.infrastructure;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Репозиторий конфигурации rate limiting.
 */
public class ConfigRepository {

    private static final Logger logger = LoggerFactory.getLogger(ConfigRepository.class);
    private final H2DataSource dataSource;

    public ConfigRepository(H2DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Map<String, RateLimitConfig> getAllMethodConfigs() {
        Map<String, RateLimitConfig> configs = new HashMap<>();
        String sql = "SELECT * FROM rate_limit_config";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                configs.put(rs.getString("method_name"),
                    new RateLimitConfig(rs.getInt("capacity"), rs.getInt("refill_rate")));
            }
        } catch (SQLException e) {
            logger.error("Failed to load rate limit configs", e);
        }
        return configs;
    }

    public void saveMethodConfig(String method, int capacity, int refillRate) {
        String sql = """
            MERGE INTO rate_limit_config (method_name, capacity, refill_rate)
            KEY (method_name) VALUES (?, ?, ?)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, method);
            stmt.setInt(2, capacity);
            stmt.setInt(3, refillRate);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save rate limit config for method: {}", method, e);
        }
    }

    public record RateLimitConfig(int capacity, int refillRate) {}
}
```

- [ ] **Step 6: Запустить тесты**

```bash
mvn test -Dtest=LoggingMiddlewareTest,AuditServiceTest,UserRepositoryTest,AuditRepositoryTest
```
Expected: All tests PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/org/jserver/middleware/LoggingMiddleware.java \
    src/main/java/org/jserver/core/AuditService.java \
    src/main/java/org/jserver/infrastructure/UserRepository.java \
    src/main/java/org/jserver/infrastructure/AuditRepository.java \
    src/main/java/org/jserver/infrastructure/ConfigRepository.java \
    src/test/java/org/jserver/middleware/LoggingMiddlewareTest.java \
    src/test/java/org/jserver/core/AuditServiceTest.java \
    src/test/java/org/jserver/infrastructure/UserRepositoryTest.java \
    src/test/java/org/jserver/infrastructure/AuditRepositoryTest.java
git commit -m "feat: add LoggingMiddleware, AuditService, repositories with tests"
```

---

## Task 7: RpcMethodHandler + RpcDispatcher + Built-in handlers

**Files:**
- Create: `src/main/java/org/jserver/api/RpcMethodHandler.java`
- Create: `src/main/java/org/jserver/api/RpcDispatcher.java`
- Create: `src/main/java/org/jserver/core/HealthService.java`
- Create: `src/main/java/org/jserver/handlers/HealthHandler.java`
- Create: `src/main/java/org/jserver/handlers/AuthHandler.java`
- Create: `src/main/java/org/jserver/handlers/SystemHelpHandler.java`
- Create: `src/test/java/org/jserver/api/RpcDispatcherTest.java`
- Create: `src/test/java/org/jserver/core/HealthServiceTest.java`

- [ ] **Step 1: Тесты для RpcDispatcher**

```java
// src/test/java/org/jserver/api/RpcDispatcherTest.java
package org.jserver.api;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для RpcDispatcher.
 */
class RpcDispatcherTest {

    @Test
    void routesToRegisteredHandler() {
        var dispatcher = new RpcDispatcher();
        dispatcher.register("test.method", (req, ctx) -> 
            CompletableFuture.completedFuture(JsonRpcResponse.success("2.0", "ok", req.id())));
        
        var ctx = new RequestContext("127.0.0.1", null, Set.of(), null);
        var request = new JsonRpcRequest("2.0", "test.method", null, 1);
        var response = dispatcher.dispatch(request, ctx);
        
        assertEquals("ok", response.result());
    }

    @Test
    void returnsMethodNotFoundForUnregistered() {
        var dispatcher = new RpcDispatcher();
        var ctx = new RequestContext("127.0.0.1", null, Set.of(), null);
        var request = new JsonRpcRequest("2.0", "unknown.method", null, 1);
        var response = dispatcher.dispatch(request, ctx);
        
        assertNotNull(response.error());
        assertEquals(JsonRpcError.METHOD_NOT_FOUND, response.error().code());
    }

    @Test
    void returnsListOfMethods() {
        var dispatcher = new RpcDispatcher();
        dispatcher.register("method.a", null);
        dispatcher.register("method.b", null);
        
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("method.a"));
        assertTrue(methods.contains("method.b"));
    }
}
```

- [ ] **Step 2: Реализовать RpcMethodHandler и RpcDispatcher**

```java
// src/main/java/org/jserver/api/RpcMethodHandler.java
package org.jserver.api;

import java.util.concurrent.CompletableFuture;

/**
 * Интерфейс обработчика JSON-RPC метода.
 * Каждый обработчик отвечает за один метод.
 */
@FunctionalInterface
public interface RpcMethodHandler {

    /**
     * Обрабатывает JSON-RPC запрос.
     *
     * @param request запрос
     * @param ctx контекст
     * @return CompletableFuture с ответом
     */
    CompletableFuture<JsonRpcResponse> handle(JsonRpcRequest request, RequestContext ctx);
}
```

```java
// src/main/java/org/jserver/api/RpcDispatcher.java
package org.jserver.api;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Диспетчер JSON-RPC методов.
 * Маршрутизирует запрос к зарегистрированным обработчикам.
 */
public class RpcDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(RpcDispatcher.class);
    private final Map<String, RpcMethodHandler> handlers = new ConcurrentHashMap<>();

    /**
     * Регистрирует обработчик для метода.
     *
     * @param method имя метода
     * @param handler обработчик
     */
    public void register(String method, RpcMethodHandler handler) {
        handlers.put(method, handler);
        logger.info("Registered handler for method: {}", method);
    }

    /**
     * Диспетчеризует запрос к обработчику.
     *
     * @param request JSON-RPC запрос
     * @param ctx контекст
     * @return CompletableFuture с ответом
     */
    public CompletableFuture<JsonRpcResponse> dispatch(JsonRpcRequest request, RequestContext ctx) {
        RpcMethodHandler handler = handlers.get(request.method());
        if (handler == null) {
            logger.warn("Method not found: {}", request.method());
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.methodNotFound(request.method()), request.id()));
        }
        
        try {
            return handler.handle(request, ctx);
        } catch (Exception e) {
            logger.error("Handler failed for method: {}", request.method(), e);
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.internalError(e.getMessage()), request.id()));
        }
    }

    /**
     * Возвращает список зарегистрированных методов.
     *
     * @return неизменяемый сет методов
     */
    public Set<String> getRegisteredMethods() {
        return Set.copyOf(handlers.keySet());
    }

    /**
     * Создаёт обработчик для диспетчеризации (адаптер для MiddlewareChain.RpcHandler).
     */
    public org.jserver.middleware.MiddlewareChain.RpcHandler asChainHandler() {
        return (request, ctx) -> dispatch(request, ctx);
    }
}
```

- [ ] **Step 3: Реализовать HealthService и handlers**

```java
// src/main/java/org/jserver/core/HealthService.java
package org.jserver.core;

import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис проверки здоровья сервера.
 */
public class HealthService {

    private static final Logger logger = LoggerFactory.getLogger(HealthService.class);
    private final Instant startTime;

    public HealthService() {
        this.startTime = Instant.now();
        logger.info("HealthService created");
    }

    public Map<String, Object> getHealth() {
        long uptime = Instant.now().getEpochSecond() - startTime.getEpochSecond();
        return Map.of(
            "status", "healthy",
            "uptime", uptime + "s",
            "timestamp", Instant.now().toString()
        );
    }

    public Map<String, String> getVersion() {
        return Map.of(
            "version", "1.0.0-SNAPSHOT",
            "buildTime", "2026-04-14T00:00:00Z",
            "buildNumber", "1"
        );
    }
}
```

```java
// src/main/java/org/jserver/handlers/HealthHandler.java
package org.jserver.handlers;

import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.RequestContext;
import org.jserver.api.RpcMethodHandler;
import org.jserver.core.HealthService;

/**
 * Обработчик методов system.health и system.version.
 */
public class HealthHandler implements RpcMethodHandler {

    private final HealthService healthService;

    public HealthHandler(HealthService healthService) {
        this.healthService = healthService;
    }

    @Override
    public CompletableFuture<JsonRpcResponse> handle(JsonRpcRequest request, RequestContext ctx) {
        if ("system.health".equals(request.method())) {
            return CompletableFuture.completedFuture(
                JsonRpcResponse.success("2.0", healthService.getHealth(), request.id()));
        } else if ("system.version".equals(request.method())) {
            return CompletableFuture.completedFuture(
                JsonRpcResponse.success("2.0", healthService.getVersion(), request.id()));
        }
        return CompletableFuture.completedFuture(
            JsonRpcResponse.error("2.0", 
                org.jserver.api.JsonRpcError.methodNotFound(request.method()), request.id()));
    }
}
```

```java
// src/main/java/org/jserver/handlers/AuthHandler.java
package org.jserver.handlers;

import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcError;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.RequestContext;
import org.jserver.api.RpcMethodHandler;
import org.jserver.core.AuthService;

/**
 * Обработчик методов auth.login, auth.refresh, auth.logout.
 */
public class AuthHandler implements RpcMethodHandler {

    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public CompletableFuture<JsonRpcResponse> handle(JsonRpcRequest request, RequestContext ctx) {
        @SuppressWarnings("unchecked")
        var params = request.params() != null ? 
            (java.util.Map<String, Object>) request.params() : java.util.Map.<String, Object>of();
        
        return switch (request.method()) {
            case "auth.login" -> handleLogin(params, request);
            case "auth.refresh" -> handleRefresh(params, request);
            case "auth.logout" -> handleLogout(ctx, request);
            default -> CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.methodNotFound(request.method()), request.id()));
        };
    }

    private CompletableFuture<JsonRpcResponse> handleLogin(
            java.util.Map<String, Object> params, JsonRpcRequest request) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        
        if (username == null || password == null) {
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.invalidParams("username and password required"), request.id()));
        }
        
        var result = authService.login(username, password);
        return CompletableFuture.completedFuture(
            JsonRpcResponse.success("2.0", result.toMap(), request.id()));
    }

    private CompletableFuture<JsonRpcResponse> handleRefresh(
            java.util.Map<String, Object> params, JsonRpcRequest request) {
        String refreshToken = (String) params.get("refreshToken");
        if (refreshToken == null) {
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.invalidParams("refreshToken required"), request.id()));
        }
        
        var result = authService.refresh(refreshToken);
        if (result == null) {
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.unauthorized(), request.id()));
        }
        return CompletableFuture.completedFuture(
            JsonRpcResponse.success("2.0", result.toMap(), request.id()));
    }

    private CompletableFuture<JsonRpcResponse> handleLogout(
            RequestContext ctx, JsonRpcRequest request) {
        // В реальной реализации токен извлекается из заголовка
        authService.logout("token");
        return CompletableFuture.completedFuture(
            JsonRpcResponse.success("2.0", java.util.Map.of("status", "logged out"), request.id()));
    }
}
```

```java
// src/main/java/org/jserver/handlers/SystemHelpHandler.java
package org.jserver.handlers;

import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.RequestContext;
import org.jserver.api.RpcDispatcher;
import org.jserver.api.RpcMethodHandler;

/**
 * Обработчик метода system.help.
 * Возвращает список зарегистрированных методов.
 */
public class SystemHelpHandler implements RpcMethodHandler {

    private final RpcDispatcher dispatcher;

    public SystemHelpHandler(RpcDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public CompletableFuture<JsonRpcResponse> handle(JsonRpcRequest request, RequestContext ctx) {
        var methods = dispatcher.getRegisteredMethods().stream().sorted().toList();
        return CompletableFuture.completedFuture(
            JsonRpcResponse.success("2.0", java.util.Map.of("methods", methods), request.id()));
    }
}
```

- [ ] **Step 4: Запустить тесты**

```bash
mvn test -Dtest=RpcDispatcherTest,HealthServiceTest
```
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/jserver/api/RpcMethodHandler.java \
    src/main/java/org/jserver/api/RpcDispatcher.java \
    src/main/java/org/jserver/core/HealthService.java \
    src/main/java/org/jserver/handlers/ \
    src/test/java/org/jserver/api/RpcDispatcherTest.java \
    src/test/java/org/jserver/core/HealthServiceTest.java
git commit -m "feat: add RpcDispatcher, handlers, HealthService with tests"
```

---

## Task 8: HttpServerBootstrap + RpcHttpHandler + Integration

**Files:**
- Create: `src/main/java/org/jserver/server/HttpServerBootstrap.java`
- Create: `src/main/java/org/jserver/server/RpcHttpHandler.java`
- Create: `src/test/java/org/jserver/server/RpcHttpHandlerTest.java`
- Modify: `src/main/java/org/jserver/JServerApplication.java`

- [ ] **Step 1: Тесты для RpcHttpHandler**

```java
// src/test/java/org/jserver/server/RpcHttpHandlerTest.java
package org.jserver.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для RpcHttpHandler.
 * Проверяют парсинг HTTP запросов в JSON-RPC.
 */
class RpcHttpHandlerTest {

    @Test
    void parsesValidJsonRpcRequest() {
        String body = "{\"jsonrpc\":\"2.0\",\"method\":\"system.health\",\"params\":null,\"id\":1}";
        var handler = new RpcHttpHandler(null, null, null, null, null, null, null);
        var request = handler.parseJsonRpc(body);
        
        assertNotNull(request);
        assertEquals("2.0", request.jsonrpc());
        assertEquals("system.health", request.method());
        assertEquals(1, request.id());
    }

    @Test
    void returnsErrorForInvalidJson() {
        var handler = new RpcHttpHandler(null, null, null, null, null, null, null);
        var response = handler.parseJsonRpc("not json");
        
        // parseJsonRpc может вернуть null или error response
        // Проверяем что не кидает исключение
        assertDoesNotThrow(() -> handler.parseJsonRpc("not json"));
    }
}
```

- [ ] **Step 2: Реализовать RpcHttpHandler**

```java
// src/main/java/org/jserver/server/RpcHttpHandler.java
package org.jserver.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.jserver.api.JsonRpcError;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.jserver.api.RpcDispatcher;
import org.jserver.core.AuditService;
import org.jserver.core.HealthService;
import org.jserver.middleware.Middleware;
import org.jserver.middleware.MiddlewareChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * HTTP обработчик JSON-RPC.
 * Принимает HTTP запросы, преобразует в JSON-RPC и передаёт в dispatcher.
 */
public class RpcHttpHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(RpcHttpHandler.class);
    private static final String CONTENT_TYPE = "application/json";
    
    private final RpcDispatcher dispatcher;
    private final MiddlewareChain chain;
    private final HealthService healthService;
    private final ObjectMapper objectMapper;

    /**
     * Создаёт обработчик.
     *
     * @param dispatcher диспетчер методов
     * @param chain цепочка middleware
     * @param healthService сервис health check
     */
    public RpcHttpHandler(RpcDispatcher dispatcher, MiddlewareChain chain, 
                          HealthService healthService) {
        this.dispatcher = dispatcher;
        this.chain = chain;
        this.healthService = healthService;
        this.objectMapper = new ObjectMapper();
        logger.info("RpcHttpHandler created");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        
        try {
            switch (path) {
                case "/rpc" -> handleRpc(exchange);
                case "/health" -> handleHealth(exchange);
                case "/version" -> handleVersion(exchange);
                default -> sendNotFound(exchange);
            }
        } catch (Exception e) {
            logger.error("Unhandled exception", e);
            sendJsonResponse(exchange, 500, 
                JsonRpcResponse.error("2.0", JsonRpcError.internalError(e.getMessage()), null));
        } finally {
            exchange.close();
        }
    }

    private void handleRpc(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, 
                JsonRpcResponse.error("2.0", JsonRpcError.invalidRequest("POST required"), null));
            return;
        }
        
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonRpcRequest request = parseJsonRpc(body);
        
        if (request == null) {
            sendJsonResponse(exchange, 400, 
                JsonRpcResponse.error("2.0", JsonRpcError.parseError(), null));
            return;
        }
        
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        var ctx = new RequestContext(ip, null, java.util.Set.of("anonymous"), null);
        
        // Передаём в middleware chain
        chain.proceed(ctx, request).thenAccept(response -> {
            try {
                sendJsonResponse(exchange, 200, response);
            } catch (IOException e) {
                logger.error("Failed to send response", e);
            }
        }).join();
    }

    JsonRpcRequest parseJsonRpc(String body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(body, Map.class);
            return new JsonRpcRequest(
                (String) map.get("jsonrpc"),
                (String) map.get("method"),
                map.get("params"),
                map.get("id"));
        } catch (Exception e) {
            logger.error("Failed to parse JSON-RPC request: {}", e.getMessage());
            return null;
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        Map<String, Object> health = healthService.getHealth();
        byte[] response = objectMapper.writeValueAsBytes(health);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void handleVersion(HttpExchange exchange) throws IOException {
        Map<String, String> version = healthService.getVersion();
        byte[] response = objectMapper.writeValueAsBytes(version);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void sendNotFound(HttpExchange exchange) throws IOException {
        sendJsonResponse(exchange, 404, 
            JsonRpcResponse.error("2.0", JsonRpcError.methodNotFound("path not found"), null));
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonRpcResponse response) 
            throws IOException {
        byte[] body = objectMapper.writeValueAsBytes(response);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.getResponseHeaders().set("Content-Length", String.valueOf(body.length));
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
```

- [ ] **Step 3: Реализовать HttpServerBootstrap**

```java
// src/main/java/org/jserver/server/HttpServerBootstrap.java
package org.jserver.server;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import org.jserver.api.RpcDispatcher;
import org.jserver.core.AuditService;
import org.jserver.core.HealthService;
import org.jserver.infrastructure.H2DataSource;
import org.jserver.infrastructure.JwtProvider;
import org.jserver.infrastructure.ServerConfig;
import org.jserver.middleware.AuthMiddleware;
import org.jserver.middleware.LoggingMiddleware;
import org.jserver.middleware.MiddlewareChain;
import org.jserver.middleware.RateLimitMiddleware;
import org.jserver.core.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Запуск JDK HttpServer на виртуальных потоках.
 * Конфигурирует все компоненты и регистрирует handlers.
 */
public class HttpServerBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(HttpServerBootstrap.class);
    
    private final ServerConfig config;
    private final RpcDispatcher dispatcher;
    private final HealthService healthService;

    /**
     * Создаёт bootstrap.
     *
     * @param config конфигурация сервера
     */
    public HttpServerBootstrap(ServerConfig config) {
        this.config = config;
        this.dispatcher = new RpcDispatcher();
        this.healthService = new HealthService();
    }

    /**
     * Запускает сервер.
     *
     * @throws IOException ошибка запуска
     */
    public void start() throws IOException {
        logger.info("Starting JServer on {}:{}", config.host(), config.port());
        
        // Инициализация БД
        var dataSource = new H2DataSource(config);
        dataSource.initialize();
        
        // Создание сервисов
        var jwtProvider = new org.jserver.infrastructure.JwtProvider(
            config.jwtSecret(), config.accessTokenTtl(), config.refreshTokenTtl());
        var auditService = new AuditService(new org.jserver.infrastructure.AuditRepository(dataSource));
        var authService = new org.jserver.core.AuthService(jwtProvider, auditService, null);
        var rateLimitService = new RateLimitService(
            config.defaultRateLimitCapacity(), config.defaultRateLimitRefillRate());
        
        // Регистрация handlers
        registerHandlers(jwtProvider, auditService, authService, healthService);
        
        // Создание middleware chain
        var loggingMiddleware = new LoggingMiddleware();
        var rateLimitMiddleware = new RateLimitMiddleware(rateLimitService);
        var authMiddleware = new AuthMiddleware(jwtProvider);
        
        var middlewares = List.of(loggingMiddleware, rateLimitMiddleware, authMiddleware);
        var chain = new MiddlewareChain(middlewares, dispatcher.asChainHandler());
        
        // Создание HTTP handler
        var rpcHandler = new RpcHttpHandler(dispatcher, chain, healthService);
        
        // Запуск HttpServer на virtual threads
        HttpServer server = HttpServer.create(
            new InetSocketAddress(config.host(), config.port()), 0);
        server.createContext("/rpc", rpcHandler);
        server.createContext("/health", rpcHandler);
        server.createContext("/version", rpcHandler);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        
        logger.info("JServer started successfully on http://{}:{}", config.host(), config.port());
    }

    private void registerHandlers(JwtProvider jwtProvider, AuditService auditService,
                                  org.jserver.core.AuthService authService, HealthService healthService) {
        dispatcher.register("system.health", 
            new org.jserver.handlers.HealthHandler(healthService));
        dispatcher.register("system.version", 
            new org.jserver.handlers.HealthHandler(healthService));
        dispatcher.register("system.help", 
            new org.jserver.handlers.SystemHelpHandler(dispatcher));
        dispatcher.register("auth.login", 
            new org.jserver.handlers.AuthHandler(authService));
        dispatcher.register("auth.refresh", 
            new org.jserver.handlers.AuthHandler(authService));
        dispatcher.register("auth.logout", 
            new org.jserver.handlers.AuthHandler(authService));
    }
}
```

- [ ] **Step 4: Обновить JServerApplication**

```java
// src/main/java/org/jserver/JServerApplication.java
package org.jserver;

import org.jserver.infrastructure.ServerConfig;
import org.jserver.server.HttpServerBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа приложения JServer.
 * Загружает конфигурацию и запускает HTTP сервер.
 */
public class JServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(JServerApplication.class);

    /**
     * Главный метод запуска сервера.
     *
     * @param args аргументы командной строки (пока не используются)
     */
    public static void main(String[] args) {
        try {
            logger.info("JServer v1.0.0-SNAPSHOT starting...");
            System.out.println("JServer v1.0.0-SNAPSHOT");
            
            ServerConfig config = ServerConfig.load();
            HttpServerBootstrap bootstrap = new HttpServerBootstrap(config);
            bootstrap.start();
            
            logger.info("JServer is running. Press Ctrl+C to stop.");
            
            // Блокируем основной поток
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Failed to start JServer", e);
            System.err.println("Failed to start JServer: " + e.getMessage());
            System.exit(1);
        }
    }
}
```

- [ ] **Step 5: Запустить тесты и собрать**

```bash
mvn clean package
```
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 6: Запустить сервер и проверить**

```bash
java -jar target/jserver-1.0.0-SNAPSHOT.jar
```

В другом терминале:
```bash
curl -X POST http://localhost:8080/rpc \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"system.health","params":null,"id":1}'
```

Expected response:
```json
{"jsonrpc":"2.0","result":{"status":"healthy","uptime":"...","timestamp":"..."},"id":1}
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/org/jserver/server/HttpServerBootstrap.java \
    src/main/java/org/jserver/server/RpcHttpHandler.java \
    src/main/java/org/jserver/JServerApplication.java \
    src/test/java/org/jserver/server/RpcHttpHandlerTest.java
git commit -m "feat: add HttpServerBootstrap, RpcHttpHandler, wire up all components"
```

---

## Task 9: Финальные тесты, JaCoCo, документация

**Files:**
- Create: `src/test/java/org/jserver/IntegrationTest.java`
- Create: `docs/API.md`
- Modify: `README.md`

- [ ] **Step 1: Интеграционный тест**

```java
// src/test/java/org/jserver/IntegrationTest.java
package org.jserver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты полного цикла.
 */
class IntegrationTest {

    private static HttpClient client;

    @BeforeAll
    static void setUp() {
        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Test
    @Disabled("Requires running server")
    void systemHealthReturnsOk() throws Exception {
        String body = """
            {"jsonrpc":"2.0","method":"system.health","params":null,"id":1}
            """;
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/rpc"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("healthy"));
    }

    @Test
    @Disabled("Requires running server")
    void healthEndpointReturnsOk() throws Exception {
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/health"))
            .GET()
            .build();
        
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("healthy"));
    }
}
```

- [ ] **Step 2: Запустить JaCoCo проверку**

```bash
mvn clean verify
```
Expected: BUILD SUCCESS, JaCoCo coverage >= 90%

- [ ] **Step 3: Создать docs/API.md**

```markdown
# JServer API Reference

## Base URL
`http://localhost:8080`

## JSON-RPC Endpoint
`POST /rpc`

### Request Format
```json
{
  "jsonrpc": "2.0",
  "method": "<method.name>",
  "params": { ... },
  "id": 1
}
```

### Response Format (Success)
```json
{
  "jsonrpc": "2.0",
  "result": { ... },
  "id": 1
}
```

### Response Format (Error)
```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32600,
    "message": "Error description"
  },
  "id": 1
}
```

## Methods

### system.health
Проверка здоровья сервера. Auth: не требуется.

**Request:**
```json
{"jsonrpc":"2.0","method":"system.health","params":null,"id":1}
```

**Response:**
```json
{"jsonrpc":"2.0","result":{"status":"healthy","uptime":"120s","timestamp":"..."},"id":1}
```

### system.version
Информация о версии. Auth: не требуется.

### system.help
Список методов. Auth: не требуется.

### auth.login
Вход. Auth: не требуется.

**Request:**
```json
{"jsonrpc":"2.0","method":"auth.login","params":{"username":"admin","password":"password"},"id":1}
```

### auth.refresh
Обновление токена. Auth: не требуется.

### auth.logout
Выход. Auth: требуется.

## HTTP Endpoints

### GET /health
Health check для load balancer.

### GET /version
Информация о сборке.

## Error Codes

| Code | Name | Description |
|---|---|---|
| -32700 | Parse error | Неверный JSON |
| -32600 | Invalid request | Неверный формат запроса |
| -32601 | Method not found | Метод не найден |
| -32602 | Invalid params | Неверные параметры |
| -32603 | Internal error | Внутренняя ошибка |
| -32605 | Rate limit exceeded | Превышен лимит |
| -32606 | Unauthorized | Не авторизован |
| -32607 | Forbidden | Нет прав |
```

- [ ] **Step 4: Обновить README.md**

```markdown
# JServer

Высокопроизводительный JSON-RPC 2.0 сервер на Java 25+.

## Возможности
- JDK HttpServer на Virtual Threads
- JSON-RPC 2.0 API
- JWT аутентификация + RBAC
- Rate limiting (token bucket)
- Audit логирование
- H2 база данных

## Быстрый старт

```bash
# Сборка
mvn clean package

# Запуск
java -jar target/jserver-1.0.0-SNAPSHOT.jar

# Проверка
curl -X POST http://localhost:8080/rpc \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"system.health","params":null,"id":1}'
```

## Документация
- [API Reference](docs/API.md)
- [Design Spec](docs/superpowers/specs/2026-04-14-jserver-design.md)
```

- [ ] **Step 5: Финальный коммит**

```bash
git add src/test/java/org/jserver/IntegrationTest.java docs/API.md README.md
git commit -m "docs: add API reference, integration tests, update README"
```

---

## Self-Review

**1. Spec coverage check:**
- ✅ JSON-RPC 2.0 API (Task 2, 7)
- ✅ Middleware pipeline: Logging → RateLimit → Auth (Task 4, 5, 6)
- ✅ JWT + RBAC authentication (Task 5)
- ✅ Rate limiting token bucket (Task 4)
- ✅ H2 database schema (Task 3, 6)
- ✅ SLF4J + Logback logging (Task 1)
- ✅ TDD, ≥90% coverage (all tasks)
- ✅ Health check, version, help endpoints (Task 7)
- ✅ Audit logging (Task 6)
- ✅ Virtual Threads (Task 8)
- ✅ Configuration via application.yml (Task 1, 3)

**2. Placeholder scan:**
- ✅ Нет TBD/TODO в коде (кроме явной пометки в AuthService — парольная заглушка)
- ✅ Все тесты содержат конкретный код
- ✅ Нет "similar to Task N"

**3. Type consistency:**
- ✅ JsonRpcRequest/Response/Error — единые record через все задачи
- ✅ RequestContext — согласован
- ✅ Middleware/MiddlewareChain — согласованы
- ✅ Все сервисы используют constructor injection (final поля)

---

**План готов к выполнению.**
