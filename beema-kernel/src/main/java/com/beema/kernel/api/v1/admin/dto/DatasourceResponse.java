package com.beema.kernel.api.v1.admin.dto;

import com.beema.kernel.domain.admin.Datasource;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record DatasourceResponse(
    UUID id,
    String name,
    String url,
    String username,
    Integer poolSize,
    String status,
    Map<String, Object> config,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static DatasourceResponse from(Datasource ds) {
        return new DatasourceResponse(
            ds.getId(),
            ds.getName(),
            ds.getUrl(),
            ds.getUsername(),
            ds.getPoolSize(),
            ds.getStatus(),
            ds.getConfig(),
            ds.getCreatedAt(),
            ds.getUpdatedAt()
        );
    }
}
