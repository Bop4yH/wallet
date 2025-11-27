package com.example.wallet.transfer.fraud;

import com.example.wallet.account.Account;
import com.example.wallet.event.TransferCompletedEvent;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface FraudRule {

    /**
     * Проверяет событие на мошенничество.
     *
     * @param event   само событие перевода
     * @param account аккаунт отправителя
     * @param now     текущее время
     * @return Optional с результатом, если правило сработало. Empty, если не сработало.
     */
    Optional<FraudRuleResult> check(TransferCompletedEvent event, Account account, OffsetDateTime now);
}
