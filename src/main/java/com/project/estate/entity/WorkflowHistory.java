package com.project.estate.entity;

import com.project.estate.enums.WorkflowHistoryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entity storing immutable audit history trail for every workflow step execution.
 */
@Entity
@Table(name = "workflow_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WorkflowHistory extends AbstractAuditEntity {

    @Column(name = "workflow_instance_id", nullable = false, length = 36)
    private String workflowInstanceId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 100)
    private String step;

    @Column(name = "previous_status", length = 50)
    private String previousStatus;

    @Column(name = "new_status", length = 50)
    private String newStatus;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkflowHistoryStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
