package br.com.jucelio.secureagent.operations;

import br.com.jucelio.secureagent.event.OutboxEventRepository;
import br.com.jucelio.secureagent.event.ProcessedEventRepository;
import org.springframework.stereotype.Service;

@Service
public class PipelineHealthService {
    private final OutboxEventRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;

    public PipelineHealthService(OutboxEventRepository outboxRepository,
                                 ProcessedEventRepository processedEventRepository) {
        this.outboxRepository = outboxRepository;
        this.processedEventRepository = processedEventRepository;
    }

    public PipelineHealthResponse snapshot() {
        long pendingOutbox = outboxRepository.countByPublishedAtIsNull();
        long processedEvents = processedEventRepository.count();

        return new PipelineHealthResponse(
                pendingOutbox,
                processedEvents,
                null,
                pendingOutbox == 0 ? "HEALTHY" : "BACKLOG",
                "n/a",
                processedEvents > 0 ? "ACTIVE" : "n/a",
                "n/a"
        );
    }
}
