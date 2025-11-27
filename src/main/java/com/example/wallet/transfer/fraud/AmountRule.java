package com.example.wallet.transfer.fraud;

import com.example.wallet.account.Account;
import com.example.wallet.configuration.FraudProperties;
import com.example.wallet.event.TransferCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AmountRule implements FraudRule {

    private final FraudProperties props;

    @Override
    public Optional<FraudRuleResult> check(TransferCompletedEvent event, Account account, OffsetDateTime now) {
        if (event.getAmount().compareTo(new BigDecimal(props.getHighAmountThreshold())) > 0) {
            return Optional.of(new FraudRuleResult(30, String.format("Amount > %d", props.getHighAmountThreshold())));
        } else if (event.getAmount().compareTo(new BigDecimal(props.getMidAmountThreshold())) > 0) {
            return Optional.of(new FraudRuleResult(10, String.format("Amount > %d", props.getMidAmountThreshold())));
        }
        return Optional.empty();
    }
}