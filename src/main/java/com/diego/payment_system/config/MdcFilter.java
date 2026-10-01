package com.diego.payment_system.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String traceId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put("traceId", traceId);

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String externalTrace = httpRequest.getHeader("X-Trace-Id");
            if (externalTrace != null) {
                MDC.put("traceId", externalTrace);
            }

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}