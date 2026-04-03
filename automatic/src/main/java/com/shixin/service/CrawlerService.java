package com.shixin.service;

import java.util.List;

import com.shixin.entity.Notification;

/**
 * 爬虫服务接口
 */
public interface CrawlerService {

    /**
     * 执行爬虫任务
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param keywords 关键词（多个用逗号分隔）
     * @param url 爬取的目标URL
     * @return 爬取到的通知数量
     */
    int executeCrawlerTask(Long userId, Long taskId, String keywords, String url);

    /**
     * 执行北注协培训通知爬虫任务
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param keywords 关键词（多个用逗号分隔）
     * @return 爬取到的通知数量
     */
    int executeBeizhuxieTrainingCrawler(Long userId, Long taskId, String keywords);

    /**
     * 获取用户的通知列表
     * 
     * @param userId 用户ID
     * @return 通知列表
     */
    List<Notification> getUserNotifications(Long userId);

    /**
     * 获取任务的通知列表
     * 
     * @param taskId 任务ID
     * @return 通知列表
     */
    List<Notification> getTaskNotifications(Long taskId);

    /**
     * 获取用户未读通知数量
     * 
     * @param userId 用户ID
     * @return 未读通知数量
     */
    long getUnreadNotificationCount(Long userId);

    /**
     * 标记通知为已读
     * 
     * @param notificationId 通知ID
     * @return 是否成功
     */
    boolean markNotificationAsRead(Long notificationId);

    /**
     * 标记用户所有通知为已读
     * 
     * @param userId 用户ID
     * @return 标记的通知数量
     */
    int markAllNotificationsAsRead(Long userId);

    /**
     * 删除通知
     * 
     * @param notificationId 通知ID
     * @return 是否成功
     */
    boolean deleteNotification(Long notificationId);

    /**
     * 删除任务的所有通知
     * 
     * @param taskId 任务ID
     * @return 删除的通知数量
     */
    int deleteTaskNotifications(Long taskId);

    /**
     * 测试爬虫连接
     * 
     * @param url 测试的URL
     * @return 是否连接成功
     */
    boolean testCrawlerConnection(String url);

    /**
     * 测试北注协培训通知爬虫
     */
    void testBeizhuxieCrawler();
}