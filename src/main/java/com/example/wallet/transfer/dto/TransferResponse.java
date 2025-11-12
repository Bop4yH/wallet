package com.example.wallet.transfer.dto;

import com.example.wallet.transfer.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class TransferResponse {
    private UUID id;
    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal amount;
    private TransferStatus status;
    private OffsetDateTime createdAt;
}