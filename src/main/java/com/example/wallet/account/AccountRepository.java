package com.example.wallet.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Находит и блокирует счёт для обновления.
     * Используется для предотвращения race conditions при финансовых операциях.
     *
     * @param id идентификатор счёта
     * @return Optional с заблокированным счётом или empty если не найден
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);

    Optional<Account> findByOwnerNameIgnoreCaseAndCurrency(String ownerName, String currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE LOWER(a.ownerName) = LOWER(:name) AND a.currency = :currency")
    Optional<Account> findByNameAndCurrencyForUpdate(
            @Param("name") String name,
            @Param("currency") String currency
    );
}