package com.megamart.useradminserver.repository;

import com.megamart.useradminserver.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUser_Id(Long userId);
    void deleteByUser_IdAndId(Long userId, Long id);
}