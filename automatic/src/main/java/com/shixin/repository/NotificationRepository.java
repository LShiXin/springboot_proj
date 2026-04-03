package com.shixin.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shixin.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 根据用户ID和任务ID查找通知
     */
    List<Notification> findByUserIdAndTaskId(Long userId, Long taskId);

    /**
     * 根据用户ID查找通知
     */
    List<Notification> findByUserId(Long userId);

    /**
     * 根据任务ID查找通知
     */
    List<Notification> findByTaskId(Long taskId);

    /**
     * 根据URL查找通知（避免重复存储）
     */
    List<Notification> findByUrl(String url);

    /**
     * 根据用户ID、任务ID和URL查找通知
     */
    Notification findByUserIdAndTaskIdAndUrl(Long userId, Long taskId, String url);

    /**
     * 查找指定时间范围内的通知
     */
    List<Notification> findByNotificationTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 查找用户未读的通知
     */
    List<Notification> findByUserIdAndReadFalse(Long userId);

    /**
     * 统计用户未读通知数量
     */
    long countByUserIdAndReadFalse(Long userId);

    /**
     * 查找包含特定关键词的通知
     */
    @Query("SELECT n FROM Notification n WHERE n.matchedKeywords LIKE %:keyword%")
    List<Notification> findByMatchedKeywordsContaining(@Param("keyword") String keyword);

    /**
     * 查找最近N条通知
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.notificationTime DESC")
    List<Notification> findRecentNotifications(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);
}