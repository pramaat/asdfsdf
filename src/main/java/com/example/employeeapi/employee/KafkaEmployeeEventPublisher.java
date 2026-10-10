package com.example.employeeapi.employee;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEmployeeEventPublisher implements EmployeeEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(KafkaEmployeeEventPublisher.class);

    private final KafkaTemplate<String, EmployeeEvent> kafkaTemplate;
    private final String topic;

    public KafkaEmployeeEventPublisher(
            KafkaTemplate<String, EmployeeEvent> kafkaTemplate,
            @Value("${app.kafka.employee-events-topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(EmployeeEvent event) {
        try {
            kafkaTemplate.send(topic, Integer.toString(event.employeeId()), event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            logFailure(event, exception);
                        }
                    });
        } catch (KafkaException exception) {
            logFailure(event, exception);
        }
    }

    private void logFailure(EmployeeEvent event, Throwable exception) {
        logger.error("Failed to publish {} for employee {} to Kafka topic {}",
                event.eventType(), event.employeeId(), topic, exception);
    }
}
