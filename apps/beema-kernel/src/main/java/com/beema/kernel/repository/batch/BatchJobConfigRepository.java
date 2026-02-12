package com.beema.kernel.repository.batch;

import com.beema.kernel.domain.batch.BatchJobConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchJobConfigRepository extends JpaRepository<BatchJobConfig, UUID> {

    Optional<BatchJobConfig> findByJobName(String jobName);

    List<BatchJobConfig> findByEnabledTrue();

    List<BatchJobConfig> findByTenantId(String tenantId);
}
