package com.example.wallet.transfer.fraud;

import com.example.wallet.account.Account;
import com.example.wallet.event.TransferCompletedEvent;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
public class AccountAgeRule implements FraudRule {

    @Override
    public Optional<FraudRuleResult> check(TransferCompletedEvent event, Account account, OffsetDateTime now) {
        long minutes = ChronoUnit.MINUTES.between(account.getCreatedAt(), now);

        if (minutes < 10) {
            return Optional.of(new FraudRuleResult(60, "Critical: Account created < 10 mins ago"));
        } else if (minutes < 1440) {
            return Optional.of(new FraudRuleResult(20, "Warning: Account created < 24 hours ago"));
        }

        return Optional.empty();
    }
}