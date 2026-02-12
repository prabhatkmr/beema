package com.beema.kernel.api.v1.metadata.dto;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.service.metadata.model.LayoutDefinition;
import com.beema.kernel.service.metadata.model.LayoutSection;

import java.util.List;

public record LayoutDefinitionResponse(
        String typeCode,
        MarketContext marketContext,
        String layoutType,
        List<SectionResponse> sections
) {

    public record SectionResponse(
            String sectionName,
            int order,
            List<FieldDefinitionResponse> fields
    ) {

        public static SectionResponse from(LayoutSection section) {
            return new SectionResponse(
                    section.sectionName(),
                    section.order(),
                    section.fields().stream().map(FieldDefinitionResponse::from).toList()
            );
        }
    }

    public static LayoutDefinitionResponse from(LayoutDefinition layout) {
        return new LayoutDefinitionResponse(
                layout.typeCode(),
                layout.marketContext(),
                layout.layoutType(),
                layout.sections().stream().map(SectionResponse::from).toList()
        );
    }
}
