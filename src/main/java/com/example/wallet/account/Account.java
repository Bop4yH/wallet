package com.example.wallet.account;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "accounts",
        indexes = {
                @Index(name = "idx_owner_currency", columnList = "owner_name, currency")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_owner_currency",
                        columnNames = {"owner_name", "currency"}
                )
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (currency != null) currency = currency.toUpperCase();
        if (balance == null) balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}

