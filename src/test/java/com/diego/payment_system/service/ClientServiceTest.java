package com.diego.payment_system.service;

import com.diego.payment_system.BaseIntegrationTest;
import com.diego.payment_system.dto.ClientRequest;
import com.diego.payment_system.dto.ClientResponse;
import com.diego.payment_system.exception.BusinessException;
import com.diego.payment_system.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class ClientServiceTest extends BaseIntegrationTest {

    @Autowired
    private ClientService clientService;
    @Autowired
    private ClientRepository clientRepository;

    @BeforeEach
    void clean(){
        clientRepository.deleteAll();
    }

    @Test
    void registerClient_persists_and_returns_response(){
        ClientRequest request = new ClientRequest("12345678", "Diego",
                LocalDate.of(1995, 1, 1), "999000001",
                "diego@test.com", "secret123");

        ClientResponse response = clientService.registerClient(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.email()).isEqualTo("diego@test.com");
    }

    @Test
    void registerClient_rejects_duplicate_dni() {
        ClientRequest request = new ClientRequest(
                "12345678", "Diego", LocalDate.of(1995, 1, 1),
                "999000001", "diego@test.com", "secret123"
        );
        clientService.registerClient(request);

        ClientRequest duplicate = new ClientRequest(
                "12345678", "Otro", LocalDate.of(1990, 1, 1),
                "999000002", "otro@test.com", "secret456"
        );

        assertThatThrownBy(() -> clientService.registerClient(duplicate))
                .isInstanceOf(BusinessException.class).hasMessageContaining("DNI");
    }

}
