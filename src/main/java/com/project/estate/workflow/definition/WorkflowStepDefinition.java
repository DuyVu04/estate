package com.project.estate.workflow.definition;

import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.ReservationActor;
import com.project.estate.enums.ReservationStatus;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

/**
 * Represents the definition (blueprint) of a single workflow step.
 */
@Value
@Builder
public class WorkflowStepDefinition {
    String stepName;
    ReservationAction action;
    ReservationStatus fromStatus;
    ReservationStatus toStatus;
    Set<ReservationActor> allowedActors;
    List<String> dependencies;
}
