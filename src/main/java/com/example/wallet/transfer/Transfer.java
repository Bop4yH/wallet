package com.example.wallet.transfer;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "transfers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "from_account_id", nullable = false, columnDefinition = "uuid")
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false, columnDefinition = "uuid")
    private UUID toAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (amount != null) amount = amount.setScale(2, RoundingMode.HALF_UP);
        if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (status == null) status = "COMPLETED";
    }
}