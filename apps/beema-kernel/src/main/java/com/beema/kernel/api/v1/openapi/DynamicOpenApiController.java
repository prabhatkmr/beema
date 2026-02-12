package com.beema.kernel.api.v1.openapi;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.repository.metadata.MetadataAgreementTypeRepository;
import com.beema.kernel.service.metadata.MetadataRegistry;
import com.beema.kernel.service.metadata.model.CompiledObjectDefinition;
import com.beema.kernel.service.openapi.DynamicSchemaGenerator;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Dynamic OpenAPI Documentation Controller.
 *
 * Generates OpenAPI v3.0 specifications on-the-fly by introspecting
 * the Beema Metadata Registry. This allows API documentation to stay
 * in sync with metadata definitions without manual updates.
 *
 * Key Features:
 * - Automatically discovers all registered object types
 * - Generates schemas from metadata field definitions
 * - Creates CRUD endpoints for each object type
 * - Supports multi-tenancy and market contexts
 * - Can be imported into Postman, Swagger UI, or API clients
 *
 * Example Usage:
 * - View in browser: http://localhost:8080/api/v1/docs/openapi.json
 * - Import to Postman: Import -> Link -> paste URL
 * - View in Swagger UI: http://localhost:8080/swagger-ui/index.html?url=/api/v1/docs/openapi.json
 */
@RestController
@RequestMapping("/api/v1/docs")
public class DynamicOpenApiController {

    private static final Logger log = LoggerFactory.getLogger(DynamicOpenApiController.class);

    private final MetadataRegistry metadataRegistry;
    private final MetadataAgreementTypeRepository agreementTypeRepository;
    private final DynamicSchemaGenerator schemaGenerator;

    public DynamicOpenApiController(
            MetadataRegistry metadataRegistry,
            MetadataAgreementTypeRepository agreementTypeRepository,
            DynamicSchemaGenerator schemaGenerator) {
        this.metadataRegistry = metadataRegistry;
        this.agreementTypeRepository = agreementTypeRepository;
        this.schemaGenerator = schemaGenerator;
    }

    /**
     * Generates dynamic OpenAPI v3.0 specification.
     *
     * @param tenantId Optional tenant ID to filter objects (default: all tenants)
     * @param marketContext Optional market context filter (RETAIL, COMMERCIAL, LONDON_MARKET)
     * @return OpenAPI specification as JSON
     */
    @GetMapping(value = "/openapi.json", produces = "application/json")
    public ResponseEntity<OpenAPI> generateOpenApiSpec(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) MarketContext marketContext) {

        log.info("Generating dynamic OpenAPI spec for tenantId={}, marketContext={}",
                tenantId, marketContext);

        OpenAPI openAPI = new OpenAPI();

        // Set basic info
        openAPI.setInfo(buildApiInfo());

        // Add servers
        openAPI.setServers(buildServers());

        // Add security scheme
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("OAuth2 JWT Bearer Token - obtain from your identity provider");

        Components components = new Components();
        components.addSecuritySchemes("bearerAuth", securityScheme);
        openAPI.setComponents(components);

        // Add global security requirement
        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.addList("bearerAuth");
        openAPI.addSecurityItem(securityRequirement);

        // Generate paths from metadata
        io.swagger.v3.oas.models.Paths paths = new io.swagger.v3.oas.models.Paths();
        generateDynamicPaths(paths, tenantId, marketContext);
        openAPI.setPaths(paths);

        log.info("Generated OpenAPI spec with {} paths", paths.size());

        return ResponseEntity.ok(openAPI);
    }

    /**
     * Builds API information section.
     */
    private Info buildApiInfo() {
        Info info = new Info();
        info.setTitle("Beema Kernel - Dynamic API");
        info.setDescription("""
                Metadata-driven insurance platform API.

                This specification is generated dynamically from the Beema Metadata Registry.
                All endpoints support bitemporal data management with full audit history.

                **Features:**
                - Metadata-driven schemas
                - Multi-tenancy support
                - Market context routing (Retail, Commercial, London Market)
                - Bitemporal versioning (valid_time + transaction_time)
                - JSONB flex-schema for dynamic attributes

                **Authentication:**
                All endpoints require OAuth2/JWT authentication.
                Include the `Authorization: Bearer <token>` header in requests.

                **Multi-Tenancy:**
                Include `X-Tenant-ID` header to specify tenant context.
                """);
        info.setVersion("1.0.0-DYNAMIC");

        Contact contact = new Contact();
        contact.setName("Beema Platform Team");
        contact.setEmail("support@beema.io");
        info.setContact(contact);

        return info;
    }

    /**
     * Builds server configurations.
     */
    private List<Server> buildServers() {
        List<Server> servers = new ArrayList<>();

        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local development server");
        servers.add(localServer);

        Server productionServer = new Server();
        productionServer.setUrl("https://api.beema.io");
        productionServer.setDescription("Production server");
        servers.add(productionServer);

        return servers;
    }

    /**
     * Generates paths for all registered object types.
     */
    private void generateDynamicPaths(io.swagger.v3.oas.models.Paths paths, UUID tenantId, MarketContext marketContext) {
        // Get all active agreement types
        var agreementTypes = agreementTypeRepository.findAll();

        Set<String> processedTypes = new HashSet<>();

        for (var agreementType : agreementTypes) {
            // Filter by tenant if specified
            if (tenantId != null && !agreementType.getTenantId().equals(tenantId)) {
                continue;
            }

            // Filter by market context if specified
            if (marketContext != null && agreementType.getMarketContext() != marketContext) {
                continue;
            }

            // Skip inactive types
            if (!agreementType.getIsActive()) {
                continue;
            }

            // Get compiled definition
            Optional<CompiledObjectDefinition> defOpt = metadataRegistry.getCompiledDefinition(
                    agreementType.getTenantId(),
                    agreementType.getTypeCode(),
                    agreementType.getMarketContext()
            );

            if (defOpt.isEmpty()) {
                log.warn("No compiled definition found for type: {}", agreementType.getTypeCode());
                continue;
            }

            CompiledObjectDefinition objectDef = defOpt.get();

            // Generate unique path key
            String pathKey = generatePathKey(objectDef);

            // Skip if already processed (avoid duplicates)
            if (processedTypes.contains(pathKey)) {
                continue;
            }
            processedTypes.add(pathKey);

            // Create path item with CRUD operations
            PathItem pathItem = createPathItem(objectDef);
            paths.addPathItem(pathKey, pathItem);

            log.debug("Generated path: {} for object: {}", pathKey, objectDef.typeCode());
        }
    }

    /**
     * Generates a unique path key for an object definition.
     */
    private String generatePathKey(CompiledObjectDefinition objectDef) {
        // Use type code as the API name
        String apiName = objectDef.typeCode().toLowerCase().replace("_", "-");
        return "/api/v1/data/" + apiName;
    }

    /**
     * Creates a PathItem with CRUD operations for an object.
     */
    private PathItem createPathItem(CompiledObjectDefinition objectDef) {
        PathItem pathItem = new PathItem();

        // Add description
        pathItem.setDescription(String.format(
                "Operations for %s (%s - %s)",
                objectDef.displayName(),
                objectDef.typeCode(),
                objectDef.marketContext()
        ));

        // POST - Create
        pathItem.setPost(createPostOperation(objectDef));

        // GET - List
        pathItem.setGet(createGetListOperation(objectDef));

        return pathItem;
    }

    /**
     * Creates POST operation for creating a new object.
     */
    private Operation createPostOperation(CompiledObjectDefinition objectDef) {
        Operation operation = new Operation();
        operation.setOperationId("create" + toPascalCase(objectDef.typeCode()));
        operation.setSummary("Create new " + objectDef.displayName());
        operation.setDescription(String.format(
                "Creates a new %s instance. Automatically generates bitemporal versioning fields.",
                objectDef.displayName()
        ));

        // Tags
        operation.addTagsItem(objectDef.marketContext().toString());
        operation.addTagsItem(objectDef.typeCode());

        // Request body
        RequestBody requestBody = new RequestBody();
        requestBody.setDescription("Object data");
        requestBody.setRequired(true);

        Content content = new Content();
        MediaType mediaType = new MediaType();
        mediaType.setSchema(schemaGenerator.generateRequestSchema(objectDef));
        content.addMediaType("application/json", mediaType);
        requestBody.setContent(content);

        operation.setRequestBody(requestBody);

        // Responses
        ApiResponses responses = new ApiResponses();

        // 201 Created
        ApiResponse createdResponse = new ApiResponse();
        createdResponse.setDescription("Successfully created");
        Content createdContent = new Content();
        MediaType createdMediaType = new MediaType();
        createdMediaType.setSchema(schemaGenerator.generateResponseSchema(objectDef));
        createdContent.addMediaType("application/json", createdMediaType);
        createdResponse.setContent(createdContent);
        responses.addApiResponse("201", createdResponse);

        // 400 Bad Request
        responses.addApiResponse("400", createErrorResponse("Invalid request data"));

        // 401 Unauthorized
        responses.addApiResponse("401", createErrorResponse("Unauthorized"));

        // 403 Forbidden
        responses.addApiResponse("403", createErrorResponse("Forbidden - insufficient permissions"));

        operation.setResponses(responses);

        return operation;
    }

    /**
     * Creates GET operation for listing objects.
     */
    private Operation createGetListOperation(CompiledObjectDefinition objectDef) {
        Operation operation = new Operation();
        operation.setOperationId("list" + toPascalCase(objectDef.typeCode()));
        operation.setSummary("List " + objectDef.displayName() + " objects");
        operation.setDescription(String.format(
                "Retrieves a paginated list of %s instances. Supports filtering and temporal queries.",
                objectDef.displayName()
        ));

        // Tags
        operation.addTagsItem(objectDef.marketContext().toString());
        operation.addTagsItem(objectDef.typeCode());

        // Query parameters
        operation.addParametersItem(createPageParameter());
        operation.addParametersItem(createSizeParameter());
        operation.addParametersItem(createValidTimeParameter());
        operation.addParametersItem(createTransactionTimeParameter());

        // Responses
        ApiResponses responses = new ApiResponses();

        // 200 OK
        ApiResponse okResponse = new ApiResponse();
        okResponse.setDescription("Successfully retrieved list");
        Content okContent = new Content();
        MediaType okMediaType = new MediaType();
        okMediaType.setSchema(schemaGenerator.generateArraySchema(objectDef));
        okContent.addMediaType("application/json", okMediaType);
        okResponse.setContent(okContent);
        responses.addApiResponse("200", okResponse);

        // 401 Unauthorized
        responses.addApiResponse("401", createErrorResponse("Unauthorized"));

        // 403 Forbidden
        responses.addApiResponse("403", createErrorResponse("Forbidden"));

        operation.setResponses(responses);

        return operation;
    }

    /**
     * Creates page number parameter.
     */
    private Parameter createPageParameter() {
        QueryParameter param = new QueryParameter();
        param.setName("page");
        param.setDescription("Page number (0-indexed)");
        param.setRequired(false);
        io.swagger.v3.oas.models.media.IntegerSchema pageSchema = new io.swagger.v3.oas.models.media.IntegerSchema();
        pageSchema.setMinimum(java.math.BigDecimal.ZERO);
        pageSchema.setDefault(0);
        param.setSchema(pageSchema);
        return param;
    }

    /**
     * Creates page size parameter.
     */
    private Parameter createSizeParameter() {
        QueryParameter param = new QueryParameter();
        param.setName("size");
        param.setDescription("Page size");
        param.setRequired(false);
        io.swagger.v3.oas.models.media.IntegerSchema sizeSchema = new io.swagger.v3.oas.models.media.IntegerSchema();
        sizeSchema.setMinimum(java.math.BigDecimal.ONE);
        sizeSchema.setMaximum(java.math.BigDecimal.valueOf(100));
        sizeSchema.setDefault(20);
        param.setSchema(sizeSchema);
        return param;
    }

    /**
     * Creates valid_time parameter for temporal queries.
     */
    private Parameter createValidTimeParameter() {
        QueryParameter param = new QueryParameter();
        param.setName("valid_time");
        param.setDescription("Point-in-time for validity (ISO-8601 format)");
        param.setRequired(false);
        param.setSchema(new io.swagger.v3.oas.models.media.DateTimeSchema());
        return param;
    }

    /**
     * Creates transaction_time parameter for temporal queries.
     */
    private Parameter createTransactionTimeParameter() {
        QueryParameter param = new QueryParameter();
        param.setName("transaction_time");
        param.setDescription("Point-in-time for transaction (ISO-8601 format)");
        param.setRequired(false);
        param.setSchema(new io.swagger.v3.oas.models.media.DateTimeSchema());
        return param;
    }

    /**
     * Creates a generic error response.
     */
    private ApiResponse createErrorResponse(String description) {
        ApiResponse errorResponse = new ApiResponse();
        errorResponse.setDescription(description);

        Content errorContent = new Content();
        MediaType errorMediaType = new MediaType();

        // Error schema
        Schema<?> errorSchema = new io.swagger.v3.oas.models.media.ObjectSchema();
        errorSchema.addProperty("error", new io.swagger.v3.oas.models.media.StringSchema());
        errorSchema.addProperty("message", new io.swagger.v3.oas.models.media.StringSchema());
        errorSchema.addProperty("timestamp", new io.swagger.v3.oas.models.media.DateTimeSchema());

        errorMediaType.setSchema(errorSchema);
        errorContent.addMediaType("application/json", errorMediaType);
        errorResponse.setContent(errorContent);

        return errorResponse;
    }

    /**
     * Converts snake_case to PascalCase.
     */
    private String toPascalCase(String snakeCase) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }
}
