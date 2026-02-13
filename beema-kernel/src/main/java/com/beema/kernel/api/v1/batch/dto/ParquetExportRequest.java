package com.beema.kernel.api.v1.batch.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public class ParquetExportRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    private LocalDate fromDate;

    private LocalDate toDate;

    public ParquetExportRequest() {
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }
}
