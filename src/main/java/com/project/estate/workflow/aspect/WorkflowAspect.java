package com.project.estate.workflow.aspect;

import com.project.estate.workflow.annotation.WorkflowEngine;
import com.project.estate.workflow.context.WorkflowContext;
import com.project.estate.workflow.factory.WorkflowStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Clean Aspect intercepting methods marked with @WorkflowEngine.
 * Delegates execution lifecycle directly to WorkflowStrategy.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class WorkflowAspect {

    private final WorkflowStrategyFactory workflowStrategyFactory;

    @Before(value = "@annotation(workflowEngine)", argNames = "joinPoint,workflowEngine")
    public void beforeProceedWorkflow(JoinPoint joinPoint, WorkflowEngine workflowEngine) {
        var stepName = workflowEngine.step();
        var action = workflowEngine.action();
        var workflowName = workflowEngine.workflowName();
        var targetIdSpel = workflowEngine.targetIdSpel();

        workflowStrategyFactory.getStrategy(action)
                .beforeProcess(joinPoint, stepName, workflowName, targetIdSpel);
    }

    @AfterReturning(value = "@annotation(workflowEngine)", returning = "result", argNames = "joinPoint,result,workflowEngine")
    public void afterReturnWorkflow(JoinPoint joinPoint, Object result, WorkflowEngine workflowEngine) {
        var action = workflowEngine.action();
        workflowStrategyFactory.getStrategy(action).afterProcess(result);
    }

    @AfterThrowing(value = "@annotation(workflowEngine)", throwing = "ex", argNames = "joinPoint,ex,workflowEngine")
    public void afterThrowWorkflow(JoinPoint joinPoint, Throwable ex, WorkflowEngine workflowEngine) {
        var action = workflowEngine.action();
        workflowStrategyFactory.getStrategy(action).afterThrowProcess(ex);
    }

    @After(value = "@annotation(workflowEngine)")
    public void afterWorkflow(WorkflowEngine workflowEngine) {
        WorkflowContext.clear();
    }
}
