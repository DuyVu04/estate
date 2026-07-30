package com.project.estate.workflow.definition;

import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.ReservationActor;
import com.project.estate.enums.ReservationStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry managing all loaded Workflow Definitions in the application.
 */
@Component
public class WorkflowDefinitionRegistry {

    public static final String RESERVATION_WORKFLOW = "reservation-workflow";

    private final Map<String, WorkflowDefinition> definitions = new ConcurrentHashMap<>();

    public WorkflowDefinitionRegistry() {
        registerDefaultWorkflows();
    }

    public void register(WorkflowDefinition definition) {
        definitions.put(definition.getWorkflowName(), definition);
    }

    public Optional<WorkflowDefinition> getDefinition(String workflowName) {
        return Optional.ofNullable(definitions.get(workflowName));
    }

    private void registerDefaultWorkflows() {
        WorkflowDefinition reservationWorkflow = WorkflowDefinition.builder()
                .workflowName(RESERVATION_WORKFLOW)
                .description("Estate Reservation Workflow Process")
                .step(WorkflowStepDefinition.builder()
                        .stepName("create-reservation")
                        .action(ReservationAction.CREATE)
                        .fromStatus(null)
                        .toStatus(ReservationStatus.ACTIVE)
                        .allowedActors(EnumSet.of(ReservationActor.CUSTOMER))
                        .dependencies(Collections.emptyList())
                        .build())
                .step(WorkflowStepDefinition.builder()
                        .stepName("cancel-reservation")
                        .action(ReservationAction.CANCEL)
                        .fromStatus(ReservationStatus.ACTIVE)
                        .toStatus(ReservationStatus.CANCELLED)
                        .allowedActors(EnumSet.of(ReservationActor.CUSTOMER, ReservationActor.ADMIN))
                        .dependencies(Collections.singletonList("create-reservation"))
                        .build())
                .step(WorkflowStepDefinition.builder()
                        .stepName("complete-reservation")
                        .action(ReservationAction.COMPLETE)
                        .fromStatus(ReservationStatus.ACTIVE)
                        .toStatus(ReservationStatus.COMPLETED)
                        .allowedActors(EnumSet.of(ReservationActor.ADMIN))
                        .dependencies(Collections.singletonList("create-reservation"))
                        .build())
                .step(WorkflowStepDefinition.builder()
                        .stepName("expire-reservation")
                        .action(ReservationAction.EXPIRE)
                        .fromStatus(ReservationStatus.ACTIVE)
                        .toStatus(ReservationStatus.EXPIRED)
                        .allowedActors(EnumSet.of(ReservationActor.SYSTEM))
                        .dependencies(Collections.singletonList("create-reservation"))
                        .build())
                .build();

        register(reservationWorkflow);
    }
}
