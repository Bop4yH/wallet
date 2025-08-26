package com.example.wallet.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AccountResponse {
    private UUID id;
    private String ownerName;
    private String currency;
    private BigDecimal balance;
    private OffsetDateTime createdAt;
}