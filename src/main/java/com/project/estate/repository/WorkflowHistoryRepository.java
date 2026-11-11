package com.project.estate.repository;

import com.project.estate.entity.WorkflowHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistory, String> {

  List<WorkflowHistory> findByWorkflowInstanceIdOrderByCreatedAtAsc(String workflowInstanceId);
}
