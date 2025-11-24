package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findById(Long id);
}
