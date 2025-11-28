package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.Role;
import com.mbotamapay.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByReferralCode(String referralCode);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByReferralCode(String referralCode);

    long countByActiveTrue();

    List<User> findByRole(Role role);
}
