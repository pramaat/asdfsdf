package com.example.employeeapi.employee;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class KafkaEmployeeEventPublisherTest {
    private final EmployeeEvent event =
            new EmployeeEvent("EMPLOYEE_CREATED", 17, Instant.now());

    @Test
    void publishesEmployeeEventsWithEmployeeIdAsKey() {
        RecordingKafkaTemplate kafkaTemplate = new RecordingKafkaTemplate(CompletableFuture.completedFuture(null));
        KafkaEmployeeEventPublisher publisher = new KafkaEmployeeEventPublisher(kafkaTemplate, "employee-events");

        publisher.publish(event);

        assertEquals("employee-events", kafkaTemplate.record.topic());
        assertEquals("17", kafkaTemplate.record.key());
        assertEquals(event, kafkaTemplate.record.value());
    }

    @Test
    void reportsAsynchronousKafkaFailuresWithoutThrowingToTheRequest() {
        RecordingKafkaTemplate kafkaTemplate = new RecordingKafkaTemplate(
                CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));
        KafkaEmployeeEventPublisher publisher = new KafkaEmployeeEventPublisher(kafkaTemplate, "employee-events");

        assertDoesNotThrow(() -> publisher.publish(event));
    }

    @Test
    void reportsSynchronousKafkaFailuresWithoutThrowingToTheRequest() {
        RecordingKafkaTemplate kafkaTemplate = new RecordingKafkaTemplate(null,
                new KafkaException("producer unavailable"));
        KafkaEmployeeEventPublisher publisher = new KafkaEmployeeEventPublisher(kafkaTemplate, "employee-events");

        assertDoesNotThrow(() -> publisher.publish(event));
    }

    private static final class RecordingKafkaTemplate extends KafkaTemplate<String, EmployeeEvent> {
        private final CompletableFuture<SendResult<String, EmployeeEvent>> result;
        private final KafkaException sendFailure;
        private ProducerRecord<String, EmployeeEvent> record;

        private RecordingKafkaTemplate(CompletableFuture<SendResult<String, EmployeeEvent>> result) {
            this(result, null);
        }

        private RecordingKafkaTemplate(
                CompletableFuture<SendResult<String, EmployeeEvent>> result,
                KafkaException sendFailure
        ) {
            super(new DefaultKafkaProducerFactory<>(Map.of()));
            this.result = result;
            this.sendFailure = sendFailure;
        }

        @Override
        public CompletableFuture<SendResult<String, EmployeeEvent>> send(
                String topic,
                String key,
                EmployeeEvent value
        ) {
            if (sendFailure != null) {
                throw sendFailure;
            }
            record = new ProducerRecord<>(topic, key, value);
            return result;
        }
    }
}
