package com.beema.kernel.repository.admin;

import com.beema.kernel.domain.admin.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegionRepository extends JpaRepository<Region, UUID> {

    Optional<Region> findByCode(String code);

    List<Region> findByIsActive(Boolean isActive);

    boolean existsByCode(String code);
}
