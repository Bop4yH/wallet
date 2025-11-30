package com.example.wallet.transfer;

import com.example.wallet.account.Account;
import com.example.wallet.account.AccountLockingService;
import com.example.wallet.account.AccountRepository;
import com.example.wallet.common.MoneyConstants;
import com.example.wallet.configuration.FraudProperties;
import com.example.wallet.event.TransferCompletedEvent;
import com.example.wallet.transfer.dto.CountResponse;
import com.example.wallet.transfer.dto.FraudAnalysisResult;
import com.example.wallet.transfer.dto.FraudRiskLevel;
import com.example.wallet.transfer.dto.TransferResponse;
import com.example.wallet.transfer.fraud.FraudRule;
import com.example.wallet.transfer.fraud.FraudRuleResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для выполнения денежных переводов между счетами.
 * Обеспечивает транзакционность и защиту от race conditions.
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepo;

    private final TransferRepository transferRepo;

    private final AccountLockingService accountLockingService;

    private final Clock clock;

    private final ApplicationEventPublisher eventPublisher;

    private final FraudProperties fraudProperties;

    private final List<FraudRule> fraudRules;

    /**
     * Выполняет перевод между счетами по их ID.
     * <p>
     * Использует пессимистичные блокировки для предотвращения race conditions
     * и детерминированный порядок блокировки для предотвращения deadlock.
     *
     * @param fromId ID счета списания
     * @param toId   ID счета пополнения
     * @param amount сумма перевода
     * @return информация о выполненном переводе
     * @throws ResponseStatusException если счета не найдены, недостаточно средств или валюты не совпадают
     */
    @Transactional
    public TransferResponse transfer(UUID fromId, UUID toId, BigDecimal amount, UUID idempotencyKey) {
        Optional<Transfer> existing = transferRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        AccountLockingService.AccountPair accounts = accountLockingService.lockTwoAccounts(
                fromId,
                toId
        );


        return transferByAccounts(accounts.from(), accounts.to(), amount, idempotencyKey);
    }

    @Transactional
    public TransferResponse transferByNames(
            String fromName, String toName, String currency, BigDecimal amount, UUID idempotencyKey) {
        Optional<Transfer> existing = transferRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        AccountLockingService.AccountPair accounts = accountLockingService.lockTwoAccountsByName(
                fromName,
                toName,
                currency
        );
        return transferByAccounts(accounts.from(), accounts.to(), amount, idempotencyKey);
    }

    public TransferResponse get(UUID id) {
        Transfer t = transferRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found"));
        return new TransferResponse(
                t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getStatus(), t.getCreatedAt(), t.getFee()
        );
    }

    @Transactional
    public TransferResponse cancel(UUID id) {
        Transfer t = transferRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no such transfer"));

        if (t.getStatus() == TransferStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transfer already cancelled");
        }
        if (t.getCreatedAt().plusMinutes(5).isBefore(OffsetDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "5 minutes passed, can't cancel");
        }

        AccountLockingService.AccountPair accounts = accountLockingService.lockTwoAccounts(
                t.getFromAccountId(),
                t.getToAccountId()
        );

        if (accounts.to().getBalance().compareTo(t.getAmount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot cancel: recipient has insufficient funds"
            );
        }

        accounts.to().setBalance(accounts.to().getBalance().subtract(t.getAmount()));
        accounts.from().setBalance(accounts.from().getBalance().add(t.getAmount().add(t.getFee())));
        t.setStatus(TransferStatus.CANCELLED);

        return new TransferResponse(
                t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getStatus(), t.getCreatedAt(), t.getFee()
        );
    }

    public CountResponse count() {
        return new CountResponse(transferRepo.countTransfersByStatus(TransferStatus.COMPLETED));
    }

    @Transactional(readOnly = true)
    public FraudAnalysisResult analyzeFraud(TransferCompletedEvent event) {
        Account fromAccount = accountRepo.findById(event.getFromAccountId())
                .orElseThrow(() -> new IllegalStateException("Account not found"));

        OffsetDateTime now = OffsetDateTime.now(clock);
        int totalScore = 0;
        List<String> reasons = new ArrayList<>();

        for (FraudRule rule : fraudRules) {
            Optional<FraudRuleResult> resultOpt = rule.check(event, fromAccount, now);
            if (resultOpt.isPresent()) {
                FraudRuleResult res = resultOpt.get();
                totalScore += res.score();
                reasons.add(res.reason() + " (+" + res.score() + ")");
            }
        }

        FraudRiskLevel finalLevel = FraudRiskLevel.LOW;
        if (totalScore >= fraudProperties.getScoreThresholdHigh()) {
            finalLevel = FraudRiskLevel.HIGH;
        } else if (totalScore >= fraudProperties.getScoreThresholdMedium()) {
            finalLevel = FraudRiskLevel.MEDIUM;
        }

        return FraudAnalysisResult.builder()
                .riskLevel(finalLevel)
                .reasons(reasons)
                .suspiciousAmount(event.getAmount())
                .build();
    }

    private TransferResponse transferByAccounts(Account from, Account to, BigDecimal amount, UUID idempotencyKey) {

        Optional<Transfer> existing = transferRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be > 0");
        }
        BigDecimal normalized = amount.setScale(MoneyConstants.SCALE, RoundingMode.HALF_UP);
        BigDecimal fee = normalized.multiply(MoneyConstants.TRANSFER_FEE_PERCENT)
                .setScale(MoneyConstants.SCALE, RoundingMode.HALF_UP);
        fee = fee.max(MoneyConstants.MIN_FEE);
        BigDecimal amountWithFee = normalized.add(fee);

        if (from.getId().equals(to.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer to same account");
        }

        if (from.getBalance().compareTo(amountWithFee) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }

        if (!from.getCurrency().equalsIgnoreCase(to.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency mismatch");
        }
        validateDailyLimit(from, normalized);

        from.setBalance(from.getBalance().subtract(amountWithFee));
        to.setBalance(to.getBalance().add(normalized));

        Transfer t = Transfer.builder()
                .idempotencyKey(idempotencyKey)
                .fromAccountId(from.getId())
                .toAccountId(to.getId())
                .amount(normalized)
                .status(TransferStatus.COMPLETED)
                .fee(fee)
                .build();

        t = transferRepo.save(t);

        TransferCompletedEvent event = new TransferCompletedEvent(
                t.getId(),
                t.getFromAccountId(),
                t.getToAccountId(),
                t.getAmount()
        );
        eventPublisher.publishEvent(event);
        return toResponse(t);
    }

    private void validateDailyLimit(Account account, BigDecimal transferAmount) {
        OffsetDateTime startOfDay = OffsetDateTime.now(clock)
                .truncatedTo(ChronoUnit.DAYS);

        BigDecimal todayTotal = transferRepo.sumDailyTransfers(
                account.getId(), startOfDay);

        BigDecimal newTotal = todayTotal.add(transferAmount);

        if (newTotal.compareTo(MoneyConstants.DAILY_TRANSFER_LIMIT) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format(
                            "Daily transfer limit exceeded: %.2f / %.2f",
                            newTotal, MoneyConstants.DAILY_TRANSFER_LIMIT
                    )
            );
        }
    }

    private TransferResponse toResponse(Transfer t) {
        return new TransferResponse(
                t.getId(),
                t.getFromAccountId(),
                t.getToAccountId(),
                t.getAmount(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getFee()
        );
    }
}