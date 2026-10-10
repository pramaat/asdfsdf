package com.example.employeeapi.aop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessOperationAspectTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsSuccessfulOperationsWithoutChangingTheirReturnValue() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("alice@example.com", "n/a", java.util.List.of()));
        OperationFixture proxy = proxiedFixture();

        assertEquals("done", proxy.succeed());
    }

    @Test
    void recordsFailuresAndRethrowsTheOriginalException() {
        OperationFixture proxy = proxiedFixture();

        assertThrows(IllegalStateException.class, proxy::fail);
    }

    private OperationFixture proxiedFixture() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new OperationFixture());
        factory.addAspect(new BusinessOperationAspect());
        return factory.getProxy();
    }

    public static class OperationFixture {
        @AuditedOperation("test.success")
        public String succeed() {
            return "done";
        }

        @AuditedOperation("test.failure")
        public String fail() {
            throw new IllegalStateException("expected test failure");
        }
    }
}
