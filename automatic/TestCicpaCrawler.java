import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TestCicpaCrawler {
    public static void main(String[] args) {
        System.out.println("测试中注协爬虫选择器修复...");
        
        try {
            // 测试要闻页面
            System.out.println("\n=== 测试要闻页面 ===");
            testPage("https://www.cicpa.org.cn/xxfb/news/");
            
            // 测试通知公告页面
            System.out.println("\n=== 测试通知公告页面 ===");
            testPage("https://www.cicpa.org.cn/xxfb/tzgg/");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void testPage(String url) throws IOException {
        System.out.println("访问URL: " + url);
        
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(10000)
                .get();
        
        System.out.println("页面标题: " + doc.title());
        
        // 测试修复后的选择器
        Elements newsItems = doc.select(".j-list-inline li");
        System.out.println("使用选择器 '.j-list-inline li' 找到 " + newsItems.size() + " 个项目");
        
        if (newsItems.size() > 0) {
            System.out.println("\n前3个项目示例:");
            for (int i = 0; i < Math.min(3, newsItems.size()); i++) {
                Element item = newsItems.get(i);
                Element titleElement = item.selectFirst("a");
                if (titleElement != null) {
                    String title = titleElement.text().trim();
                    String href = titleElement.attr("href");
                    System.out.println((i+1) + ". 标题: " + title);
                    System.out.println("   链接: " + href);
                    
                    // 提取日期
                    String itemText = item.text();
                    java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                    java.util.regex.Matcher matcher = datePattern.matcher(itemText);
                    if (matcher.find()) {
                        System.out.println("   日期: " + matcher.group());
                    }
                }
            }
        }
        
        // 测试旧的选择器作为对比
        Elements oldNewsItems = doc.select(".news-list li, .list li, .article-list li, tr");
        System.out.println("\n使用旧选择器 '.news-list li, .list li, .article-list li, tr' 找到 " + oldNewsItems.size() + " 个项目");
        
        // 测试后备选择器
        Elements backupItems = doc.select("div[class*=news], div[class*=list], table tr");
        System.out.println("使用后备选择器 'div[class*=news], div[class*=list], table tr' 找到 " + backupItems.size() + " 个项目");
    }
}