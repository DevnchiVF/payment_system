package com.diego.payment_system.service;

import com.diego.payment_system.BaseIntegrationTest;
import com.diego.payment_system.domain.Account;
import com.diego.payment_system.domain.Client;
import com.diego.payment_system.dto.AccountResponse;
import com.diego.payment_system.dto.DepositRequest;
import com.diego.payment_system.dto.WithdrawRequest;
import com.diego.payment_system.exception.InsufficientFundsException;
import com.diego.payment_system.repository.AccountRepository;
import com.diego.payment_system.repository.ClientRepository;
import com.diego.payment_system.repository.TransactionRepository;
import com.diego.payment_system.security.ClientPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class AccountServiceTest extends BaseIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp(){
        SecurityContextHolder.clearContext();
        jdbcTemplate.execute("TRUNCATE transactions, accounts, clients RESTART IDENTITY CASCADE");
    }

    @Test
    void createAccount_persists_for_authenticated_owner(){
        Client client = createAndSaveClient("12345678", "diego@test.com");
        authenticateClient(client.getId(), client.getEmail());
        AccountResponse response = accountService.createAccount(client.getId());

        assertThat(response.id()).isNotNull();
        Account accountInDb = accountRepository.findById(response.id()).orElseThrow();

        assertThat(accountInDb.getClient().getId()).isEqualTo(client.getId());
        assertThat(accountInDb.getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(accountInDb.getPhoneNumber()).isEqualTo(client.getPhoneNumber());
    }

    @Test
    void deposit_increases_balance_in_database(){
        Client client = createAndSaveClient("87654321", "deposito@test.com");
        authenticateClient(client.getId(), client.getEmail());
        AccountResponse accountResponse = accountService.createAccount(client.getId());
        BigDecimal depositAmount = new BigDecimal("150.50");
        DepositRequest request = new DepositRequest(depositAmount);
        String idempotencyKey = UUID.randomUUID().toString();
        accountService.deposit(client.getId(), request, idempotencyKey);
        Account accountInDb = accountRepository.findById(client.getId()).orElseThrow();
        assertThat(accountInDb.getBalance()).isEqualByComparingTo("150.50");
    }

    @Test
    void withdraw_insufficient_funds_throws_exception_and_does_not_alter_db_balance(){
        Client client = createAndSaveClient("11223344", "retiro@test.com");
        authenticateClient(client.getId(), client.getEmail());
        AccountResponse createdAccount = accountService.createAccount(client.getId());
        accountService.deposit(createdAccount.id(), new DepositRequest(new BigDecimal("50.00")),
                UUID.randomUUID().toString());
        WithdrawRequest failedWithdraw =new WithdrawRequest(new BigDecimal("200.00"));
        String idempotencyKey = UUID.randomUUID().toString();
        assertThatThrownBy(() -> accountService
                .withdraw(createdAccount.id(), failedWithdraw, idempotencyKey))
                .isInstanceOf(InsufficientFundsException.class);
        Account accountInDb = accountRepository.findById(createdAccount.id()).orElseThrow();
        assertThat(accountInDb.getBalance()).isEqualByComparingTo("50.00");

    }

    private void authenticateClient(Long clientId, String email) {
        ClientPrincipal principal = new ClientPrincipal(clientId, email, "hashedpass",
                List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
        );
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Client createAndSaveClient(String dni, String email) {
        Client client = new Client();
        client.setDni(dni);
        client.setName("Diego");
        client.setPassword("$2a$10$hashedfakepassword");
        client.setBirthDate(LocalDate.of(1995, 1, 1));
        client.setPhoneNumber("999" + dni.substring(0, 6));
        client.setEmail(email);
        return clientRepository.save(client);
    }
}
