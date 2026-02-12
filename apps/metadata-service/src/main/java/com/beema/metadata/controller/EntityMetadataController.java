package com.beema.metadata.controller;

import com.beema.metadata.model.EntityMetadata;
import com.beema.metadata.repository.EntityMetadataRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metadata")
@Tag(name = "Entity Metadata", description = "Metadata management endpoints")
public class EntityMetadataController {

    private final EntityMetadataRepository repository;

    public EntityMetadataController(EntityMetadataRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "List all entity metadata")
    public ResponseEntity<List<EntityMetadata>> listAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get entity metadata by ID")
    public ResponseEntity<EntityMetadata> getById(@PathVariable UUID id) {
        return repository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{entityType}")
    @Operation(summary = "Get entity metadata by type")
    public ResponseEntity<EntityMetadata> getByType(@PathVariable String entityType) {
        return repository.findByEntityType(entityType)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new entity metadata")
    public ResponseEntity<EntityMetadata> create(@RequestBody EntityMetadata metadata) {
        metadata.setCreatedAt(Instant.now());
        metadata.setTransactionTime(Instant.now());
        if (metadata.getValidFrom() == null) {
            metadata.setValidFrom(Instant.now());
        }
        EntityMetadata saved = repository.save(metadata);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update entity metadata")
    public ResponseEntity<EntityMetadata> update(@PathVariable UUID id, @RequestBody EntityMetadata metadata) {
        return repository.findById(id)
            .map(existing -> {
                metadata.setId(id);
                metadata.setUpdatedAt(Instant.now());
                metadata.setCreatedAt(existing.getCreatedAt());
                metadata.setCreatedBy(existing.getCreatedBy());
                return ResponseEntity.ok(repository.save(metadata));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete entity metadata")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
