package com.project.estate.repository;

import com.project.estate.entity.WorkflowHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistory, String> {

    List<WorkflowHistory> findByWorkflowInstanceIdOrderByCreatedAtAsc(String workflowInstanceId);
}
