package com.example.wallet.transfer;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transfer t WHERE t.id = :id")
    Optional<Transfer> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.status = :status")
    long countTransfersByStatus(@Param("status") TransferStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
            "WHERE t.fromAccountId = :accountId " +
            "AND t.status = 'COMPLETED' " +
            "AND t.createdAt >= :startDate")
    BigDecimal sumDailyTransfers(@Param("accountId") UUID accountId, @Param("startDate") OffsetDateTime startDate);

    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.status = 'COMPLETED' AND t.fromAccountId = :accountId")
    long countOutgoingTransfersById(@Param("accountId") UUID fromId);

    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.status = 'COMPLETED' AND t.toAccountId = :accountId")
    long countIncomingTransfersById(@Param("accountId") UUID toId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
            "WHERE t.fromAccountId = :accountId " +
            "AND t.status = 'COMPLETED'")
    BigDecimal sumOutgoingTransfers(@Param("accountId") UUID fromId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
            "WHERE t.toAccountId = :accountId " +
            "AND t.status = 'COMPLETED'")
    BigDecimal sumIncomingTransfers(@Param("accountId") UUID toId);
}