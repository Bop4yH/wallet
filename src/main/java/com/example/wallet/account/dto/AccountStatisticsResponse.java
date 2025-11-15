package com.example.wallet.account.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AccountStatisticsResponse {

    @NotNull
    BigDecimal currentBalance;

    long incomingTransfersCount;

    long outgoingTransfersCount;

    @NotNull
    BigDecimal totalReceived;

    @NotNull
    BigDecimal totalSent;

}
