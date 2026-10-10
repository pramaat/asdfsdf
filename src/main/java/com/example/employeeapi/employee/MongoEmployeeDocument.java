package com.example.employeeapi.employee;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("employees")
public record MongoEmployeeDocument(
        @Id int id,
        String name,
        String encryptedSalary
) {
}
