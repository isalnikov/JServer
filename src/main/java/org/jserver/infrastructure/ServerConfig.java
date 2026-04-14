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

            String jwtSecretRaw = getEnvOr("JWT_SECRET", jwt.get("secret"), "change-me-in-production-min-256-bits");
            String jwtSecret = resolvePlaceholder(jwtSecretRaw, "JWT_SECRET", "change-me-in-production-min-256-bits");
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

    /**
     * Возвращает конфигурацию со значениями по умолчанию.
     *
     * @return конфигурация по умолчанию
     */
    public static ServerConfig defaults() {
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

    /**
     * Разрешает placeholder вида ${ENV_VAR:default} в значение.
     * Если переменная окружения установлена, возвращает её значение,
     * иначе возвращает значение по умолчанию из placeholder.
     */
    private static String resolvePlaceholder(String value, String envName, String defaultValue) {
        if (value == null) return defaultValue;
        String str = value.toString();
        if (str.startsWith("${") && str.endsWith("}")) {
            String inner = str.substring(2, str.length() - 1);
            int colonIdx = inner.indexOf(':');
            if (colonIdx >= 0) {
                String envVarName = inner.substring(0, colonIdx);
                String fallback = inner.substring(colonIdx + 1);
                String envVal = System.getenv(envVarName);
                return envVal != null && !envVal.isEmpty() ? envVal : fallback;
            }
        }
        return str;
    }
}
