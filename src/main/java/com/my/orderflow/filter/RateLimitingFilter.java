package com.my.orderflow.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.orderflow.dto.error.ErrorResponseDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, IpRequestInfo> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rate-limit.auth.max-requests:5}")
    private int maxRequests;

    @Value("${rate-limit.auth.window-seconds:60}")
    private int windowSeconds;

    @Scheduled(fixedRateString = "${rate-limit.cleanup-interval-ms:300000}")
    public void cleanupExpiredEntries() {
        long now = Instant.now().toEpochMilli();
        requestCounts.entrySet().removeIf(entry ->
                (now - entry.getValue().windowStart) > (windowSeconds * 1000L * 2)
        );
        log.debug("Rate limit cache cleaned, remaining entries: {}", requestCounts.size());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        if (!requestUri.startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        IpRequestInfo info = requestCounts.computeIfAbsent(clientIp, k -> new IpRequestInfo());

        synchronized (info) {
            if (info.isExpired(windowSeconds)) {
                info.reset();
            }

            if (info.count.get() >= maxRequests) {
                log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, requestUri);
                sendRateLimitResponse(request, response, info.getSecondsUntilReset(windowSeconds));
                return;
            }

            info.count.incrementAndGet();
            response.setHeader("X-RateLimit-Remaining", String.valueOf(maxRequests - info.count.get()));
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(HttpServletRequest request, HttpServletResponse response, long retryAfter) throws IOException {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                "Rate limit exceeded. Please try again later.",
                request.getRequestURI(),
                retryAfter
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfter));

        objectMapper.findAndRegisterModules();
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private static class IpRequestInfo {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = Instant.now().toEpochMilli();

        public boolean isExpired(int windowSeconds) {
            long now = Instant.now().toEpochMilli();
            return (now - windowStart) > (windowSeconds * 1000L);
        }

        public void reset() {
            count.set(0);
            windowStart = Instant.now().toEpochMilli();
        }

        public long getSecondsUntilReset(int windowSeconds) {
            long now = Instant.now().toEpochMilli();
            long elapsed = now - windowStart;
            return Math.max(1, windowSeconds - (elapsed / 1000));
        }
    }
}