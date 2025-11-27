package com.example.wallet.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferProducer {

    private static final String TOPIC = "transfer-notifications";

    private final KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate;

    public void sendTransferEvent(TransferCompletedEvent event) {
        log.info("Sending Kafka event for transfer: {}", event.getTransferId());
        kafkaTemplate.send(TOPIC, event.getTransferId().toString(), event);
    }
}