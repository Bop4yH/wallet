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
    private static final UUID DEFAULT_ACCOUNT_ID = new UUID(69, 69);

    private static final BigDecimal DEFAULT_BALANCE = money(100);

    private static final BigDecimal EMPTY_BALANCE = money(0);

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

    private static final BigDecimal money(double balance) {
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
        assertEquals(EMPTY_BALANCE, response.getBalance());
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
    }

    @Test
    void get_accountNotFound() {
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

// ==================== DEPOSIT ====================

    @Test
    void deposit_success() {
        Account acc = defaultAccount();
        when(accountRepo.findByIdForUpdate(acc.getId())).thenReturn(Optional.of(acc));

        AccountResponse response = accountService.deposit(acc.getId(), money(50));

        assertEquals(money(150), acc.getBalance());
        assertEquals(money(150), response.getBalance());
        assertEquals(DEFAULT_ACCOUNT_ID, response.getId());
    }

    @ParameterizedTest
    @CsvSource({
            "50,     50.00",
            "50.1,   50.10",
            "50.99,  50.99",
            "100.5,  100.50"
    })
    void deposit_normalizesScale(String input, String expected) {
        Account acc = savedEmptyAccount();
        when(accountRepo.findByIdForUpdate(acc.getId())).thenReturn(Optional.of(acc));

        accountService.deposit(acc.getId(), new BigDecimal(input));

        BigDecimal balance = acc.getBalance();
        assertEquals(new BigDecimal(expected), balance);
        assertEquals(2, balance.scale());
    }

    @Test
    void deposit_accountNotFound() {
        when(accountRepo.findByIdForUpdate(DEFAULT_ACCOUNT_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> accountService.deposit(DEFAULT_ACCOUNT_ID, DEFAULT_BALANCE)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotNull(ex.getReason());
    }

    // ==================== LIST ====================

    @Test
    void list_returnsAllAccounts() {
        List<Account> accounts = List.of(
                savedAccount("John", "USD", 0),
                savedAccount("Jane", "EUR", 0)
        );
        when(accountRepo.findAll()).thenReturn(accounts);

        List<AccountResponse> response = accountService.list();

        assertEquals(2, response.size());
        assertEquals("John", response.get(0).getOwnerName());
        assertEquals("Jane", response.get(1).getOwnerName());
    }

    @Test
    void list_returnsEmptyList() {
        when(accountRepo.findAll()).thenReturn(List.of());

        List<AccountResponse> response = accountService.list();

        assertNotNull(response);
        assertTrue(response.isEmpty());
        assertEquals(0, response.size());
    }

    // ==================== GET BY NAME ====================

    @Test
    void getByName_found() {
        Account acc = savedEmptyAccount();
        when(accountRepo.findByOwnerNameIgnoreCaseAndCurrency("John", "USD"))
                .thenReturn(Optional.of(acc));

        AccountResponse response = accountService.getByName("John", "USD");

        assertEquals(acc.getId(), response.getId());
    }

    @Test
    void getByName_notFound() {
        when(accountRepo.findByOwnerNameIgnoreCaseAndCurrency("John", "USD"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> accountService.getByName("John", "USD")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotNull(ex.getReason());
    }

    // ==================== DEPOSIT BY NAME ====================

    @Test
    void depositByName_success() {
        Account acc = defaultAccount();
        when(accountRepo.findByNameAndCurrencyForUpdate("John", "USD"))
                .thenReturn(Optional.of(acc));

        accountService.depositByName("John", "USD", money(50));

        assertEquals(money(150), acc.getBalance());
    }

    @Test
    void depositByName_notFound() {
        when(accountRepo.findByNameAndCurrencyForUpdate("John", "USD"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> accountService.depositByName("John", "USD", new BigDecimal("50.00"))
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotNull(ex.getReason());
    }

    // ==================== WITHDRAW ====================

    @Test
    void withdraw_success() {
        Account acc = defaultAccount();
        when(accountRepo.findByIdForUpdate(acc.getId())).thenReturn(Optional.of(acc));

        accountService.withdraw(acc.getId(), money(30));

        assertEquals(money(70), acc.getBalance());
    }

    @Test
    void withdraw_insufficientFunds() {
        Account acc = savedAccount("John", "USD", 50);
        BigDecimal amount = money(100);
        when(accountRepo.findByIdForUpdate(acc.getId())).thenReturn(Optional.of(acc));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> accountService.withdraw(acc.getId(), amount)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Insufficient funds"));
    }

    // ==================== DELETE ====================

    @Test
    void delete_success() {
        Account acc = savedEmptyAccount();
        when(accountRepo.findByIdForUpdate(acc.getId())).thenReturn(Optional.of(acc));

        accountService.delete(acc.getId());

        verify(accountRepo).delete(acc);
    }

    @Test
    void delete_nonZeroBalance() {
        Account acc = defaultAccount();
        when(accountRepo.findByIdForUpdate(acc.getId())).thenReturn(Optional.of(acc));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> accountService.delete(acc.getId())
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("no funds"));
        verify(accountRepo, never()).delete(any());
    }

    // ==================== GET STATISTICS ====================

    @Test
    void getStatistics_success() {
        Account acc = savedAccount("John", "USD", 500);
        UUID id = acc.getId();

        when(accountRepo.findById(id)).thenReturn(Optional.of(acc));
        when(transferRepo.countIncomingTransfersById(id)).thenReturn(5L);
        when(transferRepo.countOutgoingTransfersById(id)).thenReturn(3L);
        when(transferRepo.sumIncomingTransfers(id)).thenReturn(new BigDecimal("1000.00"));
        when(transferRepo.sumOutgoingTransfers(id)).thenReturn(new BigDecimal("500.00"));

        AccountStatisticsResponse stats = accountService.getStatistics(id);

        assertEquals(money(500), stats.getCurrentBalance());
        assertEquals(5L, stats.getIncomingTransfersCount());
        assertEquals(3L, stats.getOutgoingTransfersCount());
        assertEquals(money(1000), stats.getTotalReceived());
        assertEquals(money(500), stats.getTotalSent());
    }
}