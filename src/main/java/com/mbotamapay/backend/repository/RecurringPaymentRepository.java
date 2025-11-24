package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.RecurringPayment;
import com.mbotamapay.backend.entity.RecurringPayment.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecurringPaymentRepository extends JpaRepository<RecurringPayment, Long> {
    List<RecurringPayment> findByStatusAndNextExecutionDateBefore(Status status, LocalDateTime date);

    List<RecurringPayment> findByUserId(Long userId);
}
