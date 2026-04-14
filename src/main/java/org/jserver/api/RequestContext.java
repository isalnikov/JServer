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
 * @param roles набор ролей пользователя
 */
public record RequestContext(
    String requestId,
    String clientIpAddress,
    UUID userId,
    Set<String> roles
) {

    /**
     * Создаёт контекст с автогенерированным requestId.
     *
     * @param clientIpAddress IP-адрес клиента
     * @param userId идентификатор пользователя (null для анонимов)
     * @param роли набор ролей
     * @return новый контекст запроса
     */
    public static RequestContext anonymous(String clientIpAddress, UUID userId, Set<String> roles) {
        return new RequestContext(UUID.randomUUID().toString(), clientIpAddress, userId, roles);
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
