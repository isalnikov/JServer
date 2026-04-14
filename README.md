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
java -Djwt.secret=my-secret-key -Dserver.port=9090 -jar target/jserver-1.0.0-SNAPSHOT.jar
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

## Конфигурация
Все параметры в `src/main/resources/application.yml` или через переменные окружения:
- `SERVER_PORT` — порт (по умолчанию 8080)
- `SERVER_HOST` — хост (по умолчанию 0.0.0.0)
- `JWT_SECRET` — секретный ключ JWT
- `DB_URL` — URL базы данных

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

## Лицензия
MIT
