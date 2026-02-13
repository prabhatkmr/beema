package com.beema.kernel.repository.task;

import com.beema.kernel.domain.task.SysTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<SysTask, UUID> {

    List<SysTask> findByAssigneeRoleAndStatus(String assigneeRole, String status);

    List<SysTask> findByTenantIdAndStatus(String tenantId, String status);

    List<SysTask> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<SysTask> findByWorkflowIdAndRunId(String workflowId, String runId);

    List<SysTask> findByStatusOrderByCreatedAtAsc(String status);
}
