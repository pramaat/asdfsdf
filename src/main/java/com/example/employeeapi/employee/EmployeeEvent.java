package com.example.employeeapi.employee;

import java.time.Instant;

public record EmployeeEvent(
        String eventType,
        int employeeId,
        Instant occurredAt
) {
}
