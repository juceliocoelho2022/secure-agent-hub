package br.com.jucelio.secureagent.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DomainEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumer.class);
    private static final String CONSUMER_NAME = "secure-agent-domain-events";

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final String failureInjectionEventType;

    public DomainEventConsumer(ObjectMapper objectMapper,
                               ProcessedEventRepository processedEventRepository,
                               @Value("${app.events.failure-injection-event-type:}") String failureInjectionEventType) {
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
        this.failureInjectionEventType = failureInjectionEventType;
    }

    @KafkaListener(
            topics = "${app.events.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consume(String message) throws JsonProcessingException {
        DomainEventEnvelope envelope = objectMapper.readValue(message, DomainEventEnvelope.class);

        if (processedEventRepository.existsById(envelope.eventId())) {
            log.info("Duplicate event ignored eventId={} eventType={}", envelope.eventId(), envelope.eventType());
            return;
        }

        if (!failureInjectionEventType.isBlank()
                && failureInjectionEventType.equals(envelope.eventType())) {
            log.warn(
                    "Controlled failure injection triggered eventId={} eventType={} correlationId={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.correlationId()
            );
            throw new IllegalStateException("Controlled Kafka consumer failure for DLT validation");
        }

        log.info(
                "Processing domain event eventId={} eventType={} aggregateId={} correlationId={}",
                envelope.eventId(),
                envelope.eventType(),
                envelope.aggregateId(),
                envelope.correlationId()
        );

        processedEventRepository.save(new ProcessedEvent(
                envelope.eventId(),
                CONSUMER_NAME,
                envelope.eventType()
        ));
    }
}
