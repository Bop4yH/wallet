package com.example.wallet.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferProducer {

    private static final String TOPIC = "transfer-notifications";

    private final KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate;

    @Async
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 10,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 60000)
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendTransferEvent(TransferCompletedEvent event) {
        log.info("Sending Kafka event for transfer: {}", event.getTransferId());
        kafkaTemplate.send(TOPIC, event.getTransferId().toString(), event).join();
    }

    @Recover
    public void recover(Exception e, TransferCompletedEvent event) {
        log.error(
                "FATAL: Сообщение потеряно после всех попыток. TransferId: {}. Ошибка: {}",
                event.getTransferId(), e.getMessage()
        );
        //TODO реализовать отправку в DLQ на БД
    }
}