package com.project.estate.workflow;

import static org.junit.jupiter.api.Assertions.*;

import com.project.estate.enums.ReservationAction;
import com.project.estate.workflow.annotation.WorkflowEngine;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkflowAnnotationTest {

  static class SampleService {
    @WorkflowEngine(
        action = ReservationAction.CREATE,
        step = "create-reservation",
        workflowName = "reservation-workflow",
        targetIdSpel = "#result.id")
    public String createReservation() {
      return "res-123";
    }
  }

  @Test
  @DisplayName("Should read @WorkflowEngine annotation metadata via Reflection")
  void readWorkflowEngineAnnotation_Success() throws NoSuchMethodException {
    Method method = SampleService.class.getMethod("createReservation");

    assertTrue(method.isAnnotationPresent(WorkflowEngine.class));

    WorkflowEngine annotation = method.getAnnotation(WorkflowEngine.class);

    assertEquals(ReservationAction.CREATE, annotation.action());
    assertEquals("create-reservation", annotation.step());
    assertEquals("reservation-workflow", annotation.workflowName());
    assertEquals("#result.id", annotation.targetIdSpel());
  }
}
