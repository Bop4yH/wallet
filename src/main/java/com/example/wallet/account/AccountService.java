package com.example.wallet.account;

import com.example.wallet.account.dto.AccountResponse;
import com.example.wallet.account.dto.AccountStatisticsResponse;
import com.example.wallet.account.dto.BalanceResponse;
import com.example.wallet.common.MoneyConstants;
import com.example.wallet.mapper.AccountStatisticsResponseNotificationDto;
import com.example.wallet.mapper.WalletMapper;
import com.example.wallet.transfer.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private static final String ACCOUNT_NOT_FOUND = "Account not found";

    private final AccountRepository accountRepo;

    private final TransferRepository transferRepo;

    private final WalletMapper walletMapper;

    @Qualifier("dbExecutor")
    private final Executor dbExecutor;

    public AccountResponse create(String ownerName, String currency) {
        Account a = Account.builder()
                .ownerName(ownerName)
                .currency(currency)
                .build();

        try {
            a = accountRepo.save(a);
            return walletMapper.toAccountResponse(a);
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
        return walletMapper.toAccountResponse(a);
    }

    public BalanceResponse getBalance(UUID id) {
        Account a = accountRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        return walletMapper.toBalanceResponse(a);
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

        return walletMapper.toAccountResponse(a);
    }

    public List<AccountResponse> list() {
        return accountRepo.findAll().stream().map(walletMapper::toAccountResponse).toList();
    }

    public AccountResponse getByName(String ownerName, String currency) {
        Account a = accountRepo.findByOwnerNameIgnoreCaseAndCurrency(ownerName, currency.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        return walletMapper.toAccountResponse(a);
    }

    @Transactional
    public AccountResponse depositByName(String ownerName, String currency, BigDecimal amount) {
        Account account = accountRepo.findByNameAndCurrencyForUpdate(ownerName, currency.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));

        BigDecimal normalized = amount.setScale(MoneyConstants.SCALE, RoundingMode.HALF_UP);
        account.setBalance(account.getBalance().add(normalized));

        return walletMapper.toAccountResponse(account);
    }

    @Transactional
    public AccountResponse withdraw(UUID id, BigDecimal amount) {
        Account from = accountRepo.findByIdForUpdate(id)
                .orElseThrow((() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND)));
        if (from.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }
        from.setBalance(from.getBalance().subtract(amount));
        return walletMapper.toAccountResponse(from);

    }

    @Transactional
    public void delete(UUID id) {
        Account toDelete = accountRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        if (toDelete.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            accountRepo.delete(toDelete);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "account must have no funds to delete"
            );
        }
    }

    public AccountStatisticsResponse getStatisticsAsync(UUID id) {
        Account account = accountRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));

        CompletableFuture<Long> incomingCountFuture = CompletableFuture.supplyAsync(
                () -> transferRepo.countIncomingTransfersById(id), dbExecutor);

        CompletableFuture<Long> outgoingCountFuture = CompletableFuture.supplyAsync(
                () -> transferRepo.countOutgoingTransfersById(id), dbExecutor);

        CompletableFuture<BigDecimal> incomingSumFuture = CompletableFuture.supplyAsync(
                () -> transferRepo.sumIncomingTransfers(id), dbExecutor);

        CompletableFuture<BigDecimal> outgoingSumFuture = CompletableFuture.supplyAsync(
                () -> transferRepo.sumOutgoingTransfers(id), dbExecutor);

        CompletableFuture.allOf(
                incomingCountFuture, outgoingCountFuture, incomingSumFuture, outgoingSumFuture
        ).join();

        return new AccountStatisticsResponse(
                account.getBalance(),
                incomingCountFuture.join(),
                outgoingCountFuture.join(),
                incomingSumFuture.join(),
                outgoingSumFuture.join()
        );
    }

    public AccountStatisticsResponseNotificationDto getNotificationStatistics(UUID id) {
        Account account = accountRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        AccountStatisticsResponse response = new AccountStatisticsResponse(
                account.getBalance(),
                transferRepo.countIncomingTransfersById(id),
                transferRepo.countOutgoingTransfersById(id),
                transferRepo.sumIncomingTransfers(id),
                transferRepo.sumOutgoingTransfers(id)
        );
        return walletMapper.toNotificationDto(response);
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
        return walletMapper.toAccountResponse(account);
    }

    @Recover
    public AccountResponse recoverBonus(ObjectOptimisticLockingFailureException e, UUID id, BigDecimal bonusAmount) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Something went wrong, please try again later");
    }
}