package com.example.employeeapi.employee;

public interface EmployeeEventPublisher {
    void publish(EmployeeEvent event);
}
