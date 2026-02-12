package com.beema.kernel.repository.metadata;

import com.beema.kernel.domain.metadata.MetadataTypeAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MetadataTypeAttributeRepository extends JpaRepository<MetadataTypeAttribute, UUID> {

    List<MetadataTypeAttribute> findByAgreementTypeId(UUID agreementTypeId);
}
