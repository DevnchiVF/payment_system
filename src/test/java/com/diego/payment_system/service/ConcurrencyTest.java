package com.diego.payment_system.service;

import com.diego.payment_system.BaseIntegrationTest;
import com.diego.payment_system.domain.Account;
import com.diego.payment_system.domain.Client;
import com.diego.payment_system.dto.DepositRequest;
import com.diego.payment_system.repository.AccountRepository;
import com.diego.payment_system.repository.ClientRepository;
import com.diego.payment_system.security.ClientPrincipal;
import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConcurrencyTest extends BaseIntegrationTest {

    @Autowired private AccountService accountService;
    @Autowired private ClientRepository clientRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoBean private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp(){
        jdbcTemplate.execute("TRUNCATE transactions, accounts, clients RESTART IDENTITY CASCADE");
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(rateLimiterService.tryConsumeDeposit(any())).thenReturn(probe);
    }

    @Test
    void concurrent_deposits_should_not_lose_updates() throws InterruptedException{
        Client client = createClient("11111111", "concurrent@test.com");
        authenticateClient(client.getId(), client.getEmail());
        Long accountId = accountService.createAccount(client.getId()).id();

        int threads = 10;
        BigDecimal depositAmount = new BigDecimal("100.00");
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                authenticateClient(client.getId(), client.getEmail());
                try {
                    accountService.deposit(
                            accountId,
                            new DepositRequest(depositAmount),
                            "key-concurrent-" + index
                    );
                } catch (Exception e) {
                    System.err.println("Thread " + index + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getBalance())
                .as("All 10 concurrent deposits of 100 should sum to 1000")
                .isEqualByComparingTo("1000.00");
    }

    private Client createClient(String dni, String email) {
        Client client = new Client();
        client.setDni(dni);
        client.setName("Diego");
        client.setPassword("$2a$10$hashedfakepassword");
        client.setBirthDate(LocalDate.of(1995, 1, 1));
        client.setPhoneNumber("999" + dni.substring(0, 6));
        client.setEmail(email);
        return clientRepository.save(client);
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
}