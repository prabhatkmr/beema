package com.beema.processor.config;

import java.io.Serializable;

/**
 * Database configuration for accessing sys_message_hooks.
 */
public class DatabaseConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseConfig(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public static DatabaseConfig fromEnv() {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "5433");
        String database = System.getenv().getOrDefault("DB_NAME", "beema_kernel");
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
        String username = System.getenv().getOrDefault("DB_USERNAME", "beema");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "beema");

        return new DatabaseConfig(jdbcUrl, username, password);
    }
}
