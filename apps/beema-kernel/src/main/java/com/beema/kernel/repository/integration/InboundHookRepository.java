package com.beema.kernel.repository.integration;

import com.beema.kernel.domain.integration.InboundHook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InboundHookRepository extends JpaRepository<InboundHook, String> {

    Optional<InboundHook> findByHookIdAndIsActive(String hookId, Boolean isActive);

    List<InboundHook> findByTenantIdAndIsActive(String tenantId, Boolean isActive);
}
