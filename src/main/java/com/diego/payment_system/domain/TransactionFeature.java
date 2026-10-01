package com.diego.payment_system.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_features")
@Getter @Setter
@NoArgsConstructor
public class TransactionFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long transactionId;
    private Long accountId;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    private int hourOfDay;
    private long recentTxCount;

    @Column(precision = 12, scale = 4)
    private BigDecimal amountToAvgRatio;

    private LocalDateTime createdAt;
}