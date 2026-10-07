package com.example.employeeapi.employee;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EmployeeService {
    private final ConcurrentMap<Integer, Employee> employees = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public List<Employee> findAll() {
        return employees.values().stream()
                .sorted(Comparator.comparingInt(Employee::id))
                .toList();
    }

    public Employee findById(int id) {
        return employees.get(id);
    }

    public Employee create(EmployeeRequest request) {
        int id = nextId.getAndIncrement();
        Employee employee = new Employee(id, request.name(), request.salary());
        employees.put(id, employee);
        return employee;
    }

    public Employee update(int id, EmployeeRequest request) {
        Employee employee = new Employee(id, request.name(), request.salary());
        return employees.replace(id, employee) == null ? null : employee;
    }

    public boolean delete(int id) {
        return employees.remove(id) != null;
    }
}
