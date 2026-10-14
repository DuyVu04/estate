package com.project.estate.modules.reservation.workflow.factory;

import com.project.estate.modules.reservation.enums.ReservationAction;
import com.project.estate.modules.reservation.workflow.strategy.WorkflowStrategy;
import com.project.estate.shared.enums.ErrorCode;
import com.project.estate.shared.exception.AppException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Factory class responsible for resolving the appropriate WorkflowStrategy for a given
 * ReservationAction. Uses Map-based resolution powered by Spring Dependency Injection to eliminate
 * if-else chains.
 */
@Component
public class WorkflowStrategyFactory {

  private final Map<ReservationAction, WorkflowStrategy> strategyMap;

  public WorkflowStrategyFactory(List<WorkflowStrategy> strategies) {
    this.strategyMap =
        strategies.stream()
            .collect(
                Collectors.toMap(
                    WorkflowStrategy::getAction,
                    Function.identity(),
                    (existing, replacement) -> existing));
  }

  public WorkflowStrategy getStrategy(ReservationAction action) {
    return Optional.ofNullable(strategyMap.get(action))
        .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
  }
}
