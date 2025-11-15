package com.example.wallet.transfer.dto;

import com.example.wallet.common.MoneyConstants;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferByNamesRequest {

    @NotBlank
    private String fromName;

    @NotBlank
    private String toName;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Use ISO currency code, e.g. RUB")
    private String currency;

    @NotNull
    @DecimalMin(value = MoneyConstants.MIN_AMOUNT, message = MoneyConstants.MIN_AMOUNT_MESSAGE)
    @Digits(integer = MoneyConstants.MAX_DIGITS, fraction = MoneyConstants.SCALE)
    private BigDecimal amount;
}