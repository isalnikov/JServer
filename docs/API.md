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
    "code": -32700,
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

**Request:**
```json
{"jsonrpc":"2.0","method":"system.version","params":null,"id":1}
```

**Response:**
```json
{"jsonrpc":"2.0","result":{"version":"1.0.0-SNAPSHOT","buildTime":"...","buildNumber":"1"},"id":1}
```

### system.help
Список доступных методов. Auth: не требуется.

**Request:**
```json
{"jsonrpc":"2.0","method":"system.help","params":null,"id":1}
```

**Response:**
```json
{"jsonrpc":"2.0","result":{"methods":["auth.login","auth.logout","auth.refresh","system.health","system.help","system.version"]},"id":1}
```

### auth.login
Вход в систему. Auth: не требуется.

**Request:**
```json
{"jsonrpc":"2.0","method":"auth.login","params":{"username":"admin","password":"password"},"id":1}
```

**Response:**
```json
{"jsonrpc":"2.0","result":{"accessToken":"eyJ...","refreshToken":"eyJ...","expiresAt":"..."},"id":1}
```

### auth.refresh
Обновление access токена. Auth: не требуется.

**Request:**
```json
{"jsonrpc":"2.0","method":"auth.refresh","params":{"refreshToken":"eyJ..."},"id":1}
```

### auth.logout
Выход из системы. Auth: требуется (в разработке).

**Request:**
```json
{"jsonrpc":"2.0","method":"auth.logout","params":null,"id":1}
```

## HTTP Endpoints

### GET /health
Health check для load balancer.

**Response:**
```json
{"status":"healthy","uptime":"120s","timestamp":"..."}
```

### GET /version
Информация о сборке.

**Response:**
```json
{"version":"1.0.0-SNAPSHOT","buildTime":"...","buildNumber":"1"}
```

## Error Codes

| Код | Название | Описание |
|---|---|---|
| -32700 | Parse error | Неверный JSON |
| -32600 | Invalid request | Неверный формат запроса |
| -32601 | Method not found | Метод не зарегистрирован |
| -32602 | Invalid params | Неверные параметры |
| -32603 | Internal error | Внутренняя ошибка сервера |
| -32001 | Rate limit exceeded | Превышен лимит запросов |
| -32002 | Unauthorized | Нет или неверный JWT токен |
| -32003 | Forbidden | Нет прав для метода |

## Rate Limiting

- Алгоритм: Token Bucket
- По умолчанию: 100 запросов/минуту на IP
- Лимиты настраиваются per-method через application.yml

## Аутентификация

- JWT (HS256)
- Access token: 15 минут
- Refresh token: 7 дней
- Публичные методы (не требуют auth): system.health, system.version, system.help, auth.login, auth.refresh
