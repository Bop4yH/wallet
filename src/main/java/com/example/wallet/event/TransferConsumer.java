package com.example.wallet.event;

import com.example.wallet.transfer.TransferService;
import com.example.wallet.transfer.TransferStatus;
import com.example.wallet.transfer.dto.FraudAnalysisResult;
import com.example.wallet.transfer.dto.FraudRiskLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class TransferConsumer {

    private final TransferService transferService;

    private final KafkaTemplate<String, FraudAnalysisResult> kafkaTemplate;

    //TODO реализовать идемпотентность через БД
    @KafkaListener(topics = "transfer-notifications", groupId = "wallet-sms-group")
    public void sendSMS(TransferCompletedEvent event) {
        log.info("Start processing transfer {}", event.getTransferId());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("SMS sent for transfer {}", event.getTransferId());
    }

    @KafkaListener(topics = "transfer-notifications", groupId = "wallet-fraud-analysis-group")
    public void monitorFraud(TransferCompletedEvent event) {
        FraudAnalysisResult result = transferService.analyzeFraud(event);

        if (result.getRiskLevel() == FraudRiskLevel.LOW) {
            return;
        }
        result.setTransferId(event.getTransferId());

        if (result.getRiskLevel() == FraudRiskLevel.MEDIUM) {
            result.setMessage("WARNING: Call client required. Do not cancel yet.");
            result.setTransferStatus(TransferStatus.COMPLETED);

        } else if (result.getRiskLevel() == FraudRiskLevel.HIGH) {
            try {
                log.warn("High fraud risk detected! Cancelling transfer {}", event.getTransferId());
                transferService.cancel(event.getTransferId());

                result.setMessage("CRITICAL: Transfer CANCELLED automatically. Call client.");
                result.setTransferStatus(TransferStatus.CANCELLED);

            } catch (Exception e) {
                result.setMessage("CRITICAL: Fraud detected but CANCEL FAILED! Call client. Error: " + e.getMessage());
                result.setTransferStatus(TransferStatus.COMPLETED);
            }
        }
        kafkaTemplate.send("fraud-alerts", result);

    }
}