package com.example.employeeapi.employee;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeServiceTest {
    private final List<EmployeeEvent> publishedEvents = new ArrayList<>();
    private final EmployeeEventPublisher eventPublisher = publishedEvents::add;
    private final EmployeeService employeeService = new EmployeeService(new TestEmployeeStore(), eventPublisher);

    @Test
    void publishesCreateUpdateAndDeleteEvents() {
        Employee created = employeeService.create(new EmployeeRequest("Alice", new BigDecimal("50000")));
        Employee updated = employeeService.update(created.id(), new EmployeeRequest("Alicia", new BigDecimal("60000")));
        boolean deleted = employeeService.delete(created.id());

        assertEquals("Alice", created.name());
        assertEquals("Alicia", updated.name());
        assertTrue(deleted);

        assertEquals(List.of("EMPLOYEE_CREATED", "EMPLOYEE_UPDATED", "EMPLOYEE_DELETED"),
                publishedEvents.stream().map(EmployeeEvent::eventType).toList());
        assertEquals(created.id(), publishedEvents.get(2).employeeId());
        assertTrue(publishedEvents.stream().allMatch(event -> event.occurredAt() != null));
    }

    @Test
    void returnsEmployeesInIdOrderAndDoesNotPublishForMissingChanges() {
        Employee first = employeeService.create(new EmployeeRequest("Alice", BigDecimal.TEN));
        Employee second = employeeService.create(new EmployeeRequest("Bob", BigDecimal.ONE));
        List<Employee> employees = employeeService.findAll();

        assertEquals(List.of(first, second), employees);
        assertEquals(first, employeeService.findById(first.id()));
        assertNull(employeeService.findById(99));
        assertNull(employeeService.update(99, new EmployeeRequest("Nobody", BigDecimal.ONE)));
        assertFalse(employeeService.delete(99));

        assertEquals(List.of("EMPLOYEE_CREATED", "EMPLOYEE_CREATED"),
                publishedEvents.stream().map(EmployeeEvent::eventType).toList());
    }

    private static final class TestEmployeeStore implements EmployeeStore {
        private final Map<Integer, Employee> employees = new ConcurrentHashMap<>();
        private final AtomicInteger nextId = new AtomicInteger(1);

        @Override
        public List<Employee> findAll() {
            return employees.values().stream().sorted(Comparator.comparingInt(Employee::id)).toList();
        }

        @Override
        public Employee findById(int id) {
            return employees.get(id);
        }

        @Override
        public Employee create(String name, BigDecimal salary) {
            int id = nextId.getAndIncrement();
            Employee employee = new Employee(id, name, salary);
            employees.put(id, employee);
            return employee;
        }

        @Override
        public Employee update(int id, String name, BigDecimal salary) {
            if (!employees.containsKey(id)) {
                return null;
            }
            Employee updated = new Employee(id, name, salary);
            employees.put(id, updated);
            return updated;
        }

        @Override
        public boolean delete(int id) {
            return employees.remove(id) != null;
        }
    }
}
