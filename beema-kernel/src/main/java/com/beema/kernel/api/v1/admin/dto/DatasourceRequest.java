package com.beema.kernel.api.v1.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record DatasourceRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 500) String url,
    @NotBlank @Size(max = 100) String username,
    Integer poolSize,
    String status,
    Map<String, Object> config
) {}
