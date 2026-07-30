package com.project.estate.workflow.definition;

import com.project.estate.enums.ReservationAction;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.Optional;

/**
 * Represents the blueprint of a Workflow Process.
 * Analogous to a Class definition vs Object instance.
 */
@Getter
@Builder
public class WorkflowDefinition {
    private final String workflowName;
    private final String description;

    @Singular
    private final List<WorkflowStepDefinition> steps;

    public Optional<WorkflowStepDefinition> findStepByName(String stepName) {
        return steps.stream()
                .filter(step -> step.getStepName().equalsIgnoreCase(stepName))
                .findFirst();
    }

    public Optional<WorkflowStepDefinition> findStepByAction(ReservationAction action) {
        return steps.stream()
                .filter(step -> step.getAction() == action)
                .findFirst();
    }
}
