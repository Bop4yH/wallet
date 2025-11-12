package com.example.wallet.common;

public final class MoneyConstants {
    private MoneyConstants() {
    }

    // Валидация сумм
    public static final String MIN_AMOUNT = "0.01";
    public static final String MIN_AMOUNT_MESSAGE = "amount must be > 0";

    // Точность decimal полей
    public static final int PRECISION = 19;
    public static final int SCALE = 2;

    public static final int MAX_DIGITS = 17;
}
