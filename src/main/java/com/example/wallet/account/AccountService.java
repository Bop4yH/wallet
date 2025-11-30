package com.example.wallet.account;

import com.example.wallet.account.dto.AccountResponse;
import com.example.wallet.account.dto.AccountStatisticsResponse;
import com.example.wallet.account.dto.BalanceResponse;
import com.example.wallet.common.MoneyConstants;
import com.example.wallet.transfer.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private static final String ACCOUNT_NOT_FOUND = "Account not found";

    private final AccountRepository accountRepo;

    private final TransferRepository transferRepo;

    public AccountResponse create(String ownerName, String currency) {
        Account a = Account.builder()
                .ownerName(ownerName)
                .currency(currency)
                .build();

        try {
            a = accountRepo.save(a);
            return toResponse(a);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Account already exists for " + ownerName + " in " + currency.toUpperCase()
            );
        }
    }

    public AccountResponse get(UUID id) {
        Account a = accountRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        return toResponse(a);
    }

    public BalanceResponse getBalance(UUID id) {
        Account a = accountRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        return new BalanceResponse(a.getBalance(), a.getCurrency());
    }

    /**
     * Пополняет баланс счёта.
     *
     * @param id     идентификатор счёта
     * @param amount сумма пополнения (будет округлена)
     * @return обновлённая информация о счёте
     * @throws ResponseStatusException если счёт не найден
     */
    @Transactional
    public AccountResponse deposit(UUID id, BigDecimal amount) {
        Account a = accountRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));

        BigDecimal normalized = amount.setScale(MoneyConstants.SCALE, RoundingMode.HALF_UP);
        a.setBalance(a.getBalance().add(normalized));

        return toResponse(a);
    }

    public List<AccountResponse> list() {
        return accountRepo.findAll().stream().map(AccountService::toResponse).toList();
    }

    public AccountResponse getByName(String ownerName, String currency) {
        Account a = accountRepo.findByOwnerNameIgnoreCaseAndCurrency(ownerName, currency.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        return toResponse(a);
    }

    @Transactional
    public AccountResponse depositByName(String ownerName, String currency, BigDecimal amount) {
        Account account = accountRepo.findByNameAndCurrencyForUpdate(ownerName, currency.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));

        BigDecimal normalized = amount.setScale(MoneyConstants.SCALE, RoundingMode.HALF_UP);
        account.setBalance(account.getBalance().add(normalized));

        return toResponse(account);
    }

    @Transactional
    public AccountResponse withdraw(UUID id, BigDecimal amount) {
        Account from = accountRepo.findByIdForUpdate(id)
                .orElseThrow((() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND)));
        if (from.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }
        from.setBalance(from.getBalance().subtract(amount));
        return toResponse(from);

    }

    @Transactional
    public void delete(UUID id) {
        Account toDelete =
                accountRepo.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        ACCOUNT_NOT_FOUND
                ));
        if (toDelete.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            accountRepo.delete(toDelete);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "account must have no funds to delete"
            );
        }
    }

    public AccountStatisticsResponse getStatistics(UUID id) {
        Account account = accountRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        return new AccountStatisticsResponse(
                account.getBalance(),
                transferRepo.countIncomingTransfersById(id),
                transferRepo.countOutgoingTransfersById(id),
                transferRepo.sumIncomingTransfers(id),
                transferRepo.sumOutgoingTransfers(id)
        );
    }

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    @Transactional
    public AccountResponse addBonus(UUID id, BigDecimal bonusAmount) {
        Account account = accountRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        log.info("Поток " + Thread.currentThread().getName() + " прочитал версию: " + account.getVersion());
        account.setBalance(account.getBalance().add(bonusAmount));
        return toResponse(account);
    }

    private static AccountResponse toResponse(Account a) {
        return new AccountResponse(
                a.getId(),
                a.getOwnerName(),
                a.getCurrency(),
                a.getBalance(),
                a.getCreatedAt()
        );
    }
}