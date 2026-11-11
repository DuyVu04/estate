package com.project.estate.workflow;

import static org.junit.jupiter.api.Assertions.*;

import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.ReservationActor;
import com.project.estate.workflow.context.WorkflowContext;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkflowContextTest {

  @AfterEach
  void tearDown() {
    WorkflowContext.clear();
  }

  @Test
  @DisplayName("Should set and get WorkflowContext on current thread")
  void setAndGet_Success() {
    WorkflowContext context =
        WorkflowContext.builder()
            .workflowName("reservation-workflow")
            .targetId("res-999")
            .action(ReservationAction.CREATE)
            .actor(ReservationActor.CUSTOMER)
            .userId("user-123")
            .build();

    WorkflowContext.set(context);

    assertTrue(WorkflowContext.getOptional().isPresent());
    assertEquals("res-999", WorkflowContext.get().getTargetId());
    assertEquals(ReservationAction.CREATE, WorkflowContext.get().getAction());
  }

  @Test
  @DisplayName("Should clear ThreadLocal context successfully")
  void clear_Success() {
    WorkflowContext context = WorkflowContext.builder().targetId("res-999").build();

    WorkflowContext.set(context);
    assertTrue(WorkflowContext.getOptional().isPresent());

    WorkflowContext.clear();
    assertTrue(WorkflowContext.getOptional().isEmpty());
  }

  @Test
  @DisplayName("Should ensure ThreadLocal context is isolated between separate threads")
  void threadIsolation_Success() throws InterruptedException {
    WorkflowContext mainThreadContext =
        WorkflowContext.builder().targetId("main-thread-target").build();
    WorkflowContext.set(mainThreadContext);

    AtomicReference<String> subThreadTargetId = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    Thread subThread =
        new Thread(
            () -> {
              WorkflowContext subContext =
                  WorkflowContext.builder().targetId("sub-thread-target").build();
              WorkflowContext.set(subContext);

              subThreadTargetId.set(WorkflowContext.get().getTargetId());
              WorkflowContext.clear();
              latch.countDown();
            });

    subThread.start();
    latch.await();

    // Verify sub thread received its own context
    assertEquals("sub-thread-target", subThreadTargetId.get());

    // Verify main thread context remains untouched
    assertEquals("main-thread-target", WorkflowContext.get().getTargetId());
  }
}
