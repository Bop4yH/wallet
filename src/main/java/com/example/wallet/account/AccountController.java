package com.example.wallet.account;

import com.example.wallet.account.dto.AccountResponse;
import com.example.wallet.account.dto.BalanceResponse;
import com.example.wallet.account.dto.CreateAccountRequest;
import com.example.wallet.account.dto.DepositRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest req) {
        return service.create(req.getOwnerName(), req.getCurrency());
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    public List<AccountResponse> list() {
        return service.list();
    }

    @GetMapping("/by-name/{ownerName}")
    public AccountResponse getByName(@PathVariable String ownerName, @RequestParam String currency) {
        return service.getByName(ownerName, currency);
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(@PathVariable UUID id) {
        return service.getBalance(id);
    }

    @PostMapping(value = "/{id}/deposit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse deposit(@PathVariable UUID id, @Valid @RequestBody DepositRequest req) {
        return service.deposit(id, req.getAmount());
    }

    @PostMapping(value = "/by-name/{ownerName}/deposit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse depositByName(@PathVariable String ownerName,
                                         @RequestParam String currency,
                                         @Valid @RequestBody DepositRequest req) {
        return service.depositByName(ownerName, currency, req.getAmount());
    }
}
