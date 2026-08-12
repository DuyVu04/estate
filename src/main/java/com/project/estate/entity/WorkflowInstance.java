package com.project.estate.entity;

import com.project.estate.enums.WorkflowInstanceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a persistent runtime execution of a Workflow. Tracks the target entity ID,
 * current workflow step, and execution status.
 */
@Entity
@Table(name = "workflow_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WorkflowInstance extends AbstractAuditEntity {

  @Column(name = "workflow_name", nullable = false, length = 100)
  private String workflowName;

  @Column(name = "target_id", nullable = false, length = 64)
  private String targetId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private WorkflowInstanceStatus status;

  @Column(name = "current_step", nullable = false, length = 100)
  private String currentStep;
}
