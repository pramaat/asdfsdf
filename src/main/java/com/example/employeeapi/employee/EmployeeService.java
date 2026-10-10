package com.example.employeeapi.employee;

import com.example.employeeapi.aop.AuditedOperation;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class EmployeeService {
    private final EmployeeStore employeeStore;
    private final EmployeeEventPublisher eventPublisher;

    public EmployeeService(EmployeeStore employeeStore, EmployeeEventPublisher eventPublisher) {
        this.employeeStore = employeeStore;
        this.eventPublisher = eventPublisher;
    }

    @AuditedOperation("employee.list")
    public List<Employee> findAll() {
        return employeeStore.findAll();
    }

    @AuditedOperation("employee.read")
    public Employee findById(int id) {
        return employeeStore.findById(id);
    }

    @AuditedOperation("employee.create")
    public Employee create(EmployeeRequest request) {
        Employee employee = employeeStore.create(request.name(), request.salary());
        publishEvent("EMPLOYEE_CREATED", employee.id());
        return employee;
    }

    @AuditedOperation("employee.update")
    public Employee update(int id, EmployeeRequest request) {
        Employee employee = employeeStore.update(id, request.name(), request.salary());
        if (employee == null) {
            return null;
        }
        publishEvent("EMPLOYEE_UPDATED", employee.id());
        return employee;
    }

    @AuditedOperation("employee.delete")
    public boolean delete(int id) {
        if (!employeeStore.delete(id)) {
            return false;
        }
        publishEvent("EMPLOYEE_DELETED", id);
        return true;
    }

    private void publishEvent(String eventType, int employeeId) {
        eventPublisher.publish(new EmployeeEvent(
                eventType,
                employeeId,
                Instant.now()
        ));
    }
}
