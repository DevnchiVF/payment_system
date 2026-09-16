package com.diego.payment_system.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public Long getCurrentClientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof ClientPrincipal)) {
            throw new AccessDeniedException("No authenticated client");
        }

        ClientPrincipal principal = (ClientPrincipal) auth.getPrincipal();
        return principal.getClientId();
    }
}
