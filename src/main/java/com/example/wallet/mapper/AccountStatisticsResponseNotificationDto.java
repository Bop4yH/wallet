package com.example.wallet.mapper;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Builder(toBuilder = true)
@Jacksonized
public record AccountStatisticsResponseNotificationDto(
        long incomingTransfersCount,
        long outgoingTransfersCount,

        @NotNull
        BigDecimal totalReceived,

        @NotNull
        BigDecimal totalSent
) {

}
