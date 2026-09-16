package com.diego.payment_system.service;

import com.diego.payment_system.domain.Client;
import com.diego.payment_system.dto.ClientRequest;
import com.diego.payment_system.dto.ClientResponse;
import com.diego.payment_system.exception.BusinessException;
import com.diego.payment_system.exception.ResourceNotFoundException;
import com.diego.payment_system.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientResponse registerClient(ClientRequest request){
        if (clientRepository.findByDni(request.dni()).isPresent()) {
            throw new BusinessException("DNI already registered");
        }
        if (clientRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException("Email already registered");
        }
        if(clientRepository.findByphoneNumber(request.phoneNumber()).isPresent()){
            throw new BusinessException("phone number already registered");
        }
        Client client = new Client();
        client.setDni(request.dni());
        client.setName(request.name());
        client.setBirthDate(request.birthDate());
        client.setPhoneNumber(request.phoneNumber());
        client.setEmail(request.email());
        client.setPassword(passwordEncoder.encode(request.password()));
        return toResponse(clientRepository.save(client));
    }

    public ClientResponse getClient(Long id){
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
        return toResponse(client);
    }

    private ClientResponse toResponse(Client client){
        return new ClientResponse(client.getId(), client.getDni(), client.getName(),
                client.getBirthDate(),client.getPhoneNumber(),client.getEmail());
    }
}
