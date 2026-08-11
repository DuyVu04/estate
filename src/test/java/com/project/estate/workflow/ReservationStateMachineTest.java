package com.project.estate.workflow;

import com.project.estate.enums.ErrorCode;
import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.ReservationActor;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.exception.AppException;
import com.project.estate.workflow.statemachine.ReservationStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class ReservationStateMachineTest {

    @Nested
    @DisplayName("Valid Transitions Test")
    class ValidTransitions {

        @Test
        @DisplayName("CREATE action from null status by CUSTOMER should transition to ACTIVE")
        void createReservation_Success() {
            ReservationStatus nextState = ReservationStateMachine.getNextState(
                    null, ReservationAction.CREATE, ReservationActor.CUSTOMER
            );
            assertEquals(ReservationStatus.ACTIVE, nextState);
        }

        @Test
        @DisplayName("CANCEL action from ACTIVE status by CUSTOMER should transition to CANCELLED")
        void cancelReservation_ByCustomer_Success() {
            ReservationStatus nextState = ReservationStateMachine.getNextState(
                    ReservationStatus.ACTIVE, ReservationAction.CANCEL, ReservationActor.CUSTOMER
            );
            assertEquals(ReservationStatus.CANCELLED, nextState);
        }

        @Test
        @DisplayName("CANCEL action from ACTIVE status by ADMIN should transition to CANCELLED")
        void cancelReservation_ByAdmin_Success() {
            ReservationStatus nextState = ReservationStateMachine.getNextState(
                    ReservationStatus.ACTIVE, ReservationAction.CANCEL, ReservationActor.ADMIN
            );
            assertEquals(ReservationStatus.CANCELLED, nextState);
        }

        @Test
        @DisplayName("COMPLETE action from ACTIVE status by ADMIN should transition to COMPLETED")
        void completeReservation_Success() {
            ReservationStatus nextState = ReservationStateMachine.getNextState(
                    ReservationStatus.ACTIVE, ReservationAction.COMPLETE, ReservationActor.ADMIN
            );
            assertEquals(ReservationStatus.COMPLETED, nextState);
        }

        @Test
        @DisplayName("EXPIRE action from ACTIVE status by SYSTEM should transition to EXPIRED")
        void expireReservation_Success() {
            ReservationStatus nextState = ReservationStateMachine.getNextState(
                    ReservationStatus.ACTIVE, ReservationAction.EXPIRE, ReservationActor.SYSTEM
            );
            assertEquals(ReservationStatus.EXPIRED, nextState);
        }
    }

    @Nested
    @DisplayName("Invalid Transitions & Exception Tests")
    class InvalidTransitions {

        @Test
        @DisplayName("COMPLETED reservation cannot be CANCELLED")
        void completedToCancelled_ShouldThrowException() {
            AppException ex = assertThrows(AppException.class, () ->
                    ReservationStateMachine.getNextState(
                            ReservationStatus.COMPLETED, ReservationAction.CANCEL, ReservationActor.ADMIN
                    )
            );
            assertEquals(ErrorCode.INVALID_STATE_TRANSITION, ex.getErrorCode());
        }

        @Test
        @DisplayName("CANCELLED reservation cannot be COMPLETED")
        void cancelledToCompleted_ShouldThrowException() {
            AppException ex = assertThrows(AppException.class, () ->
                    ReservationStateMachine.getNextState(
                            ReservationStatus.CANCELLED, ReservationAction.COMPLETE, ReservationActor.ADMIN
                    )
            );
            assertEquals(ErrorCode.INVALID_STATE_TRANSITION, ex.getErrorCode());
        }

        @Test
        @DisplayName("EXPIRED reservation cannot be COMPLETED")
        void expiredToCompleted_ShouldThrowException() {
            AppException ex = assertThrows(AppException.class, () ->
                    ReservationStateMachine.getNextState(
                            ReservationStatus.EXPIRED, ReservationAction.COMPLETE, ReservationActor.ADMIN
                    )
            );
            assertEquals(ErrorCode.INVALID_STATE_TRANSITION, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Actor Authorization Tests")
    class ActorAuthorization {

        @Test
        @DisplayName("CUSTOMER cannot execute COMPLETE action")
        void customerCannotCompleteReservation() {
            AppException ex = assertThrows(AppException.class, () ->
                    ReservationStateMachine.getNextState(
                            ReservationStatus.ACTIVE, ReservationAction.COMPLETE, ReservationActor.CUSTOMER
                    )
            );
            assertEquals(ErrorCode.UNAUTHORIZED_WORKFLOW_ACTOR, ex.getErrorCode());
        }

        @Test
        @DisplayName("CUSTOMER cannot execute EXPIRE action")
        void customerCannotExpireReservation() {
            AppException ex = assertThrows(AppException.class, () ->
                    ReservationStateMachine.getNextState(
                            ReservationStatus.ACTIVE, ReservationAction.EXPIRE, ReservationActor.CUSTOMER
                    )
            );
            assertEquals(ErrorCode.UNAUTHORIZED_WORKFLOW_ACTOR, ex.getErrorCode());
        }

        @Test
        @DisplayName("SYSTEM cannot execute CREATE action")
        void systemCannotCreateReservation() {
            AppException ex = assertThrows(AppException.class, () ->
                    ReservationStateMachine.getNextState(
                            null, ReservationAction.CREATE, ReservationActor.SYSTEM
                    )
            );
            assertEquals(ErrorCode.UNAUTHORIZED_WORKFLOW_ACTOR, ex.getErrorCode());
        }
    }
}
