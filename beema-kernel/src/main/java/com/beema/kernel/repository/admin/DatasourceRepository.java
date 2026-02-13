package com.beema.kernel.repository.admin;

import com.beema.kernel.domain.admin.Datasource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatasourceRepository extends JpaRepository<Datasource, UUID> {

    Optional<Datasource> findByName(String name);

    List<Datasource> findByStatus(String status);

    boolean existsByName(String name);
}
