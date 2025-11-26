package com.example.wallet.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TransferConsumer {

    @KafkaListener(topics = "transfer-notifications", groupId = "wallet-notification-group")
    public void sendSMS(TransferCompletedEvent event) {
        log.info("===============================================================");
        log.info("Start processing transfer {}", event.getTransferId());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("SMS sent for transfer {}", event.getTransferId());
        log.info("===============================================================");

    }
}