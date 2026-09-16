package com.diego.payment_system.controller;

import com.diego.payment_system.dto.*;
import com.diego.payment_system.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestParam Long clientId){
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(clientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id){
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<Page<TransactionHistoryResponse>> getHistory(@PathVariable Long id,
                                                                @ModelAttribute TransactionFilterRequest filter,
                                                                @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(accountService.getTransactionHistory(id, filter, pageable));
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<TransactionResponse> transfer(@PathVariable Long id, @RequestBody @Valid TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.ok(accountService.transfer(id, request, idempotencyKey));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<TransactionResponse> deposit(@PathVariable Long id, @RequestBody @Valid DepositRequest request,
                                                   @RequestHeader("Idempotency-Key") String idempotencyKey){
        return ResponseEntity.ok(accountService.deposit(id, request, idempotencyKey));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@PathVariable Long id, @RequestBody @Valid WithdrawRequest request,
                                                    @RequestHeader("Idempotency-Key") String idempotencyKey){
        return ResponseEntity.ok(accountService.withdraw(id, request, idempotencyKey));
    }
}
