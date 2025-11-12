package com.example.wallet.account;

import com.example.wallet.account.dto.AccountResponse;
import com.example.wallet.account.dto.BalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String ACCOUNT_NOT_FOUND = "Account not found";

    private final AccountRepository repo;

    public AccountResponse create(String ownerName, String currency) {
        Account a = Account.builder()
                .ownerName(ownerName)
                .currency(currency)
                .build();

        try {
            a = repo.save(a);
            return toResponse(a);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Account already exists for " + ownerName + " in " + currency.toUpperCase()
            );
        }
    }

    public AccountResponse get(UUID id) {
        Account a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        return toResponse(a);
    }

    public BalanceResponse getBalance(UUID id) {
        Account a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        return new BalanceResponse(a.getBalance(), a.getCurrency());
    }

    /**
     * Пополняет баланс счёта.
     *
     * @param id     идентификатор счёта
     * @param amount сумма пополнения (будет округлена до 2 знаков)
     * @return обновлённая информация о счёте
     * @throws ResponseStatusException если счёт не найден
     */
    @Transactional
    public AccountResponse deposit(UUID id, BigDecimal amount) {
        Account a = repo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));

        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        a.setBalance(a.getBalance().add(normalized));

        return toResponse(a);
    }

    public List<AccountResponse> list() {
        return repo.findAll().stream().map(AccountService::toResponse).toList();
    }

    public AccountResponse getByName(String ownerName, String currency) {
        Account a = repo.findByOwnerNameIgnoreCaseAndCurrency(ownerName, currency.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        return toResponse(a);
    }

    @Transactional
    public AccountResponse depositByName(String ownerName, String currency, BigDecimal amount) {
        Account account = repo.findByNameAndCurrencyForUpdate(ownerName, currency.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));

        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        account.setBalance(account.getBalance().add(normalized));

        return toResponse(account);
    }

    @Transactional
    public AccountResponse withdraw(UUID id, BigDecimal amount) {
        Account from = repo.findByIdForUpdate(id)
                .orElseThrow((() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND)));
        if (from.getBalance().compareTo(amount)<0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }
        from.setBalance(from.getBalance().subtract(amount));
        return toResponse(from);

    }
    @Transactional
    public void delete(UUID id) {
        Account toDelete =
                repo.findByIdForUpdate(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        ACCOUNT_NOT_FOUND));
        if (toDelete.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            repo.delete(toDelete);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "account must have no funds to delete");
        }
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