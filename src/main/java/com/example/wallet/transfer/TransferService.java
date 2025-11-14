package com.example.wallet.transfer;

import com.example.wallet.account.Account;
import com.example.wallet.account.AccountRepository;
import com.example.wallet.common.MoneyConstants;
import com.example.wallet.transfer.dto.CountResponse;
import com.example.wallet.transfer.dto.TransferByNamesRequest;
import com.example.wallet.transfer.dto.TransferRequest;
import com.example.wallet.transfer.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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

    /**
     * Выполняет перевод между счетами по их ID.
     * <p>
     * Использует пессимистичные блокировки для предотвращения race conditions
     * и детерминированный порядок блокировки для предотвращения deadlock.
     *
     * @param req запрос с ID счетов и суммой перевода
     * @return информация о выполненном переводе
     * @throws ResponseStatusException если счета не найдены, недостаточно средств или валюты не совпадают
     */
    @Transactional
    public TransferResponse transfer(TransferRequest req) {
        UUID fromId = req.getFromAccountId();
        UUID toId = req.getToAccountId();
        if (fromId.equals(toId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from and to must differ");
        }
        int comparison = fromId.compareTo(toId);
        UUID first = comparison < 0 ? fromId : toId;
        UUID second = comparison < 0 ? toId : fromId;

        Account firstAcc = accountRepo.findByIdForUpdate(first)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        Account secondAcc = accountRepo.findByIdForUpdate(second)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Account from = firstAcc.getId().equals(fromId) ? firstAcc : secondAcc;
        Account to = firstAcc.getId().equals(toId) ? firstAcc : secondAcc;

        return transferByAccounts(from, to, req.getAmount());
    }

    @Transactional
    public TransferResponse transferByNames(TransferByNamesRequest req) {
        String currency = req.getCurrency().toUpperCase();
        boolean isFromNameFirst = req.getFromName().compareToIgnoreCase(req.getToName()) < 0;
        String firstName = isFromNameFirst ? req.getFromName() : req.getToName();
        String secondName = isFromNameFirst ? req.getToName() : req.getFromName();

        Account firstAcc = accountRepo.findByNameAndCurrencyForUpdate(firstName, currency)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found: " + firstName
                ));

        Account secondAcc = accountRepo.findByNameAndCurrencyForUpdate(secondName, currency)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found: " + secondName
                ));

        Account from = isFromNameFirst ? firstAcc : secondAcc;
        Account to = isFromNameFirst ? secondAcc : firstAcc;
        return transferByAccounts(from, to, req.getAmount());
    }

    public TransferResponse get(UUID id) {
        Transfer t = transferRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found"));
        return new TransferResponse(
                t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getStatus(), t.getCreatedAt()
        );
    }

    @Transactional
    public TransferResponse cancel(UUID id) {
        Transfer t = transferRepo.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "no such transfer"
        ));
        UUID fromId = t.getFromAccountId();
        UUID toId = t.getToAccountId();
        if (t.getStatus() == TransferStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transfer already cancelled");
        }
        if (t.getCreatedAt().plusMinutes(5).isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "5 minutes passed, can't cancel");
        }


        int comparison = fromId.compareTo(toId);
        UUID first = comparison < 0 ? fromId : toId;
        UUID second = comparison < 0 ? toId : fromId;

        Account firstAcc =
                accountRepo.findByIdForUpdate(first).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "account already not exists"
                ));
        Account secondAcc =
                accountRepo.findByIdForUpdate(second).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "account already not exists"
                ));

        Account to = firstAcc.getId().equals(toId) ? firstAcc : secondAcc;
        Account from = firstAcc.getId().equals(fromId) ? firstAcc : secondAcc;

        if (to.getBalance().compareTo(t.getAmount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot cancel: recipient has insufficient funds"
            );
        }
        to.setBalance(to.getBalance().subtract(t.getAmount()));
        from.setBalance(from.getBalance().add(t.getAmount()));
        t.setStatus(TransferStatus.CANCELLED);
        return new TransferResponse(
                t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getStatus(), t.getCreatedAt()
        );
    }

    public CountResponse count() {
        return new CountResponse(transferRepo.countTransfersByStatus(TransferStatus.COMPLETED));
    }

    private TransferResponse transferByAccounts(Account from, Account to, BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be > 0");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (from.getId().equals(to.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer to same account");
        }

        if (from.getBalance().compareTo(normalized) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }

        if (!from.getCurrency().equalsIgnoreCase(to.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency mismatch");
        }
        validateDailyLimit(from, normalized);
        from.setBalance(from.getBalance().subtract(normalized));
        to.setBalance(to.getBalance().add(normalized));

        Transfer t = Transfer.builder()
                .fromAccountId(from.getId())
                .toAccountId(to.getId())
                .amount(normalized)
                .status(TransferStatus.COMPLETED)
                .build();

        t = transferRepo.save(t);

        return new TransferResponse(
                t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getStatus(), t.getCreatedAt()
        );
    }

    private void validateDailyLimit(Account account, BigDecimal transferAmount) {
        OffsetDateTime startOfDay = OffsetDateTime.now(ZoneOffset.UTC)
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
}