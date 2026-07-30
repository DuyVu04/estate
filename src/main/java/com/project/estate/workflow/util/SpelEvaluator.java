package com.project.estate.workflow.util;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Utility Component responsible for evaluating Spring SpEL expressions to extract target entity IDs.
 */
@Slf4j
@Component
public class SpelEvaluator {

    private final ExpressionParser spelParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public String parseTargetId(JoinPoint joinPoint, String spel, Object result) {
        if (spel == null || spel.isBlank()) {
            return null;
        }

        try {
            EvaluationContext evalContext = new StandardEvaluationContext();
            if (joinPoint != null) {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Method method = signature.getMethod();
                Object[] args = joinPoint.getArgs();
                String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
                if (paramNames != null && args != null) {
                    for (int i = 0; i < paramNames.length; i++) {
                        evalContext.setVariable(paramNames[i], args[i]);
                    }
                }
            }
            if (result != null) {
                evalContext.setVariable("result", result);
            }
            Object value = spelParser.parseExpression(spel).getValue(evalContext);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.debug("[SPEL_EVALUATOR] SpEL evaluation for '{}' returned null: {}", spel, e.getMessage());
            return null;
        }
    }
}
