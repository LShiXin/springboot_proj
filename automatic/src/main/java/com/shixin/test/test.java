package com.shixin.test;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 爬取北京注册会计师协会网站“培训信息”页面的最近十条通知
 * 目标URL: https://www.bicpa.org.cn/p1/pxxx.html
 */
public class test {

    // 目标URL
    private static final String TARGET_URL = "https://www.bicpa.org.cn/p1/pxxx.html";
    // 网站基础URL，用于补全相对链接
    private static final String BASE_URL = "https://www.bicpa.org.cn";

    // 请求配置：超时时间
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(5000)
            .setSocketTimeout(5000)
            .setConnectionRequestTimeout(5000)
            .build();

    public static void main(String[] args) {
        // 1. 创建HttpClient
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            // 2. 创建GET请求
            HttpGet httpGet = new HttpGet(TARGET_URL);
            httpGet.setConfig(REQUEST_CONFIG);

            // 3. 设置请求头，模拟浏览器
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
            httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            httpGet.setHeader("Referer", "https://www.bicpa.org.cn/p1/kspx.html"); // 上级页面

            // 4. 执行请求
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                System.out.println("响应状态码: " + statusCode);

                if (statusCode == 200) {
                    // 5. 获取HTML内容（UTF-8编码）
                    String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                    // 6. 使用Jsoup解析HTML，提取最近十条通知
                    parseAndPrintNotices(html);
                } else {
                    System.err.println("请求失败，HTTP状态码: " + statusCode);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析HTML并打印最近十条通知
     * @param html 网页HTML内容
     */
    private static void parseAndPrintNotices(String html) {
        // 使用Jsoup加载HTML
        Document doc = Jsoup.parse(html);

        // 定位到<ul class="list">下的所有<li>标签
        Elements listItems = doc.select("ul.list > li");

        if (listItems.isEmpty()) {
            System.out.println("未找到任何通知列表，请检查页面结构是否变化。");
            return;
        }

        System.out.println("最近十条通知：");
        System.out.println("========================");

        // 取前10条（如果不足10条则全部打印）
        int count = 0;
        for (Element item : listItems) {
            if (count >= 10) break;

            // 获取<a>标签
            Element linkTag = item.selectFirst("a");
            if (linkTag == null) continue;

            // 标题：优先取title属性，如果没有则取文本内容
            String title = linkTag.attr("title");
            if (title.isEmpty()) {
                title = linkTag.text();
            }

            // 链接：href属性
            String href = linkTag.attr("href");
            // 补全为绝对URL
            String fullUrl = href.startsWith("http") ? href : BASE_URL + href;

            // 日期：位于<span class="date">中
            Element dateTag = item.selectFirst("span.date");
            String date = dateTag != null ? dateTag.text() : "未知日期";

            // 输出
            System.out.println((count + 1) + ".");
            System.out.println("标题：" + title);
            System.out.println("链接：" + fullUrl);
            System.out.println("日期：" + date);
            System.out.println("------------------------");

            count++;
        }
    }
}