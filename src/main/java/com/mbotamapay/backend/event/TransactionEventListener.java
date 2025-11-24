package com.mbotamapay.backend.event;

import com.mbotamapay.backend.entity.NotificationType;
import com.mbotamapay.backend.entity.Transaction;
import com.mbotamapay.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {

    private final NotificationService notificationService;

    @EventListener
    @Async
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        Transaction transaction = event.getTransaction();

        log.info("Handling transaction completed event: {}", transaction.getReference());

        // Notify sender
        if (transaction.getSenderWallet() != null) {
            notificationService.sendNotification(
                    transaction.getSenderWallet().getUser(),
                    "Payment Sent",
                    String.format("You sent %.2f XAF to %s",
                            transaction.getAmount(),
                            transaction.getReceiverWallet().getUser().getName()),
                    NotificationType.PAYMENT_SENT);
        }

        // Notify receiver
        if (transaction.getReceiverWallet() != null) {
            notificationService.sendNotification(
                    transaction.getReceiverWallet().getUser(),
                    "Payment Received",
                    String.format("You received %.2f XAF from %s",
                            transaction.getAmount(),
                            transaction.getSenderWallet() != null ? transaction.getSenderWallet().getUser().getName()
                                    : "Unknown"),
                    NotificationType.PAYMENT_RECEIVED);
        }
    }
}
