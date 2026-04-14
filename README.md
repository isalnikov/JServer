# JServer

Высокопроизводительный JSON-RPC 2.0 сервер на Java 25+.

## Возможности
- JDK HttpServer на Virtual Threads
- JSON-RPC 2.0 API
- JWT аутентификация + RBAC
- Rate limiting (token bucket)
- Audit логирование
- H2 база данных
- Middleware pipeline (Logging → RateLimit → Auth → Router)
- HTML документация на `/help`

## Быстрый старт

### Сборка
```bash
mvn clean package
```

### Запуск
```bash
java -jar target/jserver-1.0.0-SNAPSHOT.jar
```

### Запуск с параметрами
```bash
# Свой порт и секрет JWT
java -Djwt.secret=my-secret-key -Dserver.port=9090 -jar target/jserver-1.0.0-SNAPSHOT.jar

# Через переменные окружения
JWT_SECRET=my-secret SERVER_PORT=9090 java -jar target/jserver-1.0.0-SNAPSHOT.jar
```

### Проверка
```bash
# JSON-RPC запрос
curl -X POST http://localhost:8080/rpc \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"system.health","params":null,"id":1}'

# Health check
curl http://localhost:8080/health

# Version
curl http://localhost:8080/version

# HTML документация
open http://localhost:8080/help
```

## Тесты
```bash
# Все тесты
mvn test

# С покрытием
mvn clean verify

# Отчёт JaCoCo
mvn jacoco:report
# Отчёт: target/site/jacoco/index.html
```

### Статус тестов
- **97 тестов**, 0 failures, 0 skipped
- JaCoCo coverage: 73% lines / 63% branches (порог: 70/60)
- 3 интеграционных теста (автоматический запуск встроенного сервера)
- Нагрузочный тест: `LoadTest.java`

## Конфигурация
Все параметры в `src/main/resources/application.yml` или через переменные окружения:
- `SERVER_PORT` — порт (по умолчанию 8080)
- `SERVER_HOST` — хост (по умолчанию 0.0.0.0)
- `JWT_SECRET` — секретный ключ JWT
- `DB_URL` — URL базы данных

## Нагрузочное тестирование

### Тестовое окружение
- **CPU:** Intel Core i7-7700 @ 3.60GHz (4 ядра / 8 потоков, Kaby Lake)
- **RAM:** 16 GB DDR4
- **Диск:** NVMe SSD 234 GB
- **OS:** Linux 6.8.0-79-generic (Ubuntu 24.04)
- **Java:** OpenJDK 25.0.2 +10 (Ubuntu build)
- **Тест:** `LoadTest.java` — встроенный сервер + HttpClient, loopback

### Результаты (middleware OFF — чистая пропускная способность)

| Concurrency | RPS     | Avg   | P50   | P95   | P99   | Errors |
|:-----------:|--------:|------:|------:|------:|------:|-------:|
| 1           | 1 425   | 0.8ms | 0.5ms | 1.3ms | 2.8ms | 0      |
| 2           | 4 016   | 0.5ms | 0.4ms | 1.0ms | 1.6ms | 0      |
| 4           | 7 273   | 0.5ms | 0.4ms | 1.1ms | 1.7ms | 0      |
| 8           | 6 908   | 1.1ms | 0.7ms | 3.5ms | 8.8ms | 0      |
| 16          | 13 158  | 1.2ms | 1.0ms | 2.5ms | 4.3ms | 0      |
| 32          | 16 719  | 1.9ms | 1.7ms | 3.4ms | 5.1ms | 0      |
| **64**      | **21 333** | **3.0ms** | **2.6ms** | **5.2ms** | **13.2ms** | **0** |
| 128         | 21 326  | 5.9ms | 5.2ms | 11.9ms | 17.4ms | 0      |
| 256         | 20 123  | 12.2ms | 9.3ms | 27.8ms | 46.1ms | 399    |

### Пиковая нагрузка
- **Peak RPS: 24 541** (256 потоков × 2000 запросов)
- **GET /health: 15 071 RPS** (128 потоков × 1000 запросов)

### Выводы
- Оптимальная точка: **64 потока** — максимум RPS (21K+) при нуле ошибок и P95 < 5ms
- Плато на ~21K RPS при 64+ потоках — ограничение в HttpClient loopback + JSON сериализация
- P99 латентность растёт линейно с конкурентностью: от 2.8ms (1 поток) до 46.1ms (256 потоков)
- При 256 потоках появляются ошибки (399/128000) — исчерпание лимита rate limiting (1M capacity)

### Запуск нагрузочного теста
```bash
mvn test -Dtest=LoadTest
```

## JSON-RPC 2.0 Методы

| Метод | Auth | Описание |
|---|---|---|
| `system.health` | Нет | Проверка здоровья сервера |
| `system.version` | Нет | Информация о версии |
| `system.help` | Нет | Список доступных методов |
| `auth.login` | Нет | Вход в систему, получение JWT токена |
| `auth.refresh` | Нет | Обновление access токена |
| `auth.logout` | Да | Выход из системы |

## HTTP Endpoints

| Endpoint | Method | Описание |
|---|---|---|
| `/rpc` | POST | Основной JSON-RPC 2.0 endpoint |
| `/health` | GET | Health check |
| `/version` | GET | Информация о версии |
| `/help` | GET | HTML страница документации |

## Документация
- [API Reference](docs/API.md)
- [Design Spec](docs/superpowers/specs/2026-04-14-jserver-design.md)
- [Implementation Plan](docs/superpowers/plans/2026-04-14-jserver-implementation.md)

## Архитектура
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

## Структура проекта
```
src/main/java/org/jserver/
├── api/          — JSON-RPC DTOs, dispatcher
├── core/         — бизнес-логика (Auth, Health, RateLimit, Audit)
├── handlers/     — обработчики методов
├── infrastructure— БД, JWT, конфигурация
├── middleware/   — цепочка middleware
├── model/        — доменные модели
├── server/       — HTTP слой
└── JServerApplication.java — точка входа
```

## Технологии
- **Java 25+** — Virtual Threads, records, switch expressions, text blocks
- **JDK HttpServer** — встроенный HTTP сервер
- **H2** — in-memory база данных
- **SLF4J + Logback** — логирование
- **Jackson** — JSON сериализация
- **SnakeYAML** — конфигурация
- **JUnit 5 + JaCoCo** — тестирование и покрытие

## Лицензия
MIT
