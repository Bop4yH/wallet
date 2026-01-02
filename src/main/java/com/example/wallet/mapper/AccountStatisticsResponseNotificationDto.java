package com.example.wallet.mapper;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class AccountStatisticsResponseNotificationDto {
    long incomingTransfersCount;

    long outgoingTransfersCount;

    @NotNull
    BigDecimal totalReceived;

    @NotNull
    BigDecimal totalSent;
}
