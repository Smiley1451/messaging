package com.notification.repository;

import com.notification.model.entity.NotificationLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface NotificationLogRepository extends ReactiveCrudRepository<NotificationLog, Long> {

    Flux<NotificationLog> findByUsername(String username);

    Flux<NotificationLog> findByStatus(String status);

    @Query("SELECT * FROM notification_logs WHERE created_at >= :startDate AND created_at <= :endDate")
    Flux<NotificationLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT * FROM notification_logs WHERE status = 'FAILED' AND retry_count < :maxRetries")
    Flux<NotificationLog> findFailedNotificationsForRetry(int maxRetries);

    Mono<Long> countByStatus(String status);
}
