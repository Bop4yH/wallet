package com.example.wallet.account;

import com.example.wallet.account.dto.AccountResponse;
import com.example.wallet.account.dto.BalanceResponse;
import com.example.wallet.account.dto.CreateAccountRequest;
import com.example.wallet.account.dto.DepositRequest;
import com.example.wallet.account.dto.WithdrawRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
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
    public AccountResponse getByName(
            @PathVariable String ownerName,
            @RequestParam @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be 3 letters") String currency
    ) {
        return service.getByName(ownerName, currency);
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(@PathVariable UUID id) {
        return service.getBalance(id);
    }

    @PostMapping(value = "/{id}/deposit", consumes = {MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE})
    public AccountResponse deposit(@PathVariable UUID id, @Valid @RequestBody DepositRequest req) {
        return service.deposit(id, req.getAmount());
    }

    @PostMapping(value = "/by-name/{ownerName}/deposit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse depositByName(
            @PathVariable String ownerName,
            @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be 3 letters")
            @RequestParam String currency,
            @Valid @RequestBody DepositRequest req
    ) {
        return service.depositByName(ownerName, currency, req.getAmount());
    }

    @GetMapping("/show-accept-header")
    public String showAcceptHeader(@RequestHeader("Accept") String acceptHeader) {
        return "Сервер получил следующий заголовок Accept: " + acceptHeader;
    }

    @PostMapping(value = "/{id}/withdraw", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse withdraw(@PathVariable UUID id, @Valid @RequestBody WithdrawRequest req){
        return service.withdraw(id,req.getAmount());
    }
}
