package com.example.wallet.transfer.dto;

import com.example.wallet.common.MoneyConstants;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferRequest {
    @NotNull
    private UUID fromAccountId;

    @NotNull
    private UUID toAccountId;

    @NotNull
    @DecimalMin(value = MoneyConstants.MIN_AMOUNT, message = MoneyConstants.MIN_AMOUNT_MESSAGE)
    @Digits(integer = MoneyConstants.PRECISION, fraction = MoneyConstants.SCALE)
    private BigDecimal amount;
}