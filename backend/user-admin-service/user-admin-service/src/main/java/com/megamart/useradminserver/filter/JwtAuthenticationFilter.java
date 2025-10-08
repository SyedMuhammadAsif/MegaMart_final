package com.megamart.useradminserver.filter;

import com.megamart.useradminserver.client.AuthServiceClient;
import com.megamart.useradminserver.dto.TokenValidationResponseDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthServiceClient authServiceClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/uploads/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {

               TokenValidationResponseDto validationResponse = authServiceClient.validateToken(authHeader);

                if (validationResponse != null && validationResponse.isValid()) {

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            validationResponse.getEmail(), // The actual user principal
                            null,
                            List.of(new SimpleGrantedAuthority(validationResponse.getRole()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("JWT Filter - Successfully authenticated user: {}", validationResponse.getEmail());
                }
            } catch (Exception e) {
                log.error("JWT Filter - Error during token validation: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}