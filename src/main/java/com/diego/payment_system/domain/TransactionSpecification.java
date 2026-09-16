package com.diego.payment_system.domain;

import com.diego.payment_system.exception.BusinessException;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionSpecification {

    public static Specification<Transaction> belongsToAccount(Long accountId) {
        return (root, query, cb) ->
                cb.or(
                        cb.equal(root.get("sourceAccount").get("id"), accountId),
                        cb.equal(root.get("targetAccount").get("id"), accountId)
                );
    }

    public static Specification<Transaction> fromDate(LocalDateTime from) {
        return (root, query, cb) ->
                from == null ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("timestamp"), from);
    }

    public static Specification<Transaction> toDate(LocalDateTime to) {
        return (root, query, cb) ->
                to == null ? cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("timestamp"), to);
    }

    public static Specification<Transaction> ofType(TransactionType type) {
        return (root, query, cb) ->
                type == null ? cb.conjunction()
                        : cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> amountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min != null && max != null && min.compareTo(max) > 0) {
                throw new BusinessException("minAmount cannot be greater than maxAmount");
            }
            if (min == null && max == null) {
                return cb.conjunction();
            }
            if (min != null && max != null) {
                return cb.between(root.get("amount"), min, max);
            }
            if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("amount"), min);
            }
            return cb.lessThanOrEqualTo(root.get("amount"), max);
        };
    }
}