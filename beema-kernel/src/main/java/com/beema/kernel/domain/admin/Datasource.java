package com.beema.kernel.domain.admin;

import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
    name = "sys_datasources",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_datasource_name", columnNames = {"name"})
    }
)
public class Datasource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "pool_size", nullable = false)
    private Integer poolSize = 20;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Convert(converter = JsonbConverter.class)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> config = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Datasource() {}

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Integer getPoolSize() { return poolSize; }
    public void setPoolSize(Integer poolSize) { this.poolSize = poolSize; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() {
        return "Datasource{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", url='" + url + '\'' +
               ", status='" + status + '\'' +
               ", poolSize=" + poolSize +
               '}';
    }
}
