package com.example.wallet.utils;

import com.example.wallet.account.Account;
import com.example.wallet.common.MoneyConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

public class TestUtils {

    public static final UUID ACCOUNT_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");

    public static final UUID ACCOUNT_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

    public static final UUID DEFAULT_ACCOUNT_ID = new UUID(69, 69);

    public static final OffsetDateTime FIXED_TIME = OffsetDateTime.parse("2025-01-01T12:00:00Z");

    private TestUtils() {
    }

    public static BigDecimal money(double amount) {
        return BigDecimal.valueOf(amount).setScale(
                MoneyConstants.SCALE,
                RoundingMode.HALF_UP
        );
    }

    public static Account makeAccount(UUID id, String owner, String currency, double balance) {
        return Account.builder()
                .id(id)
                .ownerName(owner)
                .currency(currency)
                .balance(money(balance))
                .createdAt(FIXED_TIME)
                .build();
    }
}

