package com.beema.kernel.repository.policy;

import com.beema.kernel.domain.policy.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    /**
     * Find current version of a policy.
     */
    Optional<Policy> findByPolicyNumberAndIsCurrent(String policyNumber, Boolean isCurrent);

    /**
     * Find policy version as of a specific date (bitemporal query).
     */
    @Query("SELECT p FROM Policy p WHERE p.policyNumber = :policyNumber " +
           "AND p.validFrom <= :asOf AND p.validTo > :asOf " +
           "AND p.transactionTime <= :asOf")
    Optional<Policy> findAsOf(
            @Param("policyNumber") String policyNumber,
            @Param("asOf") LocalDateTime asOf
    );

    /**
     * Get all versions of a policy (audit history).
     */
    List<Policy> findByPolicyNumberOrderByVersionAsc(String policyNumber);

    /**
     * Get latest version number for a policy.
     */
    @Query("SELECT MAX(p.version) FROM Policy p WHERE p.policyNumber = :policyNumber")
    Integer getMaxVersion(@Param("policyNumber") String policyNumber);
}
