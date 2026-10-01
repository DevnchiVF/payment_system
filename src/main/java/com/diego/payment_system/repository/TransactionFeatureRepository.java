package com.diego.payment_system.repository;

import com.diego.payment_system.domain.TransactionFeature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionFeatureRepository extends JpaRepository<TransactionFeature, Long> {
    Page<TransactionFeature> findByAccountId(Long accountId, Pageable pageable);
}
