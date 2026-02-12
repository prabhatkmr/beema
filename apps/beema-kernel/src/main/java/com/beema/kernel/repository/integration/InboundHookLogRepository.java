package com.beema.kernel.repository.integration;

import com.beema.kernel.domain.integration.InboundHookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InboundHookLogRepository extends JpaRepository<InboundHookLog, UUID> {
}
