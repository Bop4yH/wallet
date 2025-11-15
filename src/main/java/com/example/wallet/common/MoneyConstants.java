package com.example.wallet.common;

import java.math.BigDecimal;

public final class MoneyConstants {
    private MoneyConstants() {
    }

    public static final BigDecimal DAILY_TRANSFER_LIMIT = new BigDecimal("500000");

    public static final int MAX_DIGITS = 17;

    // Валидация сумм
    public static final String MIN_AMOUNT = "0.01";

    public static final String MIN_AMOUNT_MESSAGE = "amount must be > 0";

    // Точность decimal полей
    public static final int PRECISION = 19;

    public static final int SCALE = 2;

    public static final BigDecimal TRANSFER_FEE_PERCENT = new BigDecimal("0.01");

    public static final BigDecimal MIN_FEE = new BigDecimal("0.01");





}
