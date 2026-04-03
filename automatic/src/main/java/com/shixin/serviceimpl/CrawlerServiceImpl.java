package com.shixin.serviceimpl;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shixin.entity.Notification;
import com.shixin.repository.NotificationRepository;
import com.shixin.service.CrawlerService;
import com.shixin.tool.crawler.beizhuxie.BeizhuxieTrainingCrawler;

/**
 * 爬虫服务实现类
 */
@Service
public class CrawlerServiceImpl implements CrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerServiceImpl.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BeizhuxieTrainingCrawler beizhuxieTrainingCrawler;

    @Override
    public int executeCrawlerTask(Long userId, Long taskId, String keywords, String url) {
        logger.info("执行爬虫任务，用户ID: {}, 任务ID: {}, 关键词: {}, URL: {}", userId, taskId, keywords, url);
        
        // 根据URL判断使用哪个爬虫
        if (url != null && url.contains("bicpa.org.cn")) {
            return executeBeizhuxieTrainingCrawler(userId, taskId, keywords);
        } else {
            logger.warn("不支持的URL类型: {}", url);
            return 0;
        }
    }

    @Override
    public int executeBeizhuxieTrainingCrawler(Long userId, Long taskId, String keywords) {
        logger.info("执行北注协培训通知爬虫任务，用户ID: {}, 任务ID: {}, 关键词: {}", userId, taskId, keywords);
        
        try {
            return beizhuxieTrainingCrawler.crawlTrainingNotifications(userId, taskId, keywords);
        } catch (Exception e) {
            logger.error("执行北注协培训通知爬虫任务时发生错误", e);
            return 0;
        }
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        logger.debug("获取用户通知列表，用户ID: {}", userId);
        
        // 按通知时间倒序排列
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "notificationTime"));
        return notificationRepository.findRecentNotifications(userId, pageable);
    }

    @Override
    public List<Notification> getTaskNotifications(Long taskId) {
        logger.debug("获取任务通知列表，任务ID: {}", taskId);
        return notificationRepository.findByTaskId(taskId);
    }

    @Override
    public long getUnreadNotificationCount(Long userId) {
        logger.debug("获取用户未读通知数量，用户ID: {}", userId);
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public boolean markNotificationAsRead(Long notificationId) {
        logger.debug("标记通知为已读，通知ID: {}", notificationId);
        
        try {
            Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
            if (optionalNotification.isPresent()) {
                Notification notification = optionalNotification.get();
                notification.setRead(true);
                notificationRepository.save(notification);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("标记通知为已读时发生错误，通知ID: {}", notificationId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public int markAllNotificationsAsRead(Long userId) {
        logger.debug("标记用户所有通知为已读，用户ID: {}", userId);
        
        try {
            List<Notification> unreadNotifications = notificationRepository.findByUserIdAndReadFalse(userId);
            for (Notification notification : unreadNotifications) {
                notification.setRead(true);
            }
            notificationRepository.saveAll(unreadNotifications);
            return unreadNotifications.size();
        } catch (Exception e) {
            logger.error("标记用户所有通知为已读时发生错误，用户ID: {}", userId, e);
            return 0;
        }
    }

    @Override
    @Transactional
    public boolean deleteNotification(Long notificationId) {
        logger.debug("删除通知，通知ID: {}", notificationId);
        
        try {
            if (notificationRepository.existsById(notificationId)) {
                notificationRepository.deleteById(notificationId);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("删除通知时发生错误，通知ID: {}", notificationId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public int deleteTaskNotifications(Long taskId) {
        logger.debug("删除任务的所有通知，任务ID: {}", taskId);
        
        try {
            List<Notification> taskNotifications = notificationRepository.findByTaskId(taskId);
            int count = taskNotifications.size();
            notificationRepository.deleteAll(taskNotifications);
            return count;
        } catch (Exception e) {
            logger.error("删除任务的所有通知时发生错误，任务ID: {}", taskId, e);
            return 0;
        }
    }

    @Override
    public boolean testCrawlerConnection(String url) {
        logger.info("测试爬虫连接，URL: {}", url);
        
        try {
            // 这里可以添加通用的连接测试逻辑
            // 目前主要测试北注协网站
            if (url.contains("bicpa.org.cn")) {
                beizhuxieTrainingCrawler.testCrawler();
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("测试爬虫连接时发生错误，URL: {}", url, e);
            return false;
        }
    }

    @Override
    public void testBeizhuxieCrawler() {
        logger.info("测试北注协培训通知爬虫");
        beizhuxieTrainingCrawler.testCrawler();
    }

    /**
     * 获取通知详情
     * 
     * @param notificationId 通知ID
     * @return 通知详情
     */
    public Notification getNotificationDetail(Long notificationId) {
        logger.debug("获取通知详情，通知ID: {}", notificationId);
        return notificationRepository.findById(notificationId).orElse(null);
    }

    /**
     * 搜索通知
     * 
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @return 匹配的通知列表
     */
    public List<Notification> searchNotifications(Long userId, String keyword) {
        logger.debug("搜索通知，用户ID: {}, 关键词: {}", userId, keyword);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return getUserNotifications(userId);
        }
        
        // 这里可以添加更复杂的搜索逻辑
        // 目前简单地在标题和内容中搜索
        List<Notification> userNotifications = getUserNotifications(userId);
        return userNotifications.stream()
                .filter(notification -> 
                    notification.getTitle().contains(keyword) || 
                    (notification.getOriginalContent() != null && notification.getOriginalContent().contains(keyword)))
                .toList();
    }

    /**
     * 获取最近的通知
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 最近的通知列表
     */
    public List<Notification> getRecentNotifications(Long userId, int limit) {
        logger.debug("获取最近通知，用户ID: {}, 限制数量: {}", userId, limit);
        
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "notificationTime"));
        return notificationRepository.findRecentNotifications(userId, pageable);
    }
}