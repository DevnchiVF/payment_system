package com.diego.payment_system.service;

import com.diego.payment_system.domain.Client;
import com.diego.payment_system.dto.AuthRequest;
import com.diego.payment_system.dto.AuthResponse;
import com.diego.payment_system.repository.ClientRepository;
import com.diego.payment_system.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final ClientRepository clientRepository;
    private final JwtService jwtService;

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        Client client = clientRepository.findByEmail(request.email()).orElseThrow();
        return new AuthResponse(jwtService.generateToken(client));
    }
}
