package com.mbotamapay.backend.service;

import com.mbotamapay.backend.entity.RecurringPayment;
import com.mbotamapay.backend.entity.RecurringPayment.Frequency;
import com.mbotamapay.backend.entity.RecurringPayment.Status;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.repository.RecurringPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringPaymentService {

    private final RecurringPaymentRepository recurringPaymentRepository;
    private final TransactionService transactionService;

    // @Scheduled moved to RecurringPaymentScheduler
    @Transactional
    public void processRecurringPayments() {
        log.info("Starting recurring payment processing...");
        List<RecurringPayment> duePayments = recurringPaymentRepository.findByStatusAndNextExecutionDateBefore(
                Status.ACTIVE, LocalDateTime.now());

        for (RecurringPayment payment : duePayments) {
            try {
                processPayment(payment);
            } catch (Exception e) {
                log.error("Failed to process recurring payment ID: " + payment.getId(), e);
            }
        }
        log.info("Finished recurring payment processing. Processed: " + duePayments.size());
    }

    private void processPayment(RecurringPayment payment) {
        // Execute transaction
        transactionService.sendMoneyByEmail(
                payment.getUser(),
                payment.getRecipientEmail(),
                payment.getAmount(),
                "Recurring Payment: " + (payment.getDescription() != null ? payment.getDescription() : ""));

        // Update next execution date
        updateNextExecutionDate(payment);
        recurringPaymentRepository.save(payment);
    }

    private void updateNextExecutionDate(RecurringPayment payment) {
        LocalDateTime next = payment.getNextExecutionDate();
        switch (payment.getFrequency()) {
            case DAILY -> next = next.plusDays(1);
            case WEEKLY -> next = next.plusWeeks(1);
            case MONTHLY -> next = next.plusMonths(1);
        }
        payment.setNextExecutionDate(next);
    }

    public RecurringPayment createRecurringPayment(User user, String recipientEmail, BigDecimal amount,
            Frequency frequency, String description) {
        RecurringPayment payment = RecurringPayment.builder()
                .user(user)
                .recipientEmail(recipientEmail)
                .amount(amount)
                .frequency(frequency)
                .status(Status.ACTIVE)
                .nextExecutionDate(calculateNextDate(frequency))
                .description(description)
                .build();
        return recurringPaymentRepository.save(payment);
    }

    private LocalDateTime calculateNextDate(Frequency frequency) {
        LocalDateTime now = LocalDateTime.now();
        return switch (frequency) {
            case DAILY -> now.plusDays(1);
            case WEEKLY -> now.plusWeeks(1);
            case MONTHLY -> now.plusMonths(1);
        };
    }

    public List<RecurringPayment> getUserRecurringPayments(User user) {
        return recurringPaymentRepository.findByUserId(user.getId());
    }

    public void cancelRecurringPayment(Long id, User user) {
        RecurringPayment payment = recurringPaymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        payment.setStatus(Status.CANCELLED);
        recurringPaymentRepository.save(payment);
    }
}
