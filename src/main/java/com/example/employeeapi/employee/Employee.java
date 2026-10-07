package com.example.employeeapi.employee;

import java.math.BigDecimal;

public record Employee(int id, String name, BigDecimal salary) {
}
