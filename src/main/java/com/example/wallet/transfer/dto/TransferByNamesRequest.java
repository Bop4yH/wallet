package com.example.wallet.transfer.dto;

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
    @Pattern(regexp = "^[A-Z]{3}$", message = "Use ISO currency code, e.g. RUB")
    private String currency;

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be > 0")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal amount;
}