package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.PaymentRequest;
import com.mbotamapay.backend.entity.PaymentRequestStatus;
import com.mbotamapay.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {
    Page<PaymentRequest> findByRequesterOrderByCreatedAtDesc(User requester, Pageable pageable);

    Page<PaymentRequest> findByPayerAndStatusOrderByCreatedAtDesc(User payer, PaymentRequestStatus status,
            Pageable pageable);
}
