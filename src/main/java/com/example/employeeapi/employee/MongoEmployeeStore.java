package com.example.employeeapi.employee;

import com.example.employeeapi.crypto.SalaryEncryptionService;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.employee-store", havingValue = "mongo")
public class MongoEmployeeStore implements EmployeeStore {
    private static final String COUNTERS_COLLECTION = "employee_counters";
    private static final String EMPLOYEE_SEQUENCE = "employees";

    private final MongoOperations mongoTemplate;
    private final SalaryEncryptionService salaryEncryptionService;

    public MongoEmployeeStore(MongoOperations mongoTemplate, SalaryEncryptionService salaryEncryptionService) {
        this.mongoTemplate = mongoTemplate;
        this.salaryEncryptionService = salaryEncryptionService;
    }

    @Override
    public List<Employee> findAll() {
        return mongoTemplate.findAll(MongoEmployeeDocument.class).stream()
                .map(this::toEmployee)
                .sorted(Comparator.comparingInt(Employee::id))
                .toList();
    }

    @Override
    public Employee findById(int id) {
        MongoEmployeeDocument document = mongoTemplate.findById(id, MongoEmployeeDocument.class);
        return document == null ? null : toEmployee(document);
    }

    @Override
    public Employee create(String name, BigDecimal salary) {
        Document counter = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(EMPLOYEE_SEQUENCE)),
                new Update().inc("sequence", 1),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                Document.class,
                COUNTERS_COLLECTION
        );
        if (counter == null || counter.getInteger("sequence") == null) {
            throw new IllegalStateException("MongoDB did not return an employee ID");
        }
        int id = counter.getInteger("sequence");
        mongoTemplate.insert(new MongoEmployeeDocument(id, name, salaryEncryptionService.encrypt(salary)));
        return new Employee(id, name, salary);
    }

    @Override
    public Employee update(int id, String name, BigDecimal salary) {
        MongoEmployeeDocument existing = mongoTemplate.findById(id, MongoEmployeeDocument.class);
        if (existing == null) {
            return null;
        }
        mongoTemplate.save(new MongoEmployeeDocument(id, name, salaryEncryptionService.encrypt(salary)));
        return new Employee(id, name, salary);
    }

    @Override
    public boolean delete(int id) {
        MongoEmployeeDocument existing = mongoTemplate.findAndRemove(
                Query.query(Criteria.where("_id").is(id)), MongoEmployeeDocument.class);
        return existing != null;
    }

    private Employee toEmployee(MongoEmployeeDocument document) {
        return new Employee(document.id(), document.name(),
                salaryEncryptionService.decrypt(document.encryptedSalary()));
    }
}
