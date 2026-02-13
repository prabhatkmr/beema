package com.beema.kernel.repository.admin;

import com.beema.kernel.domain.admin.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByTenantId(String tenantId);

    Optional<Tenant> findBySlug(String slug);

    List<Tenant> findByStatus(String status);

    List<Tenant> findByRegionCode(String regionCode);

    List<Tenant> findByTier(String tier);

    List<Tenant> findByStatusAndRegionCode(String status, String regionCode);

    boolean existsByTenantId(String tenantId);

    boolean existsBySlug(String slug);
}
