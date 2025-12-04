package com.example.wallet.account.dto;

import com.example.wallet.common.MoneyConstants;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawRequest {

    @DecimalMin(value = MoneyConstants.MIN_AMOUNT, message = MoneyConstants.MIN_AMOUNT_MESSAGE)
    @Digits(integer = MoneyConstants.MAX_DIGITS,
            fraction = MoneyConstants.SCALE,
            message = "max " + MoneyConstants.SCALE + " fraction digits")
    @NotNull
    private BigDecimal amount;

}
