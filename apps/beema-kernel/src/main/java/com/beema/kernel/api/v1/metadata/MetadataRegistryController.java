package com.beema.kernel.api.v1.metadata;

import com.beema.kernel.api.v1.metadata.dto.FieldDefinitionResponse;
import com.beema.kernel.api.v1.metadata.dto.LayoutDefinitionResponse;
import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.service.metadata.MetadataRegistry;
import com.beema.kernel.service.metadata.model.CompiledObjectDefinition;
import com.beema.kernel.service.metadata.model.FieldDefinition;
import com.beema.kernel.service.metadata.model.LayoutDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metadata/registry")
@Tag(name = "Metadata Registry", description = "High-performance cached metadata lookups for fields and layouts")
public class MetadataRegistryController {

    private final MetadataRegistry metadataRegistry;

    public MetadataRegistryController(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }

    @GetMapping("/compiled/{typeCode}")
    @Operation(summary = "Get compiled object definition (RECOMMENDED)",
            description = "Returns the complete pre-compiled object definition with all fields, layouts, and " +
                    "pre-compiled JEXL expressions. This is the most efficient endpoint - single cache lookup " +
                    "returns ALL metadata for a type. Pre-compiled expressions are 10x faster than runtime parsing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compiled definition found"),
            @ApiResponse(responseCode = "404", description = "Definition not found")
    })
    public ResponseEntity<Map<String, Object>> getCompiledDefinition(
            @Parameter(description = "Agreement type code") @PathVariable String typeCode,
            @Parameter(description = "Tenant ID", required = true) @RequestParam UUID tenantId,
            @Parameter(description = "Market context", required = true) @RequestParam MarketContext marketContext) {

        return metadataRegistry.getCompiledDefinition(tenantId, typeCode, marketContext)
                .map(compiled -> {
                    // Convert to API-friendly format (without exposing JexlExpression objects)
                    Map<String, Object> response = new java.util.LinkedHashMap<>();
                    response.put("tenantId", compiled.tenantId());
                    response.put("typeCode", compiled.typeCode());
                    response.put("marketContext", compiled.marketContext());
                    response.put("displayName", compiled.displayName());
                    response.put("description", compiled.description());
                    response.put("fieldCount", compiled.fieldCount());
                    response.put("preCompiledExpressions", compiled.compiledExpressionCount());
                    response.put("compiledAt", compiled.compiledAt());

                    // Convert fields to responses
                    response.put("fields", compiled.allFields().stream()
                            .map(f -> FieldDefinitionResponse.from(f.toFieldDefinition()))
                            .toList());

                    response.put("calculatedFieldsSorted", compiled.calculatedFieldsSorted().stream()
                            .map(f -> FieldDefinitionResponse.from(f.toFieldDefinition()))
                            .toList());

                    if (compiled.hasLayout()) {
                        response.put("layout", LayoutDefinitionResponse.from(compiled.layout()));
                    }

                    response.put("metadata", compiled.typeMetadata());

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/fields")
    @Operation(summary = "Get all fields for an agreement type",
            description = "Returns cached field definitions for a specific agreement type, ordered by UI order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Field definitions returned")
    })
    public ResponseEntity<List<FieldDefinitionResponse>> getFieldsForType(
            @Parameter(description = "Tenant ID", required = true) @RequestParam UUID tenantId,
            @Parameter(description = "Agreement type code", required = true) @RequestParam String typeCode,
            @Parameter(description = "Market context", required = true) @RequestParam MarketContext marketContext) {
        List<FieldDefinition> fields = metadataRegistry.getFieldsForType(tenantId, typeCode, marketContext);
        return ResponseEntity.ok(fields.stream().map(FieldDefinitionResponse::from).toList());
    }

    @GetMapping("/fields/{attributeName}")
    @Operation(summary = "Get a single field definition",
            description = "Returns a cached field definition by attribute name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Field definition found"),
            @ApiResponse(responseCode = "404", description = "Field not found")
    })
    public ResponseEntity<FieldDefinitionResponse> getField(
            @Parameter(description = "Attribute name") @PathVariable String attributeName,
            @Parameter(description = "Tenant ID", required = true) @RequestParam UUID tenantId,
            @Parameter(description = "Market context", required = true) @RequestParam MarketContext marketContext) {
        return metadataRegistry.getField(tenantId, attributeName, marketContext)
                .map(field -> ResponseEntity.ok(FieldDefinitionResponse.from(field)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/fields/calculated")
    @Operation(summary = "Get calculated fields for an agreement type",
            description = "Returns calculated/virtual field definitions in dependency-resolved order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calculated field definitions returned")
    })
    public ResponseEntity<List<FieldDefinitionResponse>> getCalculatedFields(
            @Parameter(description = "Tenant ID", required = true) @RequestParam UUID tenantId,
            @Parameter(description = "Agreement type code", required = true) @RequestParam String typeCode,
            @Parameter(description = "Market context", required = true) @RequestParam MarketContext marketContext) {
        List<FieldDefinition> fields = metadataRegistry.getCalculatedFields(tenantId, typeCode, marketContext);
        return ResponseEntity.ok(fields.stream().map(FieldDefinitionResponse::from).toList());
    }

    @GetMapping("/layout")
    @Operation(summary = "Get UI layout for an agreement type",
            description = "Returns the cached layout definition with sections and field ordering for UI rendering")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Layout definition found"),
            @ApiResponse(responseCode = "404", description = "Layout not found")
    })
    public ResponseEntity<LayoutDefinitionResponse> getLayout(
            @Parameter(description = "Tenant ID", required = true) @RequestParam UUID tenantId,
            @Parameter(description = "Agreement type code", required = true) @RequestParam String typeCode,
            @Parameter(description = "Market context", required = true) @RequestParam MarketContext marketContext) {
        return metadataRegistry.getLayout(tenantId, typeCode, marketContext)
                .map(layout -> ResponseEntity.ok(LayoutDefinitionResponse.from(layout)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/cache/refresh")
    @Operation(summary = "Refresh metadata caches",
            description = "Triggers a cache refresh. If type parameters provided, refreshes only that type; otherwise refreshes all.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cache refreshed")
    })
    public ResponseEntity<Map<String, String>> refreshCache(
            @Parameter(description = "Tenant ID") @RequestParam(required = false) UUID tenantId,
            @Parameter(description = "Agreement type code") @RequestParam(required = false) String typeCode,
            @Parameter(description = "Market context") @RequestParam(required = false) MarketContext marketContext) {
        if (tenantId != null && typeCode != null && marketContext != null) {
            metadataRegistry.refreshForType(tenantId, typeCode, marketContext);
            return ResponseEntity.ok(Map.of("status", "refreshed",
                    "scope", typeCode + " [" + marketContext + "]"));
        }
        metadataRegistry.refreshAll();
        return ResponseEntity.ok(Map.of("status", "refreshed", "scope", "all"));
    }

    @GetMapping("/cache/stats")
    @Operation(summary = "Get cache statistics",
            description = "Returns hit rates, sizes, and eviction counts for all metadata caches")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cache statistics returned")
    })
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ResponseEntity.ok(metadataRegistry.getCacheStats());
    }
}
