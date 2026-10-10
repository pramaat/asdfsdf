package com.example.employeeapi.employee;

import java.math.BigDecimal;
import java.util.List;

public interface EmployeeStore {
    List<Employee> findAll();

    Employee findById(int id);

    Employee create(String name, BigDecimal salary);

    Employee update(int id, String name, BigDecimal salary);

    boolean delete(int id);
}
