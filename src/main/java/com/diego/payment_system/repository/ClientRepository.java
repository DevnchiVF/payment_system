package com.diego.payment_system.repository;

import com.diego.payment_system.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByDni(String dni);
    Optional<Client> findByEmail(String email);
    Optional<Client> findByphoneNumber(String phoneNumber);
}