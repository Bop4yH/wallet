package com.example.wallet.account;

import com.example.wallet.account.dto.AccountResponse;
import com.example.wallet.account.dto.AccountStatisticsResponse;
import com.example.wallet.account.dto.BalanceResponse;
import com.example.wallet.common.MoneyConstants;
import com.example.wallet.transfer.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {


    private static final BigDecimal EMPTY_BALLANCE = money(0);

    private static final BigDecimal DEFAULT_BALANCE = money(100);

    private static final UUID DEFAULT_ACCOUNT_ID = new UUID(69, 69);

    private static final OffsetDateTime FIXED_TIME =
            OffsetDateTime.parse("2025-01-01T12:00:00Z");

    @Mock
    private AccountRepository accountRepo;

    @Mock
    private TransferRepository transferRepo;

    @InjectMocks
    private AccountService accountService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    // ==================== HELPER METHODS ====================

    private Account savedAccount(String owner, String currency, long balance) {
        return Account.builder()
                .id(DEFAULT_ACCOUNT_ID)
                .ownerName(owner)
                .currency(currency)
                .balance(money(balance))
                .createdAt(FIXED_TIME)
                .build();
    }

    private Account defaultAccount() {
        return savedAccount("John", "USD", 100);
    }

    private Account savedEmptyAccount() {
        return savedAccount("John", "USD", 0);
    }

    private static final BigDecimal money(long balance){
        return BigDecimal.valueOf(balance).setScale(
                MoneyConstants.SCALE,
                RoundingMode.HALF_UP
        );
    }

    // ==================== CREATE ====================

    @Test
    void create_success() {
        Account saved = savedEmptyAccount();
        when(accountRepo.save(any())).thenReturn(saved);

        AccountResponse response = accountService.create("John", "USD");

        verify(accountRepo).save(accountCaptor.capture());
        assertEquals("John", response.getOwnerName());
        assertEquals("USD", response.getCurrency());
        assertEquals(EMPTY_BALLANCE, response.getBalance());
        assertEquals(DEFAULT_ACCOUNT_ID, response.getId());
        assertEquals(FIXED_TIME, response.getCreatedAt());

        Account captured = accountCaptor.getValue();
        assertEquals("John", captured.getOwnerName());
        assertEquals("USD", captured.getCurrency());
        assertNull(captured.getBalance());
        assertNull(captured.getId());
        assertNull(captured.getCreatedAt());
    }

    @Test
    void create_duplicateAccount() {
        when(accountRepo.save(any()))
                .thenThrow(new DataIntegrityViolationException("😎😎😎"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> accountService.create("John", "USD")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        assertThat(ex.getReason())
                .contains("already exists", "John", "USD");
    }

    // ==================== GET ====================

    @Test
    void get_found() {
        Account acc = defaultAccount();
        when(accountRepo.findById(acc.getId())).thenReturn(Optional.of(acc));

        AccountResponse response = accountService.get(acc.getId());

        assertEquals(acc.getId(), response.getId());
        assertEquals("John", response.getOwnerName());
        assertEquals("USD", response.getCurrency());
        assertEquals(DEFAULT_BALANCE, response.getBalance());
        assertEquals(FIXED_TIME, response.getCreatedAt());
    }

    @Test
    void get_notFound() {
        when(accountRepo.findById(DEFAULT_ACCOUNT_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> accountService.get(DEFAULT_ACCOUNT_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotNull(ex.getReason());
    }
// ==================== GET BALANCE ====================

    @Test
    void getBalance_success() {
        Account acc = savedAccount("John", "EUR", 250);
        when(accountRepo.findById(acc.getId())).thenReturn(Optional.of(acc));

        BalanceResponse response = accountService.getBalance(acc.getId());

        assertEquals(money(250), response.getBalance());
        assertEquals("EUR", response.getCurrency());
    }

    @Test
    void getBalance_notFound() {
        when(accountRepo.findById(DEFAULT_ACCOUNT_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> accountService.getBalance(DEFAULT_ACCOUNT_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotNull(ex.getReason());
    }


}