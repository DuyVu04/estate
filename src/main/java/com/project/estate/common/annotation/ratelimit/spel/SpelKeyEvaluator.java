package com.project.estate.common.annotation.ratelimit.spel;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@RequiredArgsConstructor
public class SpelKeyEvaluator {

    private final ApplicationContext applicationContext;

    private final ExpressionParser parser = new SpelExpressionParser();

    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    public String evaluate(Method method, Object[] args, String expression) {
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(null, method, args, discoverer);

        context.setBeanResolver(new BeanFactoryResolver(applicationContext));

        return parser
                .parseExpression(expression)
                .getValue(context, String.class);
    }
}