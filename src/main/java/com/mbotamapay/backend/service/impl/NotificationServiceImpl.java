package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.entity.Notification;
import com.mbotamapay.backend.entity.NotificationType;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.repository.NotificationRepository;
import com.mbotamapay.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

        private final NotificationRepository notificationRepository;
        private final SimpMessagingTemplate messagingTemplate;

        @Override
        @Async
        public void sendNotification(User user, String title, String message, NotificationType type) {
                Notification notification = Notification.builder()
                                .user(user)
                                .title(title)
                                .message(message)
                                .type(type)
                                .read(false)
                                .build();

                Notification savedNotification = notificationRepository.save(notification);

                // Send real-time notification via WebSocket
                try {
                        messagingTemplate.convertAndSendToUser(
                                        user.getEmail(),
                                        "/queue/notifications",
                                        savedNotification);
                } catch (Exception e) {
                        log.error("Failed to send WebSocket notification to user: {}", user.getEmail(), e);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public List<Notification> getUserNotifications(User user) {
                return notificationRepository.findByUserOrderByCreatedAtDesc(user);
        }

        @Override
        @Transactional(readOnly = true)
        public List<Notification> getUnreadNotifications(User user) {
                return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
        }

        @Override
        @Transactional
        public void markAsRead(Long notificationId) {
                Notification notification = notificationRepository.findById(notificationId)
                                .orElseThrow(() -> new BusinessException("Notification not found"));
                notification.setRead(true);
                notificationRepository.save(notification);
        }
}
