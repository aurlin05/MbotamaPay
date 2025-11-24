package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.Transaction;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
        List<Transaction> findBySenderWalletOrReceiverWalletOrderByCreatedAtDesc(Wallet senderWallet,
                        Wallet receiverWallet);

        Page<Transaction> findBySenderWalletOrReceiverWalletOrderByCreatedAtDesc(Wallet senderWallet,
                        Wallet receiverWallet,
                        Pageable pageable);

        List<Transaction> findBySenderWalletUserOrReceiverWalletUserOrderByCreatedAtDesc(User sender, User receiver);

        @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.status = 'SUCCESS'")
        BigDecimal calculateTotalVolume();
        
        java.util.Optional<Transaction> findByReference(String reference);
}
