package com.diego.payment_system.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<Long, Bucket> transferBuckets = new ConcurrentHashMap<>();
    private final Map<Long, Bucket> depositBuckets = new ConcurrentHashMap<>();

    public ConsumptionProbe tryConsumeTransfer(Long accountId) {
        return resolveTransferBucket(accountId).tryConsumeAndReturnRemaining(1);
    }

    private Bucket resolveTransferBucket(Long accountId) {
        return transferBuckets.computeIfAbsent(accountId, id ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(5)
                                .refillGreedy(5, Duration.ofMinutes(1))
                                .build())
                        .build()
        );
    }

    public ConsumptionProbe tryConsumeDeposit(Long accountId) {
        return resolveDepositBucket(accountId).tryConsumeAndReturnRemaining(1);
    }

    private Bucket resolveDepositBucket(Long accountId) {
        return depositBuckets.computeIfAbsent(accountId, id ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(3)
                                .refillGreedy(3, Duration.ofMinutes(1))
                                .build())
                        .build()
        );
    }
}
