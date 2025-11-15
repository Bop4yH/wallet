package com.example.wallet.account;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountLockingService {

    private final AccountRepository accountRepo;

    public AccountPair lockTwoAccounts(UUID fromId, UUID toId) {
        if (fromId.equals(toId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from and to must differ");
        }

        UUID first = fromId.compareTo(toId) < 0 ? fromId : toId;
        UUID second = fromId.compareTo(toId) < 0 ? toId : fromId;

        Account firstAcc = findAndLock(first);
        Account secondAcc = findAndLock(second);

        Account from = firstAcc.getId().equals(fromId) ? firstAcc : secondAcc;
        Account to = firstAcc.getId().equals(toId) ? firstAcc : secondAcc;

        return new AccountPair(from, to);
    }

    public AccountPair lockTwoAccountsByName(String fromName, String toName, String currency) {
        boolean isFromFirst = fromName.compareToIgnoreCase(toName) < 0;
        String first = isFromFirst ? fromName : toName;
        String second = isFromFirst ? toName : fromName;

        Account firstAcc = findAndLockByName(first, currency);
        Account secondAcc = findAndLockByName(second, currency);

        Account from = isFromFirst ? firstAcc : secondAcc;
        Account to = isFromFirst ? secondAcc : firstAcc;

        return new AccountPair(from, to);
    }

    public record AccountPair(Account from, Account to) {

    }

    private Account findAndLock(UUID id) {
        return accountRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private Account findAndLockByName(String name, String currency) {
        return accountRepo.findByNameAndCurrencyForUpdate(name, currency)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + name));
    }
}
