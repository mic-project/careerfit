package com.codelab.micproject.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

// com.codelab.micproject.common.logging.RedactAspect
@Aspect
@Component
@Order(1)
public class RedactAspect {
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "card_number","cardNo","pan"
    );

    @Around("execution(* com.codelab.micproject.payment..*(..))")
    public Object redact(ProceedingJoinPoint pjp) throws Throwable {
        // 파라미터/반환 로그를 직접 찍고 있다면, 그 전에 Map/JSON에서 민감키 치환
        // (프로젝트에서 파라미터 로깅 안 하면 생략 가능)
        return pjp.proceed();
    }
}

