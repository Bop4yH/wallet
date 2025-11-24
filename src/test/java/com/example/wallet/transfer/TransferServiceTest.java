package com.example.wallet.transfer;

import com.example.wallet.account.Account;
import com.example.wallet.account.AccountLockingService;
import com.example.wallet.common.MoneyConstants;
import com.example.wallet.transfer.dto.CountResponse;
import com.example.wallet.transfer.dto.TransferResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.example.wallet.utils.TestUtils.ACCOUNT_ID_1;
import static com.example.wallet.utils.TestUtils.ACCOUNT_ID_2;
import static com.example.wallet.utils.TestUtils.FIXED_TIME;
import static com.example.wallet.utils.TestUtils.makeAccount;
import static com.example.wallet.utils.TestUtils.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final UUID DEFAULT_TRANSFER_ID = new UUID(69, 69);

    private static final String JANE = "Jane";

    private static final String JOHN = "John";

    private static final String RUB = "RUB";

    private static final String USD = "USD";

    private static final BigDecimal ZERO_AMOUNT = money(0);

    @Spy
    private Clock clock = Clock.fixed(FIXED_TIME.toInstant(), ZoneOffset.UTC);

    @Captor
    private ArgumentCaptor<Transfer> transferCaptor;

    @Mock
    private TransferRepository transferRepo;

    @Mock
    private AccountLockingService accountLockingService;

    @InjectMocks
    private TransferService transferService;

    private Transfer makeTransfer(long amount, double fee) {
        return Transfer.builder()
                .fromAccountId(ACCOUNT_ID_1)
                .toAccountId(ACCOUNT_ID_2)
                .amount(money(amount))
                .createdAt(FIXED_TIME)
                .id(DEFAULT_TRANSFER_ID)
                .fee(money(fee))
                .status(TransferStatus.COMPLETED)
                .build();
    }

    private AccountLockingService.AccountPair createAccountPair(
            double fromBalance,
            double toBalance,
            String fromCurrency,
            String toCurrency
    ) {
        return new AccountLockingService.AccountPair(
                makeAccount(ACCOUNT_ID_1, JOHN, fromCurrency, fromBalance),
                makeAccount(ACCOUNT_ID_2, JANE, toCurrency, toBalance)
        );
    }

    private AccountLockingService.AccountPair createAccountPair(double fromBalance, double toBalance) {
        return createAccountPair(fromBalance, toBalance, USD, USD);
    }

    @Test
    void transfer_success() {
        AccountLockingService.AccountPair accounts = createAccountPair(300, 0);
        BigDecimal transferAmount = money(100);
        BigDecimal expectedFee = money(1);
        BigDecimal expectedFromBalance = money(199);
        BigDecimal expectedToBalance = money(100);
        Transfer preparedTransfer = makeTransfer(100, 1);

        when(accountLockingService.lockTwoAccounts(ACCOUNT_ID_1, ACCOUNT_ID_2)).thenReturn(accounts);
        when(transferRepo.sumDailyTransfers(any(), any())).thenReturn(ZERO_AMOUNT);
        when(transferRepo.save(transferCaptor.capture())).thenReturn(preparedTransfer);

        TransferResponse response = transferService.transfer(ACCOUNT_ID_1, ACCOUNT_ID_2, transferAmount);

        assertEquals(transferAmount, response.getAmount());
        assertEquals(expectedFee, response.getFee());
        assertEquals(TransferStatus.COMPLETED, response.getStatus());
        assertEquals(ACCOUNT_ID_1, response.getFromAccountId());
        assertEquals(ACCOUNT_ID_2, response.getToAccountId());

        assertEquals(expectedFromBalance, accounts.from().getBalance());
        assertEquals(expectedToBalance, accounts.to().getBalance());

        Transfer captured = transferCaptor.getValue();
        assertEquals(ACCOUNT_ID_1, captured.getFromAccountId());
        assertEquals(ACCOUNT_ID_2, captured.getToAccountId());
        assertEquals(transferAmount, captured.getAmount());
        assertEquals(expectedFee, captured.getFee());
        assertEquals(TransferStatus.COMPLETED, captured.getStatus());
        assertNull(captured.getId());
        assertNull(captured.getCreatedAt());
    }

    @ParameterizedTest
    @CsvSource({
            "100, 101",
            "100, 100"
    })
    void transfer_insufficientFunds(long balance, BigDecimal transferAmount) {
        AccountLockingService.AccountPair accounts = createAccountPair(balance, 0);

        when(accountLockingService.lockTwoAccounts(ACCOUNT_ID_1, ACCOUNT_ID_2)).thenReturn(accounts);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transferService.transfer(ACCOUNT_ID_1, ACCOUNT_ID_2, transferAmount)
        );

        verify(transferRepo, never()).save(any());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertThat(exception.getReason()).contains("Insufficient", "funds");
    }

    @Test
    void transfer_duplicateAccountId() {
        Account duplicateAccount = makeAccount(ACCOUNT_ID_1, JOHN, USD, 300);
        AccountLockingService.AccountPair accounts = new AccountLockingService.AccountPair(
                duplicateAccount,
                duplicateAccount
        );
        BigDecimal transferAmount = money(100);

        when(accountLockingService.lockTwoAccounts(ACCOUNT_ID_1, ACCOUNT_ID_1)).thenReturn(accounts);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transferService.transfer(ACCOUNT_ID_1, ACCOUNT_ID_1, transferAmount)
        );

        verify(transferRepo, never()).save(any());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertThat(exception.getReason()).contains("same", "account");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "null",
            "0",
            "-1",
            "-100.501"
    }, nullValues = "null")
    void transfer_wrongAmount(BigDecimal amount) {
        AccountLockingService.AccountPair accounts = createAccountPair(300, 0);

        when(accountLockingService.lockTwoAccounts(ACCOUNT_ID_1, ACCOUNT_ID_2)).thenReturn(accounts);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transferService.transfer(ACCOUNT_ID_1, ACCOUNT_ID_2, amount)
        );

        verify(transferRepo, never()).save(any());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("amount must be > 0", exception.getReason());
    }

    @Test
    void transfer_currencyMismatch() {
        AccountLockingService.AccountPair accounts = createAccountPair(300, 0, USD, RUB);
        BigDecimal transferAmount = money(100);

        when(accountLockingService.lockTwoAccounts(ACCOUNT_ID_1, ACCOUNT_ID_2)).thenReturn(accounts);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transferService.transfer(ACCOUNT_ID_1, ACCOUNT_ID_2, transferAmount)
        );

        verify(transferRepo, never()).save(any());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("currency mismatch", exception.getReason());
    }

    @ParameterizedTest
    @CsvSource({
            "500000.00, 1.00",
            "499999.00, 2.00",
            "0.00, 500001.00"
    })
    void transfer_dailyLimitExceeded(BigDecimal dailyTransferred, BigDecimal transferAmount) {
        AccountLockingService.AccountPair accounts = createAccountPair(9999999, 0);
        BigDecimal expectedTotal = dailyTransferred.add(transferAmount);

        when(accountLockingService.lockTwoAccounts(ACCOUNT_ID_1, ACCOUNT_ID_2)).thenReturn(accounts);
        when(transferRepo.sumDailyTransfers(eq(ACCOUNT_ID_1), any())).thenReturn(dailyTransferred);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transferService.transfer(ACCOUNT_ID_1, ACCOUNT_ID_2, transferAmount)
        );

        verify(transferRepo, never()).save(any());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        String expectedMessage = String.format(
                "Daily transfer limit exceeded: %.2f / %.2f",
                expectedTotal,
                MoneyConstants.DAILY_TRANSFER_LIMIT
        );
        assertEquals(expectedMessage, exception.getReason());
    }

    @ParameterizedTest
    @CsvSource({
            "100.00, 1.00, 98.99",
            "100.00, 0.01, 99.98",
            "101.00, 100.00, 0.00",
            "100.00, 10.555, 89.33",
            "100.00, 10.123, 89.78",
            "100.00, 55.55, 43.89",
            "1.00, 0.50, 0.49",
            "1.00, 0.45, 0.54"
    })
    void transfer_calculateProblemsSuccess(
            BigDecimal balance,
            BigDecimal transferAmount,
            BigDecimal expectedFromBalance
    ) {
        AccountLockingService.AccountPair accounts = createAccountPair(balance.doubleValue(), 0);
        BigDecimal expectedToBalance = transferAmount.setScale(2, RoundingMode.HALF_UP);

        when(accountLockingService.lockTwoAccounts(ACCOUNT_ID_1, ACCOUNT_ID_2)).thenReturn(accounts);
        when(transferRepo.sumDailyTransfers(any(), any())).thenReturn(ZERO_AMOUNT);
        when(transferRepo.save(any())).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            transfer.setId(DEFAULT_TRANSFER_ID);
            transfer.setCreatedAt(FIXED_TIME);
            return transfer;
        });

        transferService.transfer(ACCOUNT_ID_1, ACCOUNT_ID_2, transferAmount);

        assertEquals(expectedFromBalance, accounts.from().getBalance());
        assertEquals(expectedToBalance, accounts.to().getBalance());
    }

    @Test
    void transferByNames_success() {
        AccountLockingService.AccountPair accounts = createAccountPair(300, 0);
        BigDecimal transferAmount = money(100);
        BigDecimal expectedFee = money(1);
        BigDecimal expectedFromBalance = money(199);
        BigDecimal expectedToBalance = money(100);
        Transfer preparedTransfer = makeTransfer(100, 1);

        when(transferRepo.save(transferCaptor.capture())).thenReturn(preparedTransfer);
        when(transferRepo.sumDailyTransfers(any(), any())).thenReturn(ZERO_AMOUNT);
        when(accountLockingService.lockTwoAccountsByName(JOHN, JANE, USD)).thenReturn(accounts);

        TransferResponse response = transferService.transferByNames(JOHN, JANE, USD, transferAmount);

        assertEquals(transferAmount, response.getAmount());
        assertEquals(expectedFee, response.getFee());
        assertEquals(TransferStatus.COMPLETED, response.getStatus());
        assertEquals(ACCOUNT_ID_1, response.getFromAccountId());
        assertEquals(ACCOUNT_ID_2, response.getToAccountId());

        assertEquals(expectedFromBalance, accounts.from().getBalance());
        assertEquals(expectedToBalance, accounts.to().getBalance());

        Transfer captured = transferCaptor.getValue();
        assertEquals(ACCOUNT_ID_1, captured.getFromAccountId());
        assertEquals(ACCOUNT_ID_2, captured.getToAccountId());
        assertEquals(transferAmount, captured.getAmount());
        assertEquals(expectedFee, captured.getFee());
        assertEquals(TransferStatus.COMPLETED, captured.getStatus());
        assertNull(captured.getId());
        assertNull(captured.getCreatedAt());
    }

    @Test
    void get_success() {
        Transfer transfer = makeTransfer(100, 1);

        when(transferRepo.findById(DEFAULT_TRANSFER_ID)).thenReturn(Optional.of(transfer));

        TransferResponse transferResponse = transferService.get(DEFAULT_TRANSFER_ID);

        assertThat(transferResponse).usingRecursiveComparison().isEqualTo(transfer);
    }

    @Test
    void get_notFound() {
        when(transferRepo.findById(DEFAULT_TRANSFER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transferService.get(DEFAULT_TRANSFER_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void cancel_success() {
        AccountLockingService.AccountPair accounts = createAccountPair(300, 100);
        BigDecimal expectedFromBalance = money(401);
        BigDecimal expectedToBalance = money(0);
        Transfer transfer = Transfer.builder()
                .fromAccountId(ACCOUNT_ID_1)
                .toAccountId(ACCOUNT_ID_2)
                .amount(money(100))
                .createdAt(FIXED_TIME.minusMinutes(4))
                .id(DEFAULT_TRANSFER_ID)
                .fee(money(1))
                .status(TransferStatus.COMPLETED)
                .build();

        when(transferRepo.findByIdForUpdate(DEFAULT_TRANSFER_ID)).thenReturn(Optional.of(transfer));
        when(accountLockingService.lockTwoAccounts(ACCOUNT_ID_1, ACCOUNT_ID_2)).thenReturn(accounts);

        TransferResponse response = transferService.cancel(DEFAULT_TRANSFER_ID);

        assertEquals(TransferStatus.CANCELLED, response.getStatus());
        assertEquals(expectedFromBalance, accounts.from().getBalance());
        assertEquals(expectedToBalance, accounts.to().getBalance());
    }

    @Test
    void cancel_transferNotFound() {
        when(transferRepo.findByIdForUpdate(DEFAULT_TRANSFER_ID)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> transferService.cancel(DEFAULT_TRANSFER_ID)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());

        assertEquals("no such transfer", ex.getReason());
    }

    @Test
    void cancel_cancelledAlready() {
        Transfer transfer = Transfer.builder()
                .id(DEFAULT_TRANSFER_ID)
                .status(TransferStatus.CANCELLED)
                .build();

        when(transferRepo.findByIdForUpdate(DEFAULT_TRANSFER_ID)).thenReturn(Optional.of(transfer));
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> transferService.cancel(DEFAULT_TRANSFER_ID)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        assertEquals("transfer already cancelled", ex.getReason());
    }

    @Test
    void cancel_cancelTimePassed() {
        Transfer transfer = Transfer.builder()
                .id(DEFAULT_TRANSFER_ID)
                .createdAt(FIXED_TIME.minusMinutes(6))
                .status(TransferStatus.COMPLETED)
                .build();

        when(transferRepo.findByIdForUpdate(DEFAULT_TRANSFER_ID)).thenReturn(Optional.of(transfer));
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> transferService.cancel(DEFAULT_TRANSFER_ID)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        assertEquals("5 minutes passed, can't cancel", ex.getReason());
    }

    @Test
    void cancel_insufficientFunds() {
        Transfer transfer = makeTransfer(100,1);
        AccountLockingService.AccountPair accountPair = createAccountPair(200,0);

        when(accountLockingService.lockTwoAccounts(ACCOUNT_ID_1,ACCOUNT_ID_2)).thenReturn(accountPair);
        when(transferRepo.findByIdForUpdate(DEFAULT_TRANSFER_ID)).thenReturn(Optional.of(transfer));
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> transferService.cancel(DEFAULT_TRANSFER_ID)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        assertEquals("Cannot cancel: recipient has insufficient funds", ex.getReason());
    }

    @Test
    void count_success(){
        when(transferRepo.countTransfersByStatus(TransferStatus.COMPLETED)).thenReturn(5L);
        CountResponse transfersCount = transferService.count();
        assertEquals(5L,transfersCount.getCount());
    }
}