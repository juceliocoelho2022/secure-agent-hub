package br.com.jucelio.secureagent.operations;

public record PipelineHealthResponse(
        long pendingOutbox,
        long processedEvents,
        Long deadLetterEvents,
        String outboxStatus,
        String kafkaStatus,
        String consumerStatus,
        String dltStatus
) {}
