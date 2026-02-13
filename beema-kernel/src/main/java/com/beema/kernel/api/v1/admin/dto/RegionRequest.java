package com.beema.kernel.api.v1.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record RegionRequest(
    @NotBlank @Size(max = 20) String code,
    @NotBlank @Size(max = 100) String name,
    String description,
    Map<String, Object> dataResidencyRules,
    Boolean isActive
) {}
