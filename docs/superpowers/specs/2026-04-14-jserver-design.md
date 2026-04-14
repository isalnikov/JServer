# JServer — Software Design Document (SDD)

**Дата:** 2026-04-14  
**Версия:** 1.0  
**Статус:** Draft

---

## 1. Описание

JServer — универсальный высокопроизводительный Java-сервер, способный обрабатывать до 200 000 rps. Предоставляет JSON-RPC 2.0 API с встроенной аутентификацией (JWT + RBAC), rate limiting и аудит-логированием.

Сервер является базовым шаблоном: разработчик добавляет свою бизнес-логику, следуя约定的 слоям (controller → service → repository).

### 1.1 Цели
- Обработка 200 000 rps на одном сервере
- Минимум зависимостей (JDK HttpServer, H2, SLF4J)
- Чистый, понятный код без магии (без Lombok, без аннотаций)
- Покрытие тестами ≥ 90%
- Модульная расширяемость через чёткие слои

### 1.2 Не-цели
- Не является готовым бизнес-приложением
- Не поддерживает WebSocket, gRPC, GraphQL (только JSON-RPC 2.0)
- Не поддерживает кластеризацию (single-node)

---

## 2. Технические требования

| Параметр | Значение |
|---|---|
| Язык | Java 25+ |
| Сборка | Maven |
| HTTP сервер | JDK HttpServer + Virtual Threads |
| Протокол | JSON-RPC 2.0 |
| БД | H2 (in-memory) |
| Логирование | SLF4J + Logback |
| Auth | JWT + RBAC |
| Тесты | JUnit 5, Mockito, JaCoCo |

### 2.1 Запреты
- **Нет Lombok** — чистый код без магии
- **Нет System.out.println()** — только через Logger
- **Нет лишних библиотек** — если можно написать простой код

---

## 3. Архитектура

### 3.1 Общая схема

```
┌─────────────────────────────────────────────────────┐
│                    Клиенты (HTTP)                     │
└──────────────────────┬──────────────────────────────┘
                       │ JSON-RPC 2.0 POST /rpc
┌──────────────────────▼──────────────────────────────┐
│              JDK HttpServer (Virtual Threads)         │
│  ┌─────────────────────────────────────────────────┐ │
│  │          JsonRpcDispatcher                        │ │
│  │  (маршрутизация к обработчикам по методу)         │ │
│  └────────────────────┬────────────────────────────┘ │
└───────────────────────┼───────────────────────────────┘
                        │
┌───────────────────────▼───────────────────────────────┐
│              Middleware Pipeline                         │
│  ┌─────────┐ ┌──────────┐ ┌────────────┐ ┌─────────┐ │
│  │ Logging │→│ RateLim  │→│ Auth/JWT   │→│ Router  │ │
│  └─────────┘ └──────────┘ └────────────┘ └─────────┘ │
└───────────────────────┬───────────────────────────────┘
                        │
┌───────────────────────▼───────────────────────────────┐
│                   Service Layer                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│  │AuthService│ │HealthSvc│ │RateLimSvc│ │AuditSvc  │ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ │
└───────┼────────────┼────────────┼────────────┼────────┘
        │            │            │            │
┌───────▼────────────▼────────────▼────────────▼────────┐
│               Infrastructure Layer                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│  │  UserRepo│ │AuditRepo │ │ConfigRepo│ │   H2 DB  │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ │
└───────────────────────────────────────────────────────┘
```

### 3.2 Поток запроса

1. Клиент отправляет JSON-RPC POST на `/rpc`
2. `HttpServer` (Virtual Thread) принимает соединение
3. `LoggingMiddleware` логирует запрос (method, IP, request ID)
4. `RateLimitMiddleware` проверяет лимиты (token bucket per IP)
5. `AuthMiddleware` (если метод требует auth) валидирует JWT токен
6. `JsonRpcDispatcher` находит обработчик по `method`
7. `RpcMethodHandler` выполняет бизнес-логику
8. `Repository` работает с данными
9. Формируется JSON-RPC ответ

---

## 4. Структура проекта

```
jserver/
├── pom.xml
├── src/
│   ├── main/java/org/jserver/
│   │   ├── JServerApplication.java          # Точка входа
│   │   ├── api/                              # JSON-RPC модели
│   │   │   ├── JsonRpcRequest.java           # record: jsonrpc, method, params, id
│   │   │   ├── JsonRpcResponse.java          # record: jsonrpc, result, error, id
│   │   │   ├── JsonRpcError.java             # record: code, message, data
│   │   │   ├── RpcDispatcher.java            # Маршрутизация к handlers
│   │   │   └── RpcMethodHandler.java         # Интерфейс обработчиков
│   │   ├── middleware/                        # Цепочка обработки
│   │   │   ├── Middleware.java               # Интерфейс middleware
│   │   │   ├── MiddlewareChain.java          # Цепочка выполнения
│   │   │   ├── LoggingMiddleware.java        # Логирование запросов
│   │   │   ├── RateLimitMiddleware.java      # Rate limiting
│   │   │   └── AuthMiddleware.java           # JWT + RBAC проверка
│   │   ├── core/                              # Бизнес-логика
│   │   │   ├── AuthService.java              # Login, register, refresh
│   │   │   ├── HealthService.java            # Health check
│   │   │   ├── RateLimitService.java         # Управление лимитами
│   │   │   └── AuditService.java             # Audit лог
│   │   ├── infrastructure/                    # Данные и утилиты
│   │   │   ├── H2DataSource.java             # Подключение к БД
│   │   │   ├── UserRepository.java           # CRUD пользователей
│   │   │   ├── AuditRepository.java          # Audit записи
│   │   │   ├── ConfigRepository.java         # Конфигурация
│   │   │   ├── JwtProvider.java              # JWT генерация/валидация
│   │   │   └── ServerConfig.java             # Чтение application.yml
│   │   ├── server/                            # HTTP слой
│   │   │   ├── HttpServerBootstrap.java      # Запуск сервера
│   │   │   └── RpcHttpHandler.java           # HTTP → JSON-RPC
│   │   └── model/                             # Доменные модели
│   │       ├── User.java                     # record: id, username, passwordHash, roles
│   │       └── AuditEntry.java               # record: id, timestamp, action, userId, details
│   ├── main/resources/
│   │   ├── application.yml                   # Конфигурация
│   │   └── logback.xml                       # Логирование
│   └── test/java/org/jserver/                # Тесты (зеркальная структура)
└── docs/
```

---

## 5. JSON-RPC 2.0 Спецификация

### 5.1 Формат запроса

```json
{
  "jsonrpc": "2.0",
  "method": "auth.login",
  "params": { "username": "admin", "password": "secret" },
  "id": 1
}
```

### 5.2 Формат ответа (успех)

```json
{
  "jsonrpc": "2.0",
  "result": { "token": "eyJ...", "expiresAt": "2026-04-14T12:00:00Z" },
  "id": 1
}
```

### 5.3 Формат ответа (ошибка)

```json
{
  "jsonrpc": "2.0",
  "error": { "code": -32600, "message": "Invalid Request" },
  "id": null
}
```

### 5.4 Встроенные методы

| Метод | Auth | Описание |
|---|---|---|
| `system.health` | Нет | Проверка здоровья сервера |
| `system.version` | Нет | Информация о сборке |
| `system.help` | Нет | Список доступных методов |
| `auth.login` | Нет | Получить JWT токен |
| `auth.refresh` | Нет | Обновить JWT токен |
| `auth.logout` | Да | Выйти из системы |
| `ratelimit.status` | Admin | Статус rate limits |
| `audit.list` | Admin | Список audit записей |

### 5.5 Коды ошибок

| Код | Название | Описание |
|---|---|---|
| -32700 | Parse error | Неверный JSON |
| -32600 | Invalid request | Нет поля method или неверный формат |
| -32601 | Method not found | Метод не зарегистрирован |
| -32602 | Invalid params | Неверный формат params |
| -32603 | Internal error | Внутренняя ошибка сервера |
| -32605 | Rate limit exceeded | Превышен лимит запросов |
| -32606 | Unauthorized | Нет или неверный JWT токен |
| -32607 | Forbidden | Нет прав для метода |

---

## 6. Rate Limiting

### 6.1 Алгоритм: Token Bucket

- **Ёмкость:** 100 токенов (по умолчанию)
- **Скорость:** 100 токенов/минуту
- **Granularity:** Per IP address
- **Настраиваемость:** Per-method через `application.yml`

### 6.2 Конфигурация

```yaml
ratelimit:
  enabled: true
  default:
    capacity: 100
    refill-rate: 100  # tokens per minute
  methods:
    auth.login:
      capacity: 20
      refill-rate: 20
```

---

## 7. Аутентификация и авторизация

### 7.1 JWT

- **Алгоритм:** HS256
- **Секрет:** задаётся в `application.yml` (`jwt.secret`)
- **TTL access token:** 15 минут
- **TTL refresh token:** 7 дней

### 7.2 RBAC Роли

| Роль | Доступ |
|---|---|
| `anonymous` | `auth.login`, `system.*` |
| `user` | Все методы кроме `admin.*` |
| `admin` | Полный доступ |

### 7.3 Flow авторизации

1. Клиент вызывает `auth.login(username, password)`
2. `AuthService` проверяет учётные данные
3. Возвращает `{ accessToken, refreshToken, expiresAt }`
4. Последующие запросы: заголовок `Authorization: Bearer <accessToken>`
5. `AuthMiddleware` валидирует токен и проверяет роль

---

## 8. База данных (H2)

### 8.1 Схема

```sql
-- Пользователи
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    roles VARCHAR(255) NOT NULL,  -- comma-separated: "user,admin"
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Audit log
CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    action VARCHAR(255) NOT NULL,
    user_id UUID,
    details TEXT,
    ip_address VARCHAR(45)
);

-- Rate limit config
CREATE TABLE rate_limit_config (
    method_name VARCHAR(255) PRIMARY KEY,
    capacity INT NOT NULL,
    refill_rate INT NOT NULL
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 8.2 Подключение

- **URL:** `jdbc:h2:mem:jserver;DB_CLOSE_DELAY=-1`
- **Driver:** `org.h2.Driver`
- **Mode:** in-memory (для тестов можно file-based)

> **Примечание:** H2 in-memory используется для разработки и тестов. Для production планируется замена на PostgreSQL с миграциями (Liquibase). Это выходит за рамки фазы 1.

---

## 9. Логирование

### 9.1 Конфигурация Logback

- **Консоль:** INFO уровень, цветной вывод
- **Файл:** DEBUG уровень, ротация по 10MB, хранение 7 дней
- **Формат:** `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n`

### 9.2 Что логируется

- Все входящие запросы (method, IP, request ID, duration)
- Все ошибки (с stack trace для Internal error)
- Аутентификация (успех/провал)
- Rate limit срабатывания
- Audit действия (через `AuditService`)

### 9.3 Request ID

Каждый запрос получает уникальный `X-Request-ID` (UUID) для трассировки.

---

## 10. Тестирование

### 10.1 Стратегия: TDD

1. Пишем тест → красный
2. Пишем минимальный код → зелёный
3. Рефакторинг → зелёный
4. JaCoCo проверяет ≥ 90%

### 10.2 Типы тестов

| Слой | Тип | Инструменты |
|---|---|---|
| `api` | Unit | JUnit 5 |
| `middleware` | Unit + интеграция | JUnit 5, Mockito |
| `core` | Unit | JUnit 5, Mockito |
| `infrastructure` | Интеграция | H2 in-memory |
| `server` | End-to-end | HttpClient |
| Нагрузочные | Benchmark | JMH, wrk |

### 10.3覆盖率 (JaCoCo)

- **Line coverage:** ≥ 90%
- **Branch coverage:** ≥ 80%
- **Fail build если ниже:** да

---

## 11. Конфигурация (application.yml)

```yaml
server:
  port: 8080
  host: 0.0.0.0

database:
  url: jdbc:h2:mem:jserver;DB_CLOSE_DELAY=-1
  user: sa
  password:

jwt:
  secret: ${JWT_SECRET:change-me-in-production}
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

---

## 12. C4 Model

### 12.1 Level 1: System Context

```
[Клиент] → (JServer) → [H2 Database]
```

### 12.2 Level 2: Containers

```
[Клиент] → HTTP/JSON-RPC → [JServer Application]
                                       ↓
                                [H2 In-Memory DB]
```

### 12.3 Level 3: Components

```
HTTP Request → HttpServerBootstrap → RpcHttpHandler
                                        ↓
                              Middleware Pipeline
                              (Logging → RateLimit → Auth)
                                        ↓
                              JsonRpcDispatcher
                                        ↓
                              RpcMethodHandler(s)
                                        ↓
                              Service Layer
                                        ↓
                              Repository Layer → H2 DB
```

---

## 13. Принципы кода

- **SOLID** — каждый класс имеет одну ответственность
- **DRY** — нет дублирования кода
- **KISS** — код простой и понятный
- **YAGNI** — не добавляем лишнюю функциональность
- **TDD** — тесты пишутся до кода
- **Record для DTO**, **class для JPA-сущностей**
- **Constructor injection** (final поля)
- **Логирование:** `private static final Logger logger = LoggerFactory.getLogger(ClassName.class)`
- **Java 25+ фичи:** `var`, `switch expressions`, `text blocks`, `Pattern Matching`, `Sequenced Collections`, `Virtual Threads`
- **Javadoc 100%** — все публичные (public) классы и методы задокументированы на русском языке. Package-private и private методы — по необходимости.

---

## 14. Endpoints

| Endpoint | Метод | Описание |
|---|---|---|
| `POST /rpc` | JSON-RPC | Основной endpoint для всех вызовов |
| `GET /health` | HTTP GET | HTTP health check (для load balancer) |
| `GET /version` | HTTP GET | Информация о сборке |

---

## 15. Запуск

```bash
# Сборка
mvn clean package

# Запуск
java -jar target/jserver-1.0.0.jar

# Запуск с параметрами
java -Djwt.secret=my-secret -Dserver.port=9090 -jar target/jserver-1.0.0.jar

# Запуск тестов
mvn test

# Проверка покрытия
mvn jacoco:report
```
