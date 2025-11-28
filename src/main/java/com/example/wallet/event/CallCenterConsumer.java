package com.example.wallet.event;

import com.example.wallet.transfer.dto.FraudAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CallCenterConsumer {

    @KafkaListener(
            topics = "fraud-alerts",
            groupId = "call-center-group",
            concurrency = "5",
            properties = {
                    "max.poll.interval.ms=900000", // 15 минут
                    "max.poll.records=1"
            }
    )
    public void callClient(FraudAnalysisResult alert) {
        String operatorName = Thread.currentThread().getName();

        log.info(
                "[OPERATOR {}] Calling for transfer {}. RiskLevel: {}. Message: {}.",
                operatorName, alert.getTransferId(), alert.getRiskLevel(), alert.getMessage()
        );

        try {

            Thread.sleep(TimeUnit.MINUTES.toMillis(10));
            log.info("[OPERATOR {}] Call finished.", operatorName);

        } catch (InterruptedException e) {
            log.warn("Call interrupted!");
            Thread.currentThread().interrupt();
        }
    }
}