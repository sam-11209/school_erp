package com.schoolerp.school_erp.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // Multi-tier bucket storage
    private final ConcurrentHashMap<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenBucket> userBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenBucket> tenantBuckets = new ConcurrentHashMap<>();

    // Rate configurations (capacity, refill rate per second)
    private static final int IP_CAPACITY = 60;
    private static final double IP_REFILL_RATE = 1.0; // 1 token per second = 60/min

    private static final int USER_CAPACITY = 100;
    private static final double USER_REFILL_RATE = 1.67; // ~100/min

    private static final int TENANT_CAPACITY = 1000;
    private static final double TENANT_REFILL_RATE = 16.67; // ~1000/min

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. IP-based Check
        String clientIp = httpRequest.getRemoteAddr();
        TokenBucket ipBucket = ipBuckets.computeIfAbsent(clientIp, k -> new TokenBucket(IP_CAPACITY, IP_REFILL_RATE));
        if (!ipBucket.consume()) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            sendTooManyRequests(httpResponse, "IP rate limit exceeded");
            return;
        }

        // 2. User-based Check (extracted via header for simplicity during auth setup)
        String userId = httpRequest.getHeader("X-User-ID");
        if (userId != null && !userId.trim().isEmpty()) {
            TokenBucket userBucket = userBuckets.computeIfAbsent(userId, k -> new TokenBucket(USER_CAPACITY, USER_REFILL_RATE));
            if (!userBucket.consume()) {
                log.warn("Rate limit exceeded for User: {}", userId);
                sendTooManyRequests(httpResponse, "User rate limit exceeded");
                return;
            }
        }

        // 3. Tenant-based Check
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            TokenBucket tenantBucket = tenantBuckets.computeIfAbsent(tenantId.toString(), k -> new TokenBucket(TENANT_CAPACITY, TENANT_REFILL_RATE));
            if (!tenantBucket.consume()) {
                log.warn("Rate limit exceeded for Tenant School: {}", tenantId);
                sendTooManyRequests(httpResponse, "Tenant rate limit exceeded");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void sendTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"Too Many Requests\", \"message\": \"%s\"}", message));
    }

    // Thread-safe Token Bucket Implementation
    private static class TokenBucket {
        private final long capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private long lastRefillTimestamp;

        public TokenBucket(long capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean consume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsedSeconds = (now - lastRefillTimestamp) / 1000.0;
            lastRefillTimestamp = now;

            tokens = Math.min(capacity, tokens + (elapsedSeconds * refillRatePerSecond));
        }
    }
}
