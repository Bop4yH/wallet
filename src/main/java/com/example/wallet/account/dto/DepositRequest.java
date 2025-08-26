package com.example.wallet.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {

    @DecimalMin(value = "0.01", message = "amount must be > 0")
    @Digits(integer = 11, fraction = 2, message = "max 2 fraction digits")
    @NotNull
    private BigDecimal amount;
}