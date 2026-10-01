package com.diego.payment_system.service;

import com.diego.payment_system.domain.TransactionFeature;
import com.diego.payment_system.domain.TransactionStatus;
import com.diego.payment_system.domain.TransactionType;
import com.diego.payment_system.domain.event.TransactionCompletedEvent;
import com.diego.payment_system.repository.TransactionFeatureRepository;
import com.diego.payment_system.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudFeatureConsumer {

    private final TransactionRepository transactionRepository;
    private final TransactionFeatureRepository featureRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(TransactionCompletedEvent event) {
        log.info("Building features for transactionId={}", event.transactionId());

        LocalDateTime fiveMinutesAgo = event.timestamp().minusMinutes(5);
        long recentCount = transactionRepository.countRecentTransfers(
                event.sourceAccountId(), fiveMinutesAgo, event.timestamp(),
                TransactionType.TRANSFER, TransactionStatus.COMPLETED);

        List<BigDecimal> lastAmounts = transactionRepository.findLastTransferAmounts(
                event.sourceAccountId(),
                event.transactionId(),
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                PageRequest.of(0, 10)
        );

        BigDecimal ratio = calculateAmountRatio(event.amount(), lastAmounts);

        TransactionFeature feature = new TransactionFeature();
        feature.setTransactionId(event.transactionId());
        feature.setAccountId(event.sourceAccountId());
        feature.setAmount(event.amount());
        feature.setHourOfDay(event.timestamp().getHour());
        feature.setRecentTxCount(recentCount);
        feature.setAmountToAvgRatio(ratio);
        feature.setCreatedAt(LocalDateTime.now());

        featureRepository.save(feature);
        log.info("Features saved for transactionId={} recentCount={} ratio={}",
                event.transactionId(), recentCount, ratio);
    }

    private BigDecimal calculateAmountRatio(BigDecimal currentAmount, List<BigDecimal> previousAmounts) {
        if (previousAmounts.isEmpty()) {
            return new BigDecimal("1.0000");
        }

        BigDecimal sum = previousAmounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(previousAmounts.size()), 4, RoundingMode.HALF_UP);

        if (average.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("1.0000");
        }

        return currentAmount.divide(average, 4, RoundingMode.HALF_UP);
    }
}
