package com.beema.kernel.service.metadata.model;

import com.beema.kernel.domain.agreement.MarketContext;

import java.util.List;

public record LayoutDefinition(
        String typeCode,
        MarketContext marketContext,
        String layoutType,
        List<LayoutSection> sections
) {
}
