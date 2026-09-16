package com.diego.payment_system.service;

import com.diego.payment_system.domain.*;
import com.diego.payment_system.dto.*;
import com.diego.payment_system.exception.InsufficientFundsException;
import com.diego.payment_system.exception.RateLimitException;
import com.diego.payment_system.exception.ResourceNotFoundException;
import com.diego.payment_system.repository.AccountRepository;
import com.diego.payment_system.repository.ClientRepository;
import com.diego.payment_system.repository.TransactionRepository;
import com.diego.payment_system.security.SecurityUtils;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;
    private final RateLimiterService rateLimiterService;

    public AccountResponse createAccount(Long clientId){

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

        Account account = new Account();
        account.setClient(client);
        account.setPhoneNumber(client.getPhoneNumber());
        account.setBalance(BigDecimal.ZERO);
        return toResponse(accountRepository.save(account));
    }

    public AccountResponse getAccount(Long id){

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        return toResponse(account);
    }

    public Page<TransactionHistoryResponse> getTransactionHistory(Long accountId, TransactionFilterRequest filter, Pageable pageable) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        checkOwnership(account);

        Specification<Transaction> spec = Specification
                .where(TransactionSpecification.belongsToAccount(accountId))
                .and(TransactionSpecification.fromDate(filter.from()))
                .and(TransactionSpecification.toDate(filter.to()))
                .and(TransactionSpecification.ofType(filter.type()))
                .and(TransactionSpecification.amountBetween(filter.minAmount(), filter.maxAmount()));

        return transactionRepository.findAll(spec, pageable)
                .map(tx -> toHistoryResponse(tx, accountId));
    }

    @Transactional
    public TransactionResponse transfer(Long id, TransferRequest request, String idempotencyKey) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toTransactionResponse(existing.get());
        }
        ConsumptionProbe probe = rateLimiterService.tryConsumeTransfer(id);
        if (!probe.isConsumed()) {
            long secondsToWait = probe.getNanosToWaitForRefill() / 1_000_000_000;
            throw new RateLimitException(
                    "Transfer limit exceeded. Try again in " + secondsToWait + " seconds."
            );
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        checkOwnership(account);
        Long targetId = accountRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.phoneNumber()))
                .getId();

        Account first  = accountRepository.findByIdWithLock(Math.min(id, targetId))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + Math.min(id, targetId)));
        Account second = accountRepository.findByIdWithLock(Math.max(id, targetId))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + Math.max(id, targetId)));

        Account source = first.getId().equals(id) ? first : second;
        Account target = first.getId().equals(id) ? second : first;

        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("insufficient funds");
        }

        source.setBalance(source.getBalance().subtract(request.amount()));
        target.setBalance(target.getBalance().add(request.amount()));

        Transaction tx = new Transaction();
        tx.setSourceAccount(source);
        tx.setTargetAccount(target);
        tx.setAmount(request.amount());
        tx.setType(TransactionType.TRANSFER);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setTimestamp(LocalDateTime.now());

        try {
            return toTransactionResponse(transactionRepository.save(tx));
        } catch (DataIntegrityViolationException e) {
            return toTransactionResponse(
                    transactionRepository.findByIdempotencyKey(idempotencyKey).orElseThrow()
            );
        }
    }

    @Transactional
    public TransactionResponse deposit(Long id, DepositRequest depositRequest, String idempotencyKey) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toTransactionResponse(existing.get());
        }
        ConsumptionProbe probe = rateLimiterService.tryConsumeDeposit(id);
        if (!probe.isConsumed()) {
            long secondsToWait = probe.getNanosToWaitForRefill() / 1_000_000_000;
            throw new RateLimitException(
                    "Deposit limit exceeded. Try again in " + secondsToWait + " seconds."
            );
        }
        Account account = accountRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        checkOwnership(account);
        account.setBalance(account.getBalance().add(depositRequest.amount()));
        accountRepository.save(account);

        Transaction tx = new Transaction();
        tx.setSourceAccount(null);
        tx.setTargetAccount(account);
        tx.setAmount(depositRequest.amount());
        tx.setType(TransactionType.DEPOSIT);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setTimestamp(LocalDateTime.now());

        try {
            return toTransactionResponse(transactionRepository.save(tx));
        } catch (DataIntegrityViolationException e) {
            return toTransactionResponse(
                    transactionRepository.findByIdempotencyKey(idempotencyKey).orElseThrow()
            );
        }
    }

    @Transactional
    public TransactionResponse withdraw(Long id, WithdrawRequest withdrawRequest, String idempotencyKey) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toTransactionResponse(existing.get());
        }
        Account account = accountRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        checkOwnership(account);
        if (account.getBalance().compareTo(withdrawRequest.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        account.setBalance(account.getBalance().subtract(withdrawRequest.amount()));
        accountRepository.save(account);

        Transaction tx = new Transaction();
        tx.setSourceAccount(account);
        tx.setTargetAccount(null);
        tx.setAmount(withdrawRequest.amount());
        tx.setType(TransactionType.WITHDRAWAL);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setTimestamp(LocalDateTime.now());

        try {
            return toTransactionResponse(transactionRepository.save(tx));
        } catch (DataIntegrityViolationException e) {
            return toTransactionResponse(
                    transactionRepository.findByIdempotencyKey(idempotencyKey).orElseThrow()
            );
        }
    }

    private AccountResponse toResponse(Account account){
        ClientResponse clientResponse = new ClientResponse(
                account.getClient().getId(),
                account.getClient().getDni(),
                account.getClient().getName(),
                account.getClient().getBirthDate(),
                account.getClient().getPhoneNumber(),
                account.getClient().getEmail()
        );

        return new AccountResponse(account.getId(), clientResponse, account.getBalance());
    }

    private TransactionResponse toTransactionResponse(Transaction tx) {
        return new TransactionResponse(tx.getId(), tx.getAmount(), tx.getType(), tx.getStatus(),
                tx.getIdempotencyKey(),
                tx.getTimestamp()
        );
    }

    private TransactionHistoryResponse toHistoryResponse(Transaction tx, Long accountId) {
        String direction = tx.getTargetAccount().getId().equals(accountId)
                ? "INBOUND"
                : "OUTBOUND";
        return new TransactionHistoryResponse(
                tx.getId(), tx.getAmount(), tx.getType(),
                tx.getStatus(), direction, tx.getTimestamp()
        );
    }

    private void checkOwnership(Account account) {
        Long currentClientId = securityUtils.getCurrentClientId();
        if (!account.getClient().getId().equals(currentClientId)) {
            throw new AccessDeniedException("You don't own this account");
        }
    }

}
