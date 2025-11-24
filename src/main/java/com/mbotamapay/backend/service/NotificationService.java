package com.mbotamapay.backend.service;

import com.mbotamapay.backend.entity.Notification;
import com.mbotamapay.backend.entity.NotificationType;
import com.mbotamapay.backend.entity.User;

import java.util.List;

public interface NotificationService {
    void sendNotification(User user, String title, String message, NotificationType type);

    List<Notification> getUserNotifications(User user);

    List<Notification> getUnreadNotifications(User user);

    void markAsRead(Long notificationId);
}
