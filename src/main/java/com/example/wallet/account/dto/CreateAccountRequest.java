package com.example.wallet.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAccountRequest {
    @NotBlank
    @Size(max = 100)
    private String ownerName;

    /**
     * Код валюты в формате ISO 4217.
     * Должен состоять из 3 заглавных букв (например: USD, EUR, RUB).
     */
    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "Use ISO currency code, e.g. RUB")
    private String currency;
}