package com.schoolerp.school_erp.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantHeader = httpRequest.getHeader(TENANT_HEADER);

        if (tenantHeader != null && !tenantHeader.trim().isEmpty()) {
            try {
                UUID tenantId = UUID.fromString(tenantHeader);
                TenantContext.setCurrentTenant(tenantId);
                log.debug("Tenant Context set to: {}", tenantId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid tenant UUID format in header: {}", tenantHeader);
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            log.debug("Tenant Context cleared");
        }
    }
}
