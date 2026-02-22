package com.example.wallet.mapper;

import com.example.wallet.account.Account;
import com.example.wallet.account.dto.AccountResponse;
import com.example.wallet.account.dto.AccountStatisticsResponse;
import com.example.wallet.account.dto.BalanceResponse;
import com.example.wallet.event.TransferCompletedEvent;
import com.example.wallet.transfer.Transfer;
import com.example.wallet.transfer.dto.TransferResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    AccountResponse toAccountResponse(Account account);

    BalanceResponse toBalanceResponse(Account account);

    TransferResponse toTransferResponse(Transfer transfer);

    @Mapping(target = "transferId", source = "id")
    TransferCompletedEvent toTransferEvent(Transfer transfer);

    AccountStatisticsResponseNotificationDto toNotificationDto(
            AccountStatisticsResponse response
    );
}
