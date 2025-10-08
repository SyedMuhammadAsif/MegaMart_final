package com.megamart.useradminserver.repository;

import com.megamart.useradminserver.entity.UserPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPaymentMethodRepository extends JpaRepository<UserPaymentMethod, Long> {
    List<UserPaymentMethod> findByUser_Id(Long userId);
    void deleteByUser_IdAndId(Long userId, Long id);
}