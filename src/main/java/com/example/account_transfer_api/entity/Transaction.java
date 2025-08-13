package com.example.account_transfer_api.entity;

import com.example.account_transfer_api.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(nullable = false)
    private UUID fromAccountId;
    @Column(nullable = false)
    private UUID toAccountId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountDebited; // Amount taken from the source account
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountCredited; // Amount added to the destination account
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal transactionFee;
    @Column(nullable = false, length = 3)
    private String fromCurrency;
    @Column(nullable = false, length = 3)
    private String toCurrency;
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    private Instant createdAt = Instant.now();
}
