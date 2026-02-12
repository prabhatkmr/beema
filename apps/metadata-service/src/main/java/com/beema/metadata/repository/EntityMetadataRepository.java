package com.beema.metadata.repository;

import com.beema.metadata.model.EntityMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntityMetadataRepository extends JpaRepository<EntityMetadata, UUID> {

    Optional<EntityMetadata> findByEntityType(String entityType);
}
