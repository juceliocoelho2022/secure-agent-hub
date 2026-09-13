package br.com.jucelio.secureagent.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class DomainEventService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public DomainEventService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void append(UUID aggregateId, String eventType, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            repository.save(new OutboxEvent("AgentExecution", aggregateId, eventType, json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize domain event payload", e);
        }
    }
}
