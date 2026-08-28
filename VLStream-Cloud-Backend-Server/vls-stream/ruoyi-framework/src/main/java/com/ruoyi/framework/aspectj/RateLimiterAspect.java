/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.aspectj;

import cn.hutool.core.util.ArrayUtil;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RateType;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Process
 *
 * @author Lion Li
 */
@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    /**
     * spel Parse
     */
    private final ExpressionParser parser = new SpelExpressionParser();
    /**
     * spelParse
     */
    private final ParserContext parserContext = new TemplateParserContext();
    /**
     * spel object Parse
     */
    private final EvaluationContext context = new StandardEvaluationContext();
    /**
     * method parameterParse
     */
    private final ParameterNameDiscoverer pnd = new DefaultParameterNameDiscoverer();

    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) throws Throwable {
        int time = rateLimiter.time();
        int count = rateLimiter.count();
        String combineKey = getCombineKey(rateLimiter, point);
        try {
            RateType rateType = RateType.OVERALL;
            if (rateLimiter.limitType() == LimitType.CLUSTER) {
                rateType = RateType.PER_CLIENT;
            }
            long number = RedisUtils.rateLimiter(combineKey, rateType, count, time);
            if (number == -1) {
                String message = rateLimiter.message();
                if (StringUtils.startsWith(message, "{") && StringUtils.endsWith(message, "}")) {
                    message = MessageUtils.message(StringUtils.substring(message, 1, message.length() - 1));
                }
                throw new ServiceException(message);
            }
            log.info("限制令牌 => {}, 剩余令牌 => {}, 缓存key => '{}'", count, number, combineKey);
        } catch (Exception e) {
            if (e instanceof ServiceException) {
                throw e;
            } else {
                throw new RuntimeException("服务器限流异常，请稍候再试");
            }
        }
    }

    public String getCombineKey(RateLimiter rateLimiter, JoinPoint point) {
        String key = rateLimiter.key();
        // Get method ( method Get )
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        // Check whether is spel
        if (StringUtils.containsAny(key, "#")) {
            // Get parameter value
            Object[] args = point.getArgs();
            // Get method parameter
            String[] parameterNames = pnd.getParameterNames(method);
            if (ArrayUtil.isEmpty(parameterNames)) {
                throw new ServiceException("限流key解析异常!请联系管理员!");
            }
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
            // Parse key
            try {
                Expression expression;
                if (StringUtils.startsWith(key, parserContext.getExpressionPrefix())
                    && StringUtils.endsWith(key, parserContext.getExpressionSuffix())) {
                    expression = parser.parseExpression(key, parserContext);
                } else {
                    expression = parser.parseExpression(key);
                }
                key = expression.getValue(context, String.class) + ":";
            } catch (Exception e) {
                throw new ServiceException("限流key解析异常!请联系管理员!");
            }
        }
        StringBuilder stringBuffer = new StringBuilder(CacheConstants.RATE_LIMIT_KEY);
        stringBuffer.append(ServletUtils.getRequest().getRequestURI()).append(":");
        if (rateLimiter.limitType() == LimitType.IP) {
            // Get ip
            stringBuffer.append(ServletUtils.getClientIP()).append(":");
        } else if (rateLimiter.limitType() == LimitType.CLUSTER) {
            // Get instanceid
            stringBuffer.append(RedisUtils.getClient().getId()).append(":");
        }
        return stringBuffer.append(key).toString();
    }
}
