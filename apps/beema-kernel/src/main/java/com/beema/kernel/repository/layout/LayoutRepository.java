package com.beema.kernel.repository.layout;

import com.beema.kernel.domain.layout.Layout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LayoutRepository extends JpaRepository<Layout, UUID> {

    /**
     * Find layouts matching context and object type, ordered by priority
     * Resolution order:
     * 1. Tenant-specific layouts first
     * 2. Role-specific layouts first
     * 3. Lower priority number wins
     * 4. Higher version wins
     */
    @Query("""
        SELECT l FROM Layout l
        WHERE l.enabled = true
          AND l.context = :context
          AND l.objectType = :objectType
          AND l.marketContext = :marketContext
          AND (l.tenantId = :tenantId OR l.tenantId IS NULL)
          AND (l.role = :role OR l.role IS NULL)
        ORDER BY
          CASE WHEN l.tenantId = :tenantId THEN 1 ELSE 2 END,
          CASE WHEN l.role = :role THEN 1 ELSE 2 END,
          l.priority ASC,
          l.version DESC
        """)
    List<Layout> findMatchingLayouts(
        @Param("context") String context,
        @Param("objectType") String objectType,
        @Param("marketContext") String marketContext,
        @Param("tenantId") String tenantId,
        @Param("role") String role
    );

    /**
     * Find all enabled layouts for a specific context
     */
    @Query("""
        SELECT l FROM Layout l
        WHERE l.enabled = true
          AND l.context = :context
        ORDER BY l.layoutName ASC
        """)
    List<Layout> findAllByContext(@Param("context") String context);

    /**
     * Find all enabled layouts
     */
    @Query("""
        SELECT l FROM Layout l
        WHERE l.enabled = true
        ORDER BY l.context ASC, l.objectType ASC, l.layoutName ASC
        """)
    List<Layout> findAllEnabled();
}
