package com.example.wallet.transfer;

import com.example.wallet.common.MoneyConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_transfer_from_created", columnList = "from_account_id, created_at"),
        @Index(name = "idx_transfer_to", columnList = "to_account_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "idempotency_key", unique = true)
    private UUID idempotencyKey;

    @Column(name = "from_account_id", nullable = false, columnDefinition = "uuid")
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false, columnDefinition = "uuid")
    private UUID toAccountId;

    @Column(name = "amount", nullable = false, precision = MoneyConstants.PRECISION, scale = MoneyConstants.SCALE)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "fee", nullable = false, precision = MoneyConstants.PRECISION, scale = MoneyConstants.SCALE)
    private BigDecimal fee;

    @PrePersist
    void prePersist() {
        if (amount != null) amount = amount.setScale(MoneyConstants.SCALE, RoundingMode.HALF_UP);
        if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (status == null) status = TransferStatus.COMPLETED;
        if (fee == null) fee = BigDecimal.ZERO.setScale(MoneyConstants.SCALE, RoundingMode.HALF_UP);
    }
}