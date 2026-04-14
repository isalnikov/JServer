package org.jserver.server;

/**
 * Генератор HTML страницы документации /help.
 * Содержит описание сервера, список методов, примеры использования и инструкции по запуску.
 */
public final class HelpPageGenerator {

    private HelpPageGenerator() {
        // Утилитный класс — предотвращаем создание экземпляров
    }

    /**
     * Генерирует HTML страницу с документацией сервера.
     *
     * @return HTML строка с документацией
     */
    public static String generate() {
        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>JServer — Документация</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                           line-height: 1.6; color: #333; max-width: 960px; margin: 0 auto; padding: 20px;
                           background: #f8f9fa; }
                    h1 { color: #1a1a2e; border-bottom: 3px solid #16213e; padding-bottom: 10px; margin-bottom: 20px; }
                    h2 { color: #16213e; margin: 30px 0 15px; border-left: 4px solid #0f3460; padding-left: 10px; }
                    h3 { color: #0f3460; margin: 20px 0 10px; }
                    .badge { display: inline-block; background: #0f3460; color: #fff; padding: 4px 12px;
                             border-radius: 12px; font-size: 0.85em; margin-right: 8px; }
                    .badge.green { background: #28a745; }
                    .badge.orange { background: #fd7e14; }
                    pre { background: #1a1a2e; color: #e8e8e8; padding: 16px; border-radius: 8px;
                          overflow-x: auto; margin: 10px 0; font-size: 0.9em; }
                    code { background: #e9ecef; padding: 2px 6px; border-radius: 4px; font-size: 0.9em; }
                    pre code { background: none; padding: 0; }
                    .endpoint { background: #fff; border: 1px solid #dee2e6; border-radius: 8px;
                                padding: 16px; margin: 12px 0; }
                    .endpoint-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
                    .method { font-weight: bold; font-family: monospace; font-size: 1.1em; }
                    .method.public { color: #28a745; }
                    .method.auth { color: #007bff; }
                    table { width: 100%%; border-collapse: collapse; margin: 10px 0; }
                    th, td { padding: 10px 12px; text-align: left; border: 1px solid #dee2e6; }
                    th { background: #16213e; color: #fff; }
                    tr:nth-child(even) { background: #f8f9fa; }
                    .card { background: #fff; border-radius: 8px; padding: 20px; margin: 15px 0;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .footer { text-align: center; margin-top: 40px; padding: 20px; color: #6c757d;
                              border-top: 1px solid #dee2e6; }
                </style>
            </head>
            <body>
                <h1>🚀 JServer v1.0.0-SNAPSHOT</h1>
                <p>
                    <span class="badge">Java 25+</span>
                    <span class="badge green">Virtual Threads</span>
                    <span class="badge green">JSON-RPC 2.0</span>
                    <span class="badge orange">JWT Auth</span>
                    <span class="badge orange">Rate Limiting</span>
                </p>

                <div class="card">
                    <h2>📋 Описание</h2>
                    <p>
                        <strong>JServer</strong> — высокопроизводительный JSON-RPC 2.0 сервер,
                        написанный на чистом Java 25+ с использованием <code>Virtual Threads</code>.
                        Способен обрабатывать до 200 000 rps.
                    </p>
                    <p>
                        Предоставляет встроенную аутентификацию (JWT + RBAC), rate limiting (token bucket),
                        audit-логирование и H2 базу данных.
                    </p>
                </div>

                <h2>⚡ Быстрый старт</h2>
                <div class="card">
                    <h3>Сборка</h3>
                    <pre><code>mvn clean package</code></pre>

                    <h3>Запуск</h3>
                    <pre><code>java -jar target/jserver-1.0.0-SNAPSHOT.jar</code></pre>

                    <h3>Запуск с параметрами</h3>
                    <pre><code># Свой порт и секрет JWT
java -Djwt.secret=my-secret-key -Dserver.port=9090 -jar target/jserver-1.0.0-SNAPSHOT.jar

# Через переменные окружения
JWT_SECRET=my-secret SERVER_PORT=9090 java -jar target/jserver-1.0.0-SNAPSHOT.jar</code></pre>

                    <h3>Проверка работоспособности</h3>
                    <pre><code>curl http://localhost:8080/health</code></pre>
                </div>

                <h2>🌐 HTTP Endpoints</h2>

                <div class="endpoint">
                    <div class="endpoint-header">
                        <code style="background:#28a745;color:#fff;padding:4px 10px;border-radius:4px;">GET</code>
                        <span class="method public">/health</span>
                    </div>
                    <p>Проверка здоровья сервера. Не требует авторизации.</p>
                    <pre><code>$ curl http://localhost:8080/health
{
  "status": "healthy",
  "uptime": "120s",
  "timestamp": "2026-04-14T10:00:00Z"
}</code></pre>
                </div>

                <div class="endpoint">
                    <div class="endpoint-header">
                        <code style="background:#28a745;color:#fff;padding:4px 10px;border-radius:4px;">GET</code>
                        <span class="method public">/version</span>
                    </div>
                    <p>Информация о версии и сборке.</p>
                    <pre><code>$ curl http://localhost:8080/version
{
  "version": "1.0.0-SNAPSHOT",
  "buildTime": "2026-04-14T00:00:00Z",
  "buildNumber": "1"
}</code></pre>
                </div>

                <div class="endpoint">
                    <div class="endpoint-header">
                        <code style="background:#007bff;color:#fff;padding:4px 10px;border-radius:4px;">POST</code>
                        <span class="method">/rpc</span>
                    </div>
                    <p>Основной JSON-RPC 2.0 endpoint для вызова всех методов.</p>
                </div>

                <h2>📡 JSON-RPC 2.0 Методы</h2>

                <table>
                    <tr>
                        <th>Метод</th>
                        <th>Auth</th>
                        <th>Описание</th>
                    </tr>
                    <tr>
                        <td><code>system.health</code></td>
                        <td><span class="badge green">Нет</span></td>
                        <td>Проверка здоровья сервера</td>
                    </tr>
                    <tr>
                        <td><code>system.version</code></td>
                        <td><span class="badge green">Нет</span></td>
                        <td>Информация о версии</td>
                    </tr>
                    <tr>
                        <td><code>system.help</code></td>
                        <td><span class="badge green">Нет</span></td>
                        <td>Список доступных методов</td>
                    </tr>
                    <tr>
                        <td><code>auth.login</code></td>
                        <td><span class="badge green">Нет</span></td>
                        <td>Вход в систему, получение JWT токена</td>
                    </tr>
                    <tr>
                        <td><code>auth.refresh</code></td>
                        <td><span class="badge green">Нет</span></td>
                        <td>Обновление access токена</td>
                    </tr>
                    <tr>
                        <td><code>auth.logout</code></td>
                        <td><span class="badge orange">Да</span></td>
                        <td>Выход из системы</td>
                    </tr>
                </table>

                <h2>💡 Примеры JSON-RPC запросов</h2>

                <div class="card">
                    <h3>system.health</h3>
                    <pre><code>$ curl -X POST http://localhost:8080/rpc \\
  -H "Content-Type: application/json" \\
  -d '{"jsonrpc":"2.0","method":"system.health","params":null,"id":1}'

{
  "jsonrpc": "2.0",
  "result": {"status": "healthy", "uptime": "15s", "timestamp": "..."},
  "error": null,
  "id": 1
}</code></pre>
                </div>

                <div class="card">
                    <h3>system.help — список методов</h3>
                    <pre><code>$ curl -s -X POST http://localhost:8080/rpc \\
  -H "Content-Type: application/json" \\
  -d '{"jsonrpc":"2.0","method":"system.help","params":null,"id":1}'

{
  "jsonrpc": "2.0",
  "result": {
    "methods": [
      "auth.login", "auth.logout", "auth.refresh",
      "system.health", "system.help", "system.version"
    ]
  },
  "error": null,
  "id": 1
}</code></pre>
                </div>

                <div class="card">
                    <h3>auth.login — получение токена</h3>
                    <pre><code>$ curl -s -X POST http://localhost:8080/rpc \\
  -H "Content-Type: application/json" \\
  -d '{"jsonrpc":"2.0","method":"auth.login",
       "params":{"username":"admin","password":"password"},"id":1}'

{
  "jsonrpc": "2.0",
  "result": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "expiresAt": "2026-04-14T11:15:00Z"
  },
  "error": null,
  "id": 1
}</code></pre>
                </div>

                <div class="card">
                    <h3>auth.refresh — обновление токена</h3>
                    <pre><code>$ curl -s -X POST http://localhost:8080/rpc \\
  -H "Content-Type: application/json" \\
  -d '{"jsonrpc":"2.0","method":"auth.refresh",
       "params":{"refreshToken":"eyJhbGci..."},"id":2}'

{
  "jsonrpc": "2.0",
  "result": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "expiresAt": "2026-04-14T11:30:00Z"
  },
  "error": null,
  "id": 2
}</code></pre>
                </div>

                <div class="card">
                    <h3>Обработка ошибок</h3>
                    <h4>Неверный JSON</h4>
                    <pre><code>$ curl -s -X POST http://localhost:8080/rpc \\
  -H "Content-Type: application/json" -d 'not json'

{
  "jsonrpc": "2.0",
  "error": {"code": -32700, "message": "Parse error"},
  "id": null
}</code></pre>

                    <h4>Неизвестный метод</h4>
                    <pre><code>{
  "jsonrpc": "2.0",
  "error": {"code": -32601, "message": "Method not found: unknown.method"},
  "id": 1
}</code></pre>
                </div>

                <h2>🔒 Коды ошибок</h2>
                <table>
                    <tr><th>Код</th><th>Название</th><th>Описание</th></tr>
                    <tr><td><code>-32700</code></td><td>Parse error</td><td>Неверный JSON</td></tr>
                    <tr><td><code>-32600</code></td><td>Invalid request</td><td>Неверный формат запроса</td></tr>
                    <tr><td><code>-32601</code></td><td>Method not found</td><td>Метод не зарегистрирован</td></tr>
                    <tr><td><code>-32602</code></td><td>Invalid params</td><td>Неверные параметры</td></tr>
                    <tr><td><code>-32603</code></td><td>Internal error</td><td>Внутренняя ошибка сервера</td></tr>
                    <tr><td><code>-32001</code></td><td>Rate limit exceeded</td><td>Превышен лимит запросов</td></tr>
                    <tr><td><code>-32002</code></td><td>Unauthorized</td><td>Нет или неверный JWT токен</td></tr>
                    <tr><td><code>-32003</code></td><td>Forbidden</td><td>Нет прав для метода</td></tr>
                </table>

                <h2>⚙️ Конфигурация</h2>
                <div class="card">
                    <p>Все параметры в <code>application.yml</code> или через переменные окружения:</p>
                    <table>
                        <tr><th>Переменная</th><th>По умолчанию</th><th>Описание</th></tr>
                        <tr><td><code>SERVER_PORT</code></td><td><code>8080</code></td><td>Порт сервера</td></tr>
                        <tr><td><code>SERVER_HOST</code></td><td><code>0.0.0.0</code></td><td>Хост</td></tr>
                        <tr><td><code>JWT_SECRET</code></td><td><code>change-me-...</code></td><td>Секрет JWT (мин. 256 бит)</td></tr>
                        <tr><td><code>DB_URL</code></td><td><code>jdbc:h2:mem:jserver</code></td><td>URL базы данных</td></tr>
                    </table>
                </div>

                <h2>🛡️ Rate Limiting</h2>
                <div class="card">
                    <p>Алгоритм: <strong>Token Bucket</strong></p>
                    <ul>
                        <li>По умолчанию: <strong>100 запросов/минуту</strong> на один IP</li>
                        <li>Настраивается per-method через <code>application.yml</code></li>
                        <li>При превышении: ошибка <code>-32001 Rate limit exceeded</code></li>
                    </ul>
                </div>

                <div class="footer">
                    <p>JServer v1.0.0-SNAPSHOT | Java 25+ | JSON-RPC 2.0 | MIT License</p>
                    <p><a href="https://github.com/isalnikov/JServer">GitHub</a></p>
                </div>
            </body>
            </html>
            """.formatted();
    }
}
