package com.project.estate.modules.reservation.workflow.definition;

import com.project.estate.modules.reservation.enums.ReservationAction;
import com.project.estate.modules.reservation.enums.ReservationActor;
import com.project.estate.modules.reservation.enums.ReservationStatus;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

/** Represents the definition (blueprint) of a single workflow step. */
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
