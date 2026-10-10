package com.example.employeeapi.employee;

import com.example.employeeapi.crypto.SalaryEncryptionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.employee-store", havingValue = "postgres", matchIfMissing = true)
public class JdbcEmployeeStore implements EmployeeStore {
    private final JdbcTemplate jdbcTemplate;
    private final SalaryEncryptionService salaryEncryptionService;

    public JdbcEmployeeStore(JdbcTemplate jdbcTemplate, SalaryEncryptionService salaryEncryptionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.salaryEncryptionService = salaryEncryptionService;
    }

    @Override
    public List<Employee> findAll() {
        return jdbcTemplate.query("SELECT id, name, encrypted_salary FROM employees ORDER BY id",
                (resultSet, rowNumber) -> readEmployee(resultSet.getInt("id"), resultSet.getString("name"),
                        resultSet.getString("encrypted_salary")));
    }

    @Override
    public Employee findById(int id) {
        return jdbcTemplate.query("SELECT id, name, encrypted_salary FROM employees WHERE id = ?",
                        (resultSet, rowNumber) -> readEmployee(resultSet.getInt("id"), resultSet.getString("name"),
                                resultSet.getString("encrypted_salary")),
                        id)
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public Employee create(String name, BigDecimal salary) {
        String encryptedSalary = salaryEncryptionService.encrypt(salary);
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO employees (name, encrypted_salary) VALUES (?, ?)", new String[]{"id"});
            statement.setString(1, name);
            statement.setString(2, encryptedSalary);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Database did not return an employee ID");
        }
        return new Employee(key.intValue(), name, salary);
    }

    @Override
    public Employee update(int id, String name, BigDecimal salary) {
        int updated = jdbcTemplate.update("UPDATE employees SET name = ?, encrypted_salary = ? WHERE id = ?",
                name, salaryEncryptionService.encrypt(salary), id);
        return updated == 0 ? null : new Employee(id, name, salary);
    }

    @Override
    public boolean delete(int id) {
        return jdbcTemplate.update("DELETE FROM employees WHERE id = ?", id) > 0;
    }

    private Employee readEmployee(int id, String name, String encryptedSalary) {
        return new Employee(id, name, salaryEncryptionService.decrypt(encryptedSalary));
    }
}
