package com.example.wallet.mapper;

import com.example.wallet.account.dto.AccountStatisticsResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    AccountStatisticsResponseNotificationDto mapToAccountStatisticsResponseNotificationDto(
            AccountStatisticsResponse response
    );
}
