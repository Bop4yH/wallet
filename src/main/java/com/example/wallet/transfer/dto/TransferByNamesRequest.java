package com.example.wallet.transfer.dto;

import com.example.wallet.common.MoneyConstants;
import com.example.wallet.validation.CurrencyCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferByNamesRequest {

    @NotNull
    private UUID idempotencyKey;

    @NotBlank
    private String fromName;

    @NotBlank
    private String toName;

    @NotBlank
    @CurrencyCode
    private String currency;

    @NotNull
    @DecimalMin(value = MoneyConstants.MIN_AMOUNT, message = MoneyConstants.MIN_AMOUNT_MESSAGE)
    @Digits(integer = MoneyConstants.MAX_DIGITS, fraction = MoneyConstants.SCALE)
    private BigDecimal amount;
}