package com.example.wallet.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "wallet.fraud")
public class FraudProperties {

    private int scoreThresholdMedium;

    private int scoreThresholdHigh;

    private int velocityTimeMinutes;

    private int velocityLimitCount;

    private int highAmountThreshold;

    private int midAmountThreshold;
}
