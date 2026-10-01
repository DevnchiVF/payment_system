package com.diego.payment_system.repository;

import com.diego.payment_system.domain.Transaction;
import com.diego.payment_system.domain.TransactionStatus;
import com.diego.payment_system.domain.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByIdempotencyKey(String key);

    @Query("""
    SELECT COUNT(t) FROM Transaction t
    WHERE t.sourceAccount.id = :accountId
    AND t.type = :type
    AND t.status = :status
    AND t.timestamp BETWEEN :from AND :to
""")
    long countRecentTransfers(
            @Param("accountId") Long accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status
    );

    @Query("""
        SELECT t.amount FROM Transaction t
        WHERE t.sourceAccount.id = :accountId
        AND t.type = :type
        AND t.status = :status
        AND t.id != :currentTxId
        ORDER BY t.timestamp DESC
    """)
    List<BigDecimal> findLastTransferAmounts(
            @Param("accountId") Long accountId,
            @Param("currentTxId") Long currentTxId,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            Pageable pageable
    );
}
