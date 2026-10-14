package com.project.estate.modules.reservation.workflow.repository;

import com.project.estate.modules.reservation.workflow.entity.WorkflowInstance;
import com.project.estate.modules.reservation.workflow.enums.WorkflowInstanceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, String> {

  Optional<WorkflowInstance> findByWorkflowNameAndTargetId(String workflowName, String targetId);

  List<WorkflowInstance> findByStatus(WorkflowInstanceStatus status);

  boolean existsByWorkflowNameAndTargetId(String workflowName, String targetId);
}
