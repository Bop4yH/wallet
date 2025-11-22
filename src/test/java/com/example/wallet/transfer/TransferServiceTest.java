package com.example.wallet.transfer;

import com.example.wallet.account.AccountLockingService;
import com.example.wallet.account.AccountRepository;
import com.example.wallet.transfer.dto.TransferResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static com.example.wallet.utils.TestUtils.ACCOUNT_ID_1;
import static com.example.wallet.utils.TestUtils.ACCOUNT_ID_2;
import static com.example.wallet.utils.TestUtils.FIXED_TIME;
import static com.example.wallet.utils.TestUtils.makeAccount;
import static com.example.wallet.utils.TestUtils.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final UUID DEFAULT_TRANSFER_ID = new UUID(69, 69);

    @Captor
    private ArgumentCaptor<Transfer> transferCaptor;

    @Mock
    private AccountRepository accountRepo;

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

    @Test
    void transfer() {
        AccountLockingService.AccountPair accounts = new AccountLockingService.AccountPair(
                makeAccount(ACCOUNT_ID_1, "John", "USD", 300),
                makeAccount(ACCOUNT_ID_2, "Jane", "USD", 0)
        );
        Transfer transfer = makeTransfer(100, 1);
        when(accountLockingService.lockTwoAccounts(ACCOUNT_ID_1, ACCOUNT_ID_2)).thenReturn(accounts);
        when(transferRepo.sumDailyTransfers(any(), any())).thenReturn(money(0));
        when(transferRepo.save(transferCaptor.capture())).thenReturn(transfer);

        TransferResponse response = transferService.transfer(ACCOUNT_ID_1, ACCOUNT_ID_2, money(100));

        BigDecimal fee = (money(1));
        assertEquals(money(100), response.getAmount());
        assertEquals(TransferStatus.COMPLETED, response.getStatus());
        assertEquals(ACCOUNT_ID_1, response.getFromAccountId());
        assertEquals(ACCOUNT_ID_2, response.getToAccountId());
        assertEquals(fee, response.getFee());
        assertEquals(accounts.from().getBalance(), money(199));
        assertEquals(accounts.to().getBalance(), money(100));

        Transfer captured = transferCaptor.getValue();
        assertEquals(ACCOUNT_ID_1, captured.getFromAccountId());
        assertEquals(ACCOUNT_ID_2, captured.getToAccountId());
        assertEquals(money(100), captured.getAmount());
        assertEquals(fee, captured.getFee());
        assertEquals(TransferStatus.COMPLETED, captured.getStatus());
    }
}