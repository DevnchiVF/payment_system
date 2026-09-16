package com.diego.payment_system.controller;

import com.diego.payment_system.dto.ClientRequest;
import com.diego.payment_system.dto.ClientResponse;
import com.diego.payment_system.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    public ResponseEntity<ClientResponse> registerClient(@RequestBody @Valid ClientRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.registerClient(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable Long id){
        return ResponseEntity.ok(clientService.getClient(id));
    }
}
