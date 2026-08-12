package com.project.estate.workflow;

import static org.junit.jupiter.api.Assertions.*;

import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.ReservationActor;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.workflow.definition.WorkflowDefinition;
import com.project.estate.workflow.definition.WorkflowDefinitionRegistry;
import com.project.estate.workflow.definition.WorkflowStepDefinition;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkflowDefinitionTest {

  private WorkflowDefinitionRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new WorkflowDefinitionRegistry();
  }

  @Test
  @DisplayName("Should retrieve default reservation workflow definition")
  void getDefinition_ReservationWorkflow_Success() {
    Optional<WorkflowDefinition> defOpt =
        registry.getDefinition(WorkflowDefinitionRegistry.RESERVATION_WORKFLOW);
    assertTrue(defOpt.isPresent());

    WorkflowDefinition definition = defOpt.get();
    assertEquals("reservation-workflow", definition.getWorkflowName());
    assertEquals(4, definition.getSteps().size());
  }

  @Test
  @DisplayName("Should find step by action CREATE")
  void findStepByAction_Create_Success() {
    WorkflowDefinition definition =
        registry.getDefinition(WorkflowDefinitionRegistry.RESERVATION_WORKFLOW).orElseThrow();

    Optional<WorkflowStepDefinition> stepOpt =
        definition.findStepByAction(ReservationAction.CREATE);
    assertTrue(stepOpt.isPresent());

    WorkflowStepDefinition step = stepOpt.get();
    assertEquals("create-reservation", step.getStepName());
    assertNull(step.getFromStatus());
    assertEquals(ReservationStatus.ACTIVE, step.getToStatus());
    assertTrue(step.getAllowedActors().contains(ReservationActor.CUSTOMER));
  }

  @Test
  @DisplayName("Should find step by stepName cancel-reservation")
  void findStepByName_Cancel_Success() {
    WorkflowDefinition definition =
        registry.getDefinition(WorkflowDefinitionRegistry.RESERVATION_WORKFLOW).orElseThrow();

    Optional<WorkflowStepDefinition> stepOpt = definition.findStepByName("cancel-reservation");
    assertTrue(stepOpt.isPresent());

    WorkflowStepDefinition step = stepOpt.get();
    assertEquals(ReservationAction.CANCEL, step.getAction());
    assertEquals(ReservationStatus.ACTIVE, step.getFromStatus());
    assertEquals(ReservationStatus.CANCELLED, step.getToStatus());
    assertTrue(step.getAllowedActors().contains(ReservationActor.CUSTOMER));
    assertTrue(step.getAllowedActors().contains(ReservationActor.ADMIN));
  }
}
