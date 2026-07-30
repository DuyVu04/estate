package com.project.estate.workflow;

import com.project.estate.entity.WorkflowHistory;
import com.project.estate.enums.WorkflowHistoryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowHistoryTest {

    @Test
    @DisplayName("Should create successful WorkflowHistory record correctly")
    void createSuccessWorkflowHistory() {
        WorkflowHistory history = WorkflowHistory.builder()
                .workflowInstanceId("wf-inst-001")
                .action("CREATE")
                .step("create-reservation")
                .previousStatus("NONE")
                .newStatus("ACTIVE")
                .performedBy("customer-user-id")
                .status(WorkflowHistoryStatus.SUCCESS)
                .build();

        assertEquals("wf-inst-001", history.getWorkflowInstanceId());
        assertEquals("CREATE", history.getAction());
        assertEquals("create-reservation", history.getStep());
        assertEquals("NONE", history.getPreviousStatus());
        assertEquals("ACTIVE", history.getNewStatus());
        assertEquals("customer-user-id", history.getPerformedBy());
        assertEquals(WorkflowHistoryStatus.SUCCESS, history.getStatus());
        assertNull(history.getErrorMessage());
    }

    @Test
    @DisplayName("Should create failed WorkflowHistory record with error message")
    void createFailedWorkflowHistory() {
        WorkflowHistory history = WorkflowHistory.builder()
                .workflowInstanceId("wf-inst-001")
                .action("CANCEL")
                .step("cancel-reservation")
                .previousStatus("ACTIVE")
                .newStatus("ACTIVE")
                .performedBy("customer-user-id")
                .status(WorkflowHistoryStatus.FAILED)
                .errorMessage("Property is already sold")
                .build();

        assertEquals(WorkflowHistoryStatus.FAILED, history.getStatus());
        assertEquals("Property is already sold", history.getErrorMessage());
    }
}
