package com.diego.payment_system.controller;

import com.diego.payment_system.dto.TransactionFeatureResponse;
import com.diego.payment_system.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/features")
@RequiredArgsConstructor
public class FeatureController {

    private final AccountService accountService;

    @GetMapping("/{accountId}")
    public ResponseEntity<Page<TransactionFeatureResponse>> getFeatures(
            @PathVariable Long accountId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(accountService.getFeatures(accountId, pageable));
    }
}
