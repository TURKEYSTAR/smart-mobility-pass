package com.smartmobility.notificationservice.controller;

import com.smartmobility.notificationservice.entity.Notification;
import com.smartmobility.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * NotificationController
 *
 * GET  /notifications/user/{userId}           → toutes les notifications d'un usager
 * GET  /notifications/user/{userId}/unread    → notifications non lues
 * GET  /notifications/user/{userId}/unread/count → nombre de non lues
 * PUT  /notifications/{id}/read               → marquer une notif comme lue
 * PUT  /notifications/user/{userId}/read-all  → tout marquer comme lu
 * GET  /notifications/health                  → health check
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getByUser(@PathVariable UUID userId) {
        log.info("[NotificationController] GET /notifications/user/{}", userId);
        return ResponseEntity.ok(notificationService.getNotificationsByUserId(userId));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnread(@PathVariable UUID userId) {
        log.info("[NotificationController] GET /notifications/user/{}/unread", userId);
        return ResponseEntity.ok(notificationService.getUnreadByUserId(userId));
    }

    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Map<String, Long>> countUnread(@PathVariable UUID userId) {
        long count = notificationService.countUnread(userId);
        log.info("[NotificationController] GET unread count userId={} → {}", userId, count);
        return ResponseEntity.ok(Map.of("unread", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable UUID id) {
        log.info("[NotificationController] PUT /notifications/{}/read", id);
        return notificationService.markAsRead(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable UUID userId) {
        log.info("[NotificationController] PUT /notifications/user/{}/read-all", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service ✅ opérationnel - Port 8085");
    }
}