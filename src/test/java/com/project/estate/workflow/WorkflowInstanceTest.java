package com.project.estate.workflow;

import static org.junit.jupiter.api.Assertions.*;

import com.project.estate.entity.WorkflowInstance;
import com.project.estate.enums.WorkflowInstanceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkflowInstanceTest {

  @Test
  @DisplayName("Should instantiate WorkflowInstance correctly using SuperBuilder")
  void createWorkflowInstance_Success() {
    WorkflowInstance instance =
        WorkflowInstance.builder()
            .workflowName("reservation-workflow")
            .targetId("res-12345")
            .status(WorkflowInstanceStatus.IN_PROGRESS)
            .currentStep("create-reservation")
            .build();

    assertEquals("reservation-workflow", instance.getWorkflowName());
    assertEquals("res-12345", instance.getTargetId());
    assertEquals(WorkflowInstanceStatus.IN_PROGRESS, instance.getStatus());
    assertEquals("create-reservation", instance.getCurrentStep());
  }

  @Test
  @DisplayName("Should update currentStep and status on workflow progression")
  void updateWorkflowInstance_Success() {
    WorkflowInstance instance =
        WorkflowInstance.builder()
            .workflowName("reservation-workflow")
            .targetId("res-12345")
            .status(WorkflowInstanceStatus.IN_PROGRESS)
            .currentStep("create-reservation")
            .build();

    // Progress step to completion
    instance.setCurrentStep("complete-reservation");
    instance.setStatus(WorkflowInstanceStatus.COMPLETED);

    assertEquals("complete-reservation", instance.getCurrentStep());
    assertEquals(WorkflowInstanceStatus.COMPLETED, instance.getStatus());
  }
}
