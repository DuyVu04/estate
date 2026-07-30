package com.project.estate.repository;

import com.project.estate.entity.WorkflowInstance;
import com.project.estate.enums.WorkflowInstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, String> {

    Optional<WorkflowInstance> findByWorkflowNameAndTargetId(String workflowName, String targetId);

    List<WorkflowInstance> findByStatus(WorkflowInstanceStatus status);

    boolean existsByWorkflowNameAndTargetId(String workflowName, String targetId);
}
