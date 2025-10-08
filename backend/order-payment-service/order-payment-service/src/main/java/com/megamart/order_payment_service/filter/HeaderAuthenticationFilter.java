package com.megamart.order_payment_service.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String userId = request.getHeader("X-User-ID");
        String userRoles = request.getHeader("X-User-Roles");
        
       log.info("Order Service HeaderAuthenticationFilter - X-User-ID: " + userId);
        log.info("Order Service HeaderAuthenticationFilter - X-User-Roles: " + userRoles);
        
        if (userId != null && userRoles != null) {
            List<SimpleGrantedAuthority> authorities = Arrays.stream(userRoles.split(","))
                    .map(String::trim)
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            
            log.info("Order Service HeaderAuthenticationFilter - Created authorities: " + authorities);
            
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.info("Order Service HeaderAuthenticationFilter - Authentication set successfully");
        } else {
           log.info("Order Service HeaderAuthenticationFilter - Missing headers, no authentication set");
        }
        
        filterChain.doFilter(request, response);
    }
}