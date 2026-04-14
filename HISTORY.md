# JServer — История разработки

**Дата:** 14 апреля 2026  
**Подход:** TDD + Subagent-Driven Development  
**Итого:** 16+ коммитов, 30 файлов, 96 тестов

## Хронология

### Фаза 1: Спецификация и планирование
1. Ознакомление с README — определёны требования: Java 25+, JSON-RPC 2.0, JWT auth, rate limiting, H2, TDD
2. Brainstorming: уточнены требования через 7 вопросов (бизнес-логика, расширяемость, auth, БД, протокол, сервер, структура)
3. Создан SDD документ: `docs/superpowers/specs/2026-04-14-jserver-design.md`
   - Архитектура: минималистичный монолит с чёткими слоями
   - JSON-RPC 2.0 с middleware pipeline
   - JWT HS256 + RBAC
   - Token bucket rate limiting
4. Создан план реализации: `docs/superpowers/plans/2026-04-14-jserver-implementation.md` (9 задач)

### Фаза 2: Реализация (Subagent-Driven)

#### Task 1: Maven проект
- pom.xml: JDK 25, SLF4J, Logback, H2, Jackson, SnakeYAML, JUnit 5, JaCoCo
- application.yml, logback.xml, JServerApplication.java (stub)
- **Review fix:** Убран System.out.println (нарушает требования), версии вынесены в properties

#### Task 2: Domain модели и JSON-RPC DTO
- User, AuditEntry, RefreshToken (records)
- JsonRpcRequest, JsonRpcResponse, JsonRpcError (records с factory methods)
- **Review fix:** Error codes перенесены в диапазон -32000 (JSON-RPC 2.0 server-defined), добавлены тесты на factory methods

#### Task 3: RequestContext, ServerConfig, H2DataSource
- RequestContext: static factory method `anonymous()`, hasRole()
- ServerConfig: чтение application.yml + env var overrides
- H2DataSource: инициализация 4 таблиц (users, audit_log, rate_limit_config, refresh_tokens)

#### Task 4: RateLimitService + RateLimitMiddleware
- Token bucket алгоритм (ConcurrentHashMap + synchronized)
- Middleware интерфейс + MiddlewareChain
- **Review fix:** Убран Mockito (несовместим с Java 25), исправлен typo

#### Task 5: JwtProvider + AuthService + AuthMiddleware
- JWT HS256 генерация/валидация, hashToken()
- AuthService: login/logout/refresh (stub password check)
- AuthMiddleware: блокирует непубличные методы без JWT
- AuditService + AuditRepository (stubs, нужны для AuthService)
- **Review fix:** AuthMiddleware теперь извлекает токен из params, добавлены тесты на hashToken/accessTokenTtl/toMap

#### Task 6: LoggingMiddleware + Repositories
- LoggingMiddleware: логирует запросы/ответы с duration
- AuditRepository: полная H2 реализация (save + findRecent)
- UserRepository: findByUsername, findById, save
- ConfigRepository: getAllMethodConfigs, saveMethodConfig

#### Task 7: RpcDispatcher + Handlers
- RpcMethodHandler (@FunctionalInterface), RpcDispatcher
- HealthHandler (system.health, system.version)
- AuthHandler (auth.login, auth.refresh, auth.logout)
- SystemHelpHandler (sorted method list)

#### Task 8: HttpServerBootstrap + Integration
- HttpServerBootstrap: wiring всех компонентов, Virtual Threads
- RpcHttpHandler: HTTP → JSON-RPC (/rpc, /health, /version)
- JServerApplication: запуск сервера
- **Fix:** RequestContext.anonymous() вместо конструктора с dummy param

#### Task 9: Финальные штрихи
- .gitignore, docs/API.md, обновление README.md
- maven-shade-plugin для fat JAR
- JaCoCo thresholds: 70% lines / 60% branches
- SystemHelpHandlerTest, HttpServerBootstrapTest

### Фаза 3: Тестирование
- `mvn clean verify` — BUILD SUCCESS
- 96 тестов, 0 failures, 3 skipped (@Disabled integration tests)
- JaCoCo: 73% lines / 63% branches (порог 70/60)
- Живой тест: сервер запущен, все endpoints работают

## Итоговая структура
```
src/main/java/org/jserver/
├── api/          — JSON-RPC DTOs, dispatcher
├── core/         — Auth, Health, RateLimit, Audit
├── handlers/     — HealthHandler, AuthHandler, SystemHelpHandler
├── infrastructure— H2, JWT, Config, Repositories
├── middleware/   — Logging, RateLimit, Auth
├── model/        — User, AuditEntry, RefreshToken
├── server/       — HttpServerBootstrap, RpcHttpHandler
└── JServerApplication.java
```

## Технологии
- Java 25+ (Virtual Threads, records, switch expressions, text blocks)
- JDK HttpServer
- H2 (in-memory)
- SLF4J + Logback
- Jackson (JSON)
- SnakeYAML (config)
- JUnit 5, JaCoCo
