package com.example.employeeapi.employee;

import com.example.employeeapi.crypto.LocalKeyEncryptionProvider;
import com.example.employeeapi.crypto.SalaryEncryptionService;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MongoEmployeeStoreTest {
    private final FakeMongoOperations mongo = new FakeMongoOperations();
    private final MongoEmployeeStore store = new MongoEmployeeStore(
            mongo.proxy(),
            new SalaryEncryptionService(new LocalKeyEncryptionProvider("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")));

    @Test
    void persistsEncryptedEmployeeDocumentsAndMaintainsSequenceOrder() {
        Employee alice = store.create("Alice", new BigDecimal("50000.00"));
        Employee bob = store.create("Bob", new BigDecimal("40000.00"));

        assertEquals(1, alice.id());
        assertEquals(2, bob.id());
        assertEquals(List.of(alice, bob), store.findAll());
        assertEquals(alice, store.findById(alice.id()));
        assertTrue(mongo.documents.get(1).encryptedSalary().startsWith("v1."));
    }

    @Test
    void updatesAndDeletesExistingEmployeesOnly() {
        Employee employee = store.create("Alice", BigDecimal.TEN);

        assertEquals(new Employee(employee.id(), "Alicia", new BigDecimal("20")),
                store.update(employee.id(), "Alicia", new BigDecimal("20")));
        assertNull(store.update(99, "Nobody", BigDecimal.ONE));
        assertTrue(store.delete(employee.id()));
        assertFalse(store.delete(99));
        assertNull(store.findById(employee.id()));
    }

    private static final class FakeMongoOperations implements InvocationHandler {
        private final Map<Integer, MongoEmployeeDocument> documents = new ConcurrentHashMap<>();
        private final AtomicInteger sequence = new AtomicInteger();

        private MongoOperations proxy() {
            return (MongoOperations) Proxy.newProxyInstance(
                    MongoOperations.class.getClassLoader(),
                    new Class<?>[]{MongoOperations.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return switch (method.getName()) {
                case "findAll" -> new ArrayList<>(documents.values());
                case "findById" -> documents.get((Integer) args[0]);
                case "findAndModify" -> new Document("_id", "employees")
                        .append("sequence", sequence.incrementAndGet());
                case "insert", "save" -> save((MongoEmployeeDocument) args[0]);
                case "findAndRemove" -> documents.remove(queryId((Query) args[0]));
                case "toString" -> "FakeMongoOperations";
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private MongoEmployeeDocument save(MongoEmployeeDocument document) {
            documents.put(document.id(), document);
            return document;
        }

        private int queryId(Query query) {
            return query.getQueryObject().getInteger("_id");
        }
    }
}
