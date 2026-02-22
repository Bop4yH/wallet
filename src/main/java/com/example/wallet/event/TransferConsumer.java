package com.example.wallet.event;

import com.example.wallet.transfer.TransferService;
import com.example.wallet.transfer.TransferStatus;
import com.example.wallet.transfer.dto.FraudAnalysisResult;
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

    @KafkaListener(topics = "transfer-notifications", groupId = "wallet-fraud-analysis-group")
    public void monitorFraud(TransferCompletedEvent event) {
        FraudAnalysisResult result = transferService.analyzeFraud(event);

        result.setTransferId(event.getTransferId());

        switch (result.getRiskLevel()) {
            case LOW -> {
                return;
            }
            case MEDIUM -> {
                result.setMessage("WARNING: Call client required. Do not cancel yet.");
                result.setTransferStatus(TransferStatus.COMPLETED);
            }
            case HIGH -> {
                try {
                    log.warn("High fraud risk detected! Cancelling transfer {}", event.getTransferId());
                    transferService.cancel(event.getTransferId());
                    result.setMessage("CRITICAL: Transfer CANCELLED automatically. Call client.");
                    result.setTransferStatus(TransferStatus.CANCELLED);
                } catch (Exception e) {
                    result.setMessage(
                            "CRITICAL: Fraud detected but CANCEL FAILED! Call client. Error: " + e.getMessage());
                    result.setTransferStatus(TransferStatus.COMPLETED);
                }
            }
        }
        kafkaTemplate.send("fraud-alerts", result);

    }
}