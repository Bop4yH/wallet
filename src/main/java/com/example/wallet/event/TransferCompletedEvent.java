package com.example.wallet.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferCompletedEvent {

    private UUID transferId;

    private UUID fromAccountId;

    private UUID toAccountId;

    private BigDecimal amount;
}