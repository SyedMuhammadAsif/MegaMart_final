package com.megamart.authservice.repository;

import com.megamart.authservice.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
    AuthUser findByEmail(String email);
    boolean existsByEmail(String email);
}