package com.project.estate.workflow.statemachine;

import com.project.estate.enums.ErrorCode;
import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.ReservationActor;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.exception.AppException;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * State Machine for Reservation Workflow. Manages valid state transitions and actor authorization.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReservationStateMachine {

  @Value
  public static class TransitionRule {
    ReservationStatus currentStatus;
    ReservationAction action;
    ReservationStatus nextStatus;
    Set<ReservationActor> allowedActors;
  }

  private static final List<TransitionRule> TRANSITION_RULES =
      Arrays.asList(
          // CREATE: NONE (null) -> ACTIVE by CUSTOMER
          new TransitionRule(
              null,
              ReservationAction.CREATE,
              ReservationStatus.ACTIVE,
              EnumSet.of(ReservationActor.CUSTOMER)),

          // CANCEL: ACTIVE or DEPOSIT_PAID -> CANCELLED by CUSTOMER or ADMIN
          new TransitionRule(
              ReservationStatus.ACTIVE,
              ReservationAction.CANCEL,
              ReservationStatus.CANCELLED,
              EnumSet.of(ReservationActor.CUSTOMER, ReservationActor.ADMIN)),
          new TransitionRule(
              ReservationStatus.DEPOSIT_PAID,
              ReservationAction.CANCEL,
              ReservationStatus.CANCELLED,
              EnumSet.of(ReservationActor.CUSTOMER, ReservationActor.ADMIN)),

          // COMPLETE: DEPOSIT_PAID or ACTIVE -> COMPLETED by ADMIN
          new TransitionRule(
              ReservationStatus.DEPOSIT_PAID,
              ReservationAction.COMPLETE,
              ReservationStatus.COMPLETED,
              EnumSet.of(ReservationActor.ADMIN, ReservationActor.SYSTEM)),
          new TransitionRule(
              ReservationStatus.ACTIVE,
              ReservationAction.COMPLETE,
              ReservationStatus.COMPLETED,
              EnumSet.of(ReservationActor.ADMIN, ReservationActor.SYSTEM)),

          // EXPIRE: ACTIVE -> EXPIRED by SYSTEM
          new TransitionRule(
              ReservationStatus.ACTIVE,
              ReservationAction.EXPIRE,
              ReservationStatus.EXPIRED,
              EnumSet.of(ReservationActor.SYSTEM)),

          // PAY DEPOSIT: ACTIVE -> DEPOSIT_PAID by CUSTOMER
          new TransitionRule(
              ReservationStatus.ACTIVE,
              ReservationAction.PAY_DEPOSIT,
              ReservationStatus.DEPOSIT_PAID,
              EnumSet.of(ReservationActor.CUSTOMER)));

  /** Finds transition rule for given currentStatus and action. */
  public static Optional<TransitionRule> findRule(
      ReservationStatus currentStatus, ReservationAction action) {
    return TRANSITION_RULES.stream()
        .filter(
            rule ->
                Objects.equals(rule.getCurrentStatus(), currentStatus)
                    && rule.getAction() == action)
        .findFirst();
  }

  /**
   * Validates transition and returns the next status. Throws AppException if transition is invalid
   * or actor is unauthorized.
   */
  public static ReservationStatus getNextState(
      ReservationStatus currentStatus, ReservationAction action, ReservationActor actor) {
    TransitionRule rule =
        findRule(currentStatus, action)
            .orElseThrow(() -> new AppException(ErrorCode.INVALID_STATE_TRANSITION));

    if (!rule.getAllowedActors().contains(actor)) {
      throw new AppException(ErrorCode.UNAUTHORIZED_WORKFLOW_ACTOR);
    }

    return rule.getNextStatus();
  }

  /** Checks if a transition is valid for the given actor. */
  public static boolean isValidTransition(
      ReservationStatus currentStatus, ReservationAction action, ReservationActor actor) {
    Optional<TransitionRule> ruleOpt = findRule(currentStatus, action);
    return ruleOpt.isPresent() && ruleOpt.get().getAllowedActors().contains(actor);
  }
}
