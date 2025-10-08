package com.megamart.gateway;

import com.megamart.gateway.client.AuthServiceClient;
import com.megamart.gateway.dto.TokenValidationResponseDto;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.*;
import java.util.Collections;

@Slf4j
@Component
public class JwtAuthenticationFilter implements Filter {

    private final AuthServiceClient authServiceClient;

    public JwtAuthenticationFilter(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        log.debug("Gateway Filter - Processing path: {}", path);

        if ("OPTIONS".equals(httpRequest.getMethod())) {
            log.debug("Gateway Filter - Allowing OPTIONS request");
            chain.doFilter(request, response);
            return;
        }

        if (isPublicPath(httpRequest)) {
            log.debug("Gateway Filter - Allowing public path: {}", path);
            chain.doFilter(request, response);
            return;
        }

        log.debug("Gateway Filter - Protected path, checking auth: {}", path);
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Gateway Filter - No valid auth header for path: {}", path);
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        try {
            TokenValidationResponseDto validationResponse =
                    authServiceClient.validateToken(authHeader);

            if (validationResponse != null && validationResponse.isValid()) {
                log.debug("Gateway Filter - Token valid for user: {}", validationResponse.getUserId());
                // Use the new, complete wrapper to add headers
                HeaderMapRequestWrapper wrappedRequest = new HeaderMapRequestWrapper(httpRequest);
                wrappedRequest.addHeader("X-User-ID", validationResponse.getUserId().toString());
                wrappedRequest.addHeader("X-User-Roles", validationResponse.getRole());
                chain.doFilter(wrappedRequest, response);
            } else {
                log.warn("Gateway Filter - Token invalid for path: {}", path);
                httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            }
        } catch (Exception e) {
            log.error("Gateway Filter - Exception during validation for path {}: {}", path, e.getMessage());
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }

    /**
     * This is the corrected, complete wrapper implementation that makes new headers visible.
     */
    public static class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> headerMap = new HashMap<>();

        public HeaderMapRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            headerMap.put(name.toLowerCase(), value);
        }

        @Override
        public String getHeader(String name) {
            String headerValue = headerMap.get(name.toLowerCase());
            if (headerValue != null) {
                return headerValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>(Collections.list(super.getHeaderNames()));
            names.addAll(headerMap.keySet());
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String headerValue = headerMap.get(name.toLowerCase());
            if (headerValue != null) {
                return Collections.enumeration(Collections.singletonList(headerValue));
            }
            return super.getHeaders(name);
        }
    }

    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        AntPathMatcher pathMatcher = new AntPathMatcher();

        log.debug("Gateway Filter - Checking if public path: {} ({})", path, method);

        if ("GET".equals(method) && (
                pathMatcher.match("/productservice/api/products/**", path) ||
                        pathMatcher.match("/productservice/api/categories/**", path) ||
                        pathMatcher.match("/productservice/api/brands/**", path))) {
            log.debug("Gateway Filter - Matched product service public path");
            return true;
        }

        if (pathMatcher.match("/auth-service/api/auth/login", path) ||
                pathMatcher.match("/auth-service/api/auth/customer/login", path) ||
                pathMatcher.match("/auth-service/api/auth/admin/login", path) ||
                pathMatcher.match("/user-admin-service/api/users/register", path)) {
            log.debug("Gateway Filter - Matched auth/register public path");
            return true;
        }

        // Allow fallback endpoints for circuit breaker
        if (pathMatcher.match("/fallback/**", path)) {
            log.debug("Gateway Filter - Matched fallback public path");
            return true;
        }

        log.debug("Gateway Filter - No public path match found");
        return false;
    }
}