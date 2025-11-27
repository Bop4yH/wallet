package com.example.wallet.transfer.fraud;

import com.example.wallet.account.Account;
import com.example.wallet.configuration.FraudProperties;
import com.example.wallet.event.TransferCompletedEvent;
import com.example.wallet.transfer.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VelocityRule implements FraudRule {

    private final TransferRepository transferRepo;

    private final FraudProperties props;

    @Override
    public Optional<FraudRuleResult> check(TransferCompletedEvent event, Account account, OffsetDateTime now) {
        long recentCount = transferRepo.countRecentTransfers(
                event.getFromAccountId(),
                now.minusMinutes(props.getVelocityTimeMinutes())
        );

        if (recentCount > props.getVelocityLimitCount()) {
            String reason = String.format(
                    "Velocity: %d transfers in last %d mins",
                    recentCount, props.getVelocityTimeMinutes()
            );
            return Optional.of(new FraudRuleResult(40, reason));
        }
        return Optional.empty();
    }
}