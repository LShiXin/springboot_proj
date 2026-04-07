package com.shixin.tool.crawler.beizhuxie;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.shixin.entity.Notification;
import com.shixin.repository.NotificationRepository;
import com.shixin.tool.crawler.KeywordHighlighter;

/**
 * 北注协培训通知爬虫工具类
 * 爬取北京注册会计师协会培训通知
 * 网址：https://www.bicpa.org.cn/p1/pxxx.html
 * 翻页：https://www.bicpa.org.cn/p1/pxxx_2.html, https://www.bicpa.org.cn/p1/pxxx_3.html 等
 */
@Component
public class BeizhuxieTrainingCrawler {

    private static final Logger logger = LoggerFactory.getLogger(BeizhuxieTrainingCrawler.class);

    // 基础URL
    private static final String BASE_URL = "https://www.bicpa.org.cn/p1/pxxx";
    private static final String BASE_PAGE_URL = BASE_URL + ".html";
    private static final String PAGE_URL_TEMPLATE = BASE_URL + "_%d.html";

    // 日期格式化器
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 用户代理
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // 连接超时时间（毫秒）
    private static final int TIMEOUT = 10000;

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * 爬取培训通知
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param keywords 关键词（多个用逗号分隔）
     * @return 爬取到的通知数量
     */
    public int crawlTrainingNotifications(Long userId, Long taskId, String keywords) {
        logger.info("开始爬取北注协培训通知，用户ID: {}, 任务ID: {}, 关键词: {}", userId, taskId, keywords);

        int totalNotifications = 0;
        int page = 1;
        boolean hasMorePages = true;
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        try {
            while (hasMorePages) {
                String pageUrl = getPageUrl(page);
                logger.info("爬取第 {} 页: {}", page, pageUrl);

                Document doc = fetchDocument(pageUrl);
                if (doc == null) {
                    logger.warn("无法获取第 {} 页内容，停止爬取", page);
                    break;
                }

                // 解析通知列表
                List<Notification> notifications = parseNotifications(doc, userId, taskId, keywords, oneMonthAgo);
                
                // 保存通知
                for (Notification notification : notifications) {
                    saveNotificationIfNotExists(notification);
                    totalNotifications++;
                }

                // 检查是否有下一页
                hasMorePages = hasNextPage(doc);
                page++;

                // 避免请求过于频繁
                Thread.sleep(1000);
            }

            logger.info("爬取完成，共找到 {} 条相关通知", totalNotifications);
        } catch (InterruptedException e) {
            logger.error("爬取过程被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("爬取北注协培训通知时发生错误", e);
        }

        return totalNotifications;
    }

    /**
     * 获取页面URL
     */
    private String getPageUrl(int page) {
        if (page <= 1) {
            return BASE_PAGE_URL;
        } else {
            return String.format(PAGE_URL_TEMPLATE, page);
        }
    }

    /**
     * 获取网页文档
     */
    private Document fetchDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .get();
        } catch (IOException e) {
            logger.error("获取网页失败: {}", url, e);
            return null;
        }
    }

    /**
     * 解析通知列表
     */
    private List<Notification> parseNotifications(Document doc, Long userId, Long taskId, 
                                                 String keywords, LocalDateTime oneMonthAgo) {
        List<Notification> notifications = new ArrayList<>();

        // 根据实际网页结构选择合适的选择器
        // 这里使用通用的选择器，可能需要根据实际网页结构调整
        Elements newsItems = doc.select(".news-list li, .list li, .article-list li, tr");
        
        if (newsItems.isEmpty()) {
            // 尝试其他常见的选择器
            newsItems = doc.select("div[class*=news], div[class*=list], table tr");
        }

        for (Element item : newsItems) {
            try {
                Notification notification = parseNotificationItem(item, userId, taskId, keywords, oneMonthAgo);
                if (notification != null) {
                    notifications.add(notification);
                }
            } catch (Exception e) {
                logger.warn("解析通知项时发生错误", e);
            }
        }

        return notifications;
    }

    /**
     * 解析单个通知项
     */
    private Notification parseNotificationItem(Element item, Long userId, Long taskId, 
                                              String keywords, LocalDateTime oneMonthAgo) {
        // 提取标题和链接
        Element titleElement = item.selectFirst("a");
        if (titleElement == null) {
            return null;
        }

        String title = titleElement.text().trim();
        String relativeUrl = titleElement.attr("href");
        
        if (title.isEmpty() || relativeUrl.isEmpty()) {
            return null;
        }

        // 构建完整URL
        String url = buildFullUrl(relativeUrl);

        // 提取日期
        LocalDateTime notificationTime = extractNotificationTime(item);
        if (notificationTime == null) {
            // 如果没有日期，使用当前日期
            notificationTime = LocalDateTime.now();
        }

        // 检查是否在一个月内
        if (notificationTime.isBefore(oneMonthAgo)) {
            return null;
        }

        // 检查是否包含关键词
        if (!KeywordHighlighter.containsKeywords(title, keywords)) {
            return null;
        }

        // 获取通知详情内容
        String content = fetchNotificationContent(url);
        if (content == null) {
            content = title; // 如果无法获取详情，使用标题作为内容
        }

        // 处理关键词高亮
        String processedContent = KeywordHighlighter.highlightKeywords(content, keywords);
        String matchedKeywords = KeywordHighlighter.getMatchedKeywords(content, keywords);

        // 创建通知对象
        Notification notification = new Notification(userId, taskId, title, url, notificationTime);
        notification.setOriginalContent(content);
        notification.setProcessedContent(processedContent);
        notification.setMatchedKeywords(matchedKeywords);

        return notification;
    }

    /**
     * 构建完整URL
     */
    private String buildFullUrl(String relativeUrl) {
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl;
        } else if (relativeUrl.startsWith("/")) {
            return "https://www.bicpa.org.cn" + relativeUrl;
        } else {
            return "https://www.bicpa.org.cn/p1/" + relativeUrl;
        }
    }

    /**
     * 提取通知时间
     */
    private LocalDateTime extractNotificationTime(Element item) {
        // 尝试多种日期选择器
        String[] dateSelectors = {
            "span.date, .date, .time, .pub-date, .news-date",
            "td:last-child", // 表格中的最后一列可能是日期
            "div:last-child" // 最后一个div可能是日期
        };

        for (String selector : dateSelectors) {
            Element dateElement = item.selectFirst(selector);
            if (dateElement != null) {
                String dateText = dateElement.text().trim();
                if (!dateText.isEmpty()) {
                    try {
                        // 尝试解析日期
                        return parseDateString(dateText);
                    } catch (DateTimeParseException e) {
                        // 继续尝试其他选择器
                    }
                }
            }
        }

        // 尝试从文本中提取日期模式
        String itemText = item.text();
        Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        Matcher matcher = datePattern.matcher(itemText);
        if (matcher.find()) {
            try {
                return parseDateString(matcher.group());
            } catch (DateTimeParseException e) {
                // 忽略解析错误
            }
        }

        return null;
    }

    /**
     * 解析日期字符串
     */
    private LocalDateTime parseDateString(String dateString) {
        try {
            // 尝试解析完整日期时间
            return LocalDateTime.parse(dateString + " 00:00:00", DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e1) {
            try {
                // 尝试解析日期
                LocalDate date = LocalDate.parse(dateString, DATE_FORMATTER);
                return date.atStartOfDay();
            } catch (DateTimeParseException e2) {
                // 尝试其他常见格式
                String[] formats = {
                    "yyyy/MM/dd",
                    "yyyy.MM.dd",
                    "yyyy年MM月dd日"
                };
                
                for (String format : formats) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                        LocalDate date = LocalDate.parse(dateString, formatter);
                        return date.atStartOfDay();
                    } catch (DateTimeParseException e3) {
                        // 继续尝试下一个格式
                    }
                }
                
                throw e2; // 如果所有格式都失败，抛出异常
            }
        }
    }

    /**
     * 获取通知详情内容
     */
    private String fetchNotificationContent(String url) {
        try {
            Document detailDoc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .get();

            // 尝试常见的内容选择器
            String[] contentSelectors = {
                ".article-content, .news-content, .content, .detail, .main-content",
                "div[class*=content], div[class*=detail], div[class*=article]",
                "#content, #detail, #article"
            };

            for (String selector : contentSelectors) {
                Element contentElement = detailDoc.selectFirst(selector);
                if (contentElement != null) {
                    return contentElement.text().trim();
                }
            }

            // 如果找不到特定内容区域，返回body文本
            return detailDoc.body().text().trim();
        } catch (IOException e) {
            logger.warn("获取通知详情失败: {}", url, e);
            return null;
        }
    }

    /**
     * 检查是否有下一页
     */
    private boolean hasNextPage(Document doc) {
        // 检查常见的分页元素
        Elements nextPageElements = doc.select("a:contains(下一页), a:contains(Next), .next-page, .page-next");
        
        for (Element element : nextPageElements) {
            String href = element.attr("href");
            if (href != null && !href.isEmpty() && !href.contains("#")) {
                return true;
            }
        }

        // 检查分页数字
        Elements pageNumbers = doc.select(".page-num, .pagination a");
        if (!pageNumbers.isEmpty()) {
            // 如果有多个页码，认为有下一页
            return pageNumbers.size() > 1;
        }

        return false;
    }

    /**
     * 保存通知（如果不存在）
     */
    private void saveNotificationIfNotExists(Notification notification) {
        try {
            // 检查是否已存在相同URL的通知
            Notification existing = notificationRepository.findByUserIdAndTaskIdAndUrl(
                notification.getUserId(), 
                notification.getTaskId(), 
                notification.getUrl()
            );

            if (existing == null) {
                notificationRepository.save(notification);
                logger.info("保存新通知: {}", notification.getTitle());
            } else {
                logger.debug("通知已存在，跳过: {}", notification.getTitle());
            }
        } catch (Exception e) {
            logger.error("保存通知失败: {}", notification.getTitle(), e);
        }
    }

    /**
     * 按照需求策略爬取培训通知
     * 策略：1.先对第一页的通知进行处理
     *       2.每处理一条通知就与该定时任务的关键词进行对比
     *       3.如果存在关键词就保存进数据库
     *       4.如果该条通知已经存在，就不保存
     *       5.所有的通知链接仅扫描一个月以内的数据
     *       6.如果查到有之外的通知，就停止扫描，否则就进行下一页的扫描
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param keywords 关键词（多个用逗号分隔）
     * @return 爬取到的通知数量
     */
    public int crawlTrainingNotificationsWithStrategy(Long userId, Long taskId, String keywords) {
        return crawlTrainingNotificationsWithStrategy(userId, taskId, keywords, null);
    }
    
    public int crawlTrainingNotificationsWithStrategy(Long userId, Long taskId, String keywords, Long executionRecordId) {
        logger.info("开始按照策略爬取北注协培训通知，用户ID: {}, 任务ID: {}, 关键词: {}, 执行记录ID: {}", 
                userId, taskId, keywords, executionRecordId);
        
        int totalNotifications = 0;
        int page = 1;
        boolean shouldContinue = true;
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        try {
            while (shouldContinue) {
                String pageUrl = getPageUrl(page);
                logger.info("爬取第 {} 页: {}", page, pageUrl);
                
                Document doc = fetchDocument(pageUrl);
                if (doc == null) {
                    logger.warn("无法获取第 {} 页内容，停止爬取", page);
                    break;
                }
                
                // 解析当前页面的通知项
                Elements newsItems = doc.select(".news-list li, .list li, .article-list li, tr");
                if (newsItems.isEmpty()) {
                    // 尝试其他常见的选择器
                    newsItems = doc.select("div[class*=news], div[class*=list], table tr");
                }
                
                if (newsItems.isEmpty()) {
                    logger.warn("第 {} 页没有找到通知项，停止爬取", page);
                    break;
                }
                
                // 处理当前页的每个通知项
                boolean foundOutOfMonth = false;
                for (Element item : newsItems) {
                    try {
                        // 解析单个通知项
                        Notification notification = parseNotificationItemWithStrategy(
                            item, userId, taskId, keywords, oneMonthAgo, executionRecordId
                        );
                        
                        if (notification == null) {
                            // 通知为null可能表示时间超过一个月
                            foundOutOfMonth = true;
                            continue;
                        }
                        
                        // 保存通知（如果不存在）
                        saveNotificationIfNotExists(notification);
                        totalNotifications++;
                        logger.info("保存通知: {}", notification.getTitle());
                        
                    } catch (Exception e) {
                        logger.warn("处理通知项时发生错误", e);
                    }
                }
                
                // 检查是否应该继续下一页
                if (foundOutOfMonth) {
                    // 如果当前页发现了超过一个月前的通知，停止扫描
                    logger.info("第 {} 页发现超过一个月前的通知，停止扫描后续页面", page);
                    shouldContinue = false;
                } else {
                    // 检查是否有下一页
                    shouldContinue = hasNextPage(doc);
                    page++;
                    
                    // 避免请求过于频繁
                    Thread.sleep(1000);
                }
            }
            
            logger.info("策略爬取完成，共找到 {} 条相关通知", totalNotifications);
        } catch (InterruptedException e) {
            logger.error("爬取过程被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("按照策略爬取北注协培训通知时发生错误", e);
        }
        
        return totalNotifications;
    }
    
    /**
     * 按照策略解析单个通知项
     * 返回null表示通知时间超过一个月
     */
    private Notification parseNotificationItemWithStrategy(Element item, Long userId, Long taskId, 
                                                         String keywords, LocalDateTime oneMonthAgo) {
        return parseNotificationItemWithStrategy(item, userId, taskId, keywords, oneMonthAgo, null);
    }
    
    /**
     * 按照策略解析单个通知项（带执行记录ID）
     * 返回null表示通知时间超过一个月
     */
    private Notification parseNotificationItemWithStrategy(Element item, Long userId, Long taskId, 
                                                         String keywords, LocalDateTime oneMonthAgo, Long executionRecordId) {
        // 提取标题和链接
        Element titleElement = item.selectFirst("a");
        if (titleElement == null) {
            return null;
        }
        
        String title = titleElement.text().trim();
        String relativeUrl = titleElement.attr("href");
        
        if (title.isEmpty() || relativeUrl.isEmpty()) {
            return null;
        }
        
        // 构建完整URL
        String url = buildFullUrl(relativeUrl);
        
        // 提取日期
        LocalDateTime notificationTime = extractNotificationTime(item);
        if (notificationTime == null) {
            // 如果没有日期，使用当前日期
            notificationTime = LocalDateTime.now();
        }
        
        // 检查是否在一个月内 - 如果超过一个月，返回null
        if (notificationTime.isBefore(oneMonthAgo)) {
            return null;
        }
        
        // 检查是否包含关键词 - 如果不包含关键词，返回null
        if (!KeywordHighlighter.containsKeywords(title, keywords)) {
            return null;
        }
        
        // 获取通知详情内容
        String content = fetchNotificationContent(url);
        if (content == null) {
            content = title; // 如果无法获取详情，使用标题作为内容
        }
        
        // 处理关键词高亮
        String processedContent = KeywordHighlighter.highlightKeywords(content, keywords);
        String matchedKeywords = KeywordHighlighter.getMatchedKeywords(content, keywords);
        
        // 创建通知对象
        Notification notification = new Notification(userId, taskId, title, url, notificationTime);
        notification.setOriginalContent(content);
        notification.setProcessedContent(processedContent);
        notification.setMatchedKeywords(matchedKeywords);
        
        // 设置执行记录ID
        if (executionRecordId != null) {
            notification.setExecutionRecordId(executionRecordId);
        }
        
        return notification;
    }

    /**
     * 测试爬虫功能
     */
    public void testCrawler() {
        logger.info("开始测试北注协培训通知爬虫...");
        
        try {
            Document doc = fetchDocument(BASE_PAGE_URL);
            if (doc != null) {
                logger.info("成功连接到北注协网站");
                logger.info("页面标题: {}", doc.title());
                
                // 测试解析通知
                List<Notification> testNotifications = parseNotifications(
                    doc, 1L, 1L, "培训,通知", LocalDateTime.now().minusMonths(1)
                );
                
                logger.info("测试解析到 {} 条通知", testNotifications.size());
                for (Notification notification : testNotifications) {
                    logger.info("测试通知: {}", notification.getTitle());
                }
            } else {
                logger.error("无法连接到北注协网站");
            }
        } catch (Exception e) {
            logger.error("测试爬虫时发生错误", e);
        }
    }
}
