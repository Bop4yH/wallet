package com.example.wallet.transfer;

import com.example.wallet.account.Account;
import com.example.wallet.account.AccountRepository;
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
     *
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
        BigDecimal amount = req.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be > 0");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);

        UUID first = fromId.compareTo(toId) < 0 ? fromId : toId;
        UUID second = fromId.compareTo(toId) < 0 ? toId : fromId;

        Account firstAcc = accountRepo.findByIdForUpdate(first)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        Account secondAcc = accountRepo.findByIdForUpdate(second)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Account from = firstAcc.getId().equals(fromId) ? firstAcc : secondAcc;
        Account to = from == firstAcc ? secondAcc : firstAcc;

        if (!from.getCurrency().equalsIgnoreCase(to.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency mismatch");
        }
        if (from.getBalance().compareTo(normalized) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(normalized));
        to.setBalance(to.getBalance().add(normalized));

        Transfer t = Transfer.builder()
                .fromAccountId(fromId)
                .toAccountId(toId)
                .amount(normalized)
                .status("COMPLETED")
                .build();

        t = transferRepo.save(t);

        return new TransferResponse(
                t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getStatus(), t.getCreatedAt()
        );
    }

    @Transactional
    public TransferResponse transferByNames(TransferByNamesRequest req) {
        String currency = req.getCurrency().toUpperCase();

        BigDecimal amount = req.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be > 0");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);

        String firstName;
        String secondName;
        boolean isFromFirst;

        if (req.getFromName().compareToIgnoreCase(req.getToName()) < 0) {
            firstName = req.getFromName();
            secondName = req.getToName();
            isFromFirst = true;
        } else {
            firstName = req.getToName();
            secondName = req.getFromName();
            isFromFirst = false;
        }

        Account firstAcc = accountRepo.findByNameAndCurrencyForUpdate(firstName, currency)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found: " + firstName));

        Account secondAcc = accountRepo.findByNameAndCurrencyForUpdate(secondName, currency)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found: " + secondName));

        Account from = isFromFirst ? firstAcc : secondAcc;
        Account to = isFromFirst ? secondAcc : firstAcc;

        if (from.getId().equals(to.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer to same account");
        }

        if (from.getBalance().compareTo(normalized) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(normalized));
        to.setBalance(to.getBalance().add(normalized));

        Transfer t = Transfer.builder()
                .fromAccountId(from.getId())
                .toAccountId(to.getId())
                .amount(normalized)
                .status("COMPLETED")
                .build();

        t = transferRepo.save(t);

        return new TransferResponse(
                t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getStatus(), t.getCreatedAt()
        );
    }

    public TransferResponse get(UUID id) {
        Transfer t = transferRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found"));
        return new TransferResponse(
                t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getStatus(), t.getCreatedAt()
        );
    }
}