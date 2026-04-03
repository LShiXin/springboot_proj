package com.shixin.tool.crawler.beizhuxie;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关键词高亮处理器
 * 用于将文本中的关键词进行变色处理
 */
public class KeywordHighlighter {

    /**
     * 默认高亮样式：红色加粗
     */
    private static final String DEFAULT_HIGHLIGHT_START = "<span style=\"color: red; font-weight: bold;\">";
    private static final String DEFAULT_HIGHLIGHT_END = "</span>";

    /**
     * 高亮文本中的关键词
     * 
     * @param text 原始文本
     * @param keywords 关键词列表（多个关键词用逗号分隔）
     * @return 处理后的文本（关键词被高亮）
     */
    public static String highlightKeywords(String text, String keywords) {
        if (text == null || text.isEmpty() || keywords == null || keywords.isEmpty()) {
            return text;
        }

        // 分割关键词
        String[] keywordArray = keywords.split(",");
        List<String> keywordList = new ArrayList<>();
        
        for (String keyword : keywordArray) {
            String trimmed = keyword.trim();
            if (!trimmed.isEmpty()) {
                keywordList.add(trimmed);
            }
        }

        if (keywordList.isEmpty()) {
            return text;
        }

        // 构建正则表达式模式
        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < keywordList.size(); i++) {
            if (i > 0) {
                patternBuilder.append("|");
            }
            // 对关键词进行转义，避免正则特殊字符问题
            patternBuilder.append(Pattern.quote(keywordList.get(i)));
        }
        
        Pattern pattern = Pattern.compile(patternBuilder.toString());
        Matcher matcher = pattern.matcher(text);
        
        // 使用StringBuffer进行替换
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String matchedKeyword = matcher.group();
            matcher.appendReplacement(result, DEFAULT_HIGHLIGHT_START + matchedKeyword + DEFAULT_HIGHLIGHT_END);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 高亮文本中的关键词，并返回匹配到的关键词列表
     * 
     * @param text 原始文本
     * @param keywords 关键词列表（多个关键词用逗号分隔）
     * @param matchedKeywords 输出参数，用于存储匹配到的关键词
     * @return 处理后的文本（关键词被高亮）
     */
    public static String highlightKeywords(String text, String keywords, List<String> matchedKeywords) {
        if (text == null || text.isEmpty() || keywords == null || keywords.isEmpty()) {
            return text;
        }

        // 清空输出列表
        if (matchedKeywords != null) {
            matchedKeywords.clear();
        }

        // 分割关键词
        String[] keywordArray = keywords.split(",");
        List<String> keywordList = new ArrayList<>();
        
        for (String keyword : keywordArray) {
            String trimmed = keyword.trim();
            if (!trimmed.isEmpty()) {
                keywordList.add(trimmed);
            }
        }

        if (keywordList.isEmpty()) {
            return text;
        }

        // 构建正则表达式模式
        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < keywordList.size(); i++) {
            if (i > 0) {
                patternBuilder.append("|");
            }
            patternBuilder.append(Pattern.quote(keywordList.get(i)));
        }
        
        Pattern pattern = Pattern.compile(patternBuilder.toString());
        Matcher matcher = pattern.matcher(text);
        
        // 使用StringBuffer进行替换
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String matchedKeyword = matcher.group();
            matcher.appendReplacement(result, DEFAULT_HIGHLIGHT_START + matchedKeyword + DEFAULT_HIGHLIGHT_END);
            
            // 记录匹配到的关键词（去重）
            if (matchedKeywords != null && !matchedKeywords.contains(matchedKeyword)) {
                matchedKeywords.add(matchedKeyword);
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 检查文本中是否包含关键词
     * 
     * @param text 原始文本
     * @param keywords 关键词列表（多个关键词用逗号分隔）
     * @return 是否包含关键词
     */
    public static boolean containsKeywords(String text, String keywords) {
        if (text == null || text.isEmpty() || keywords == null || keywords.isEmpty()) {
            return false;
        }

        // 分割关键词
        String[] keywordArray = keywords.split(",");
        
        for (String keyword : keywordArray) {
            String trimmed = keyword.trim();
            if (!trimmed.isEmpty() && text.contains(trimmed)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 获取文本中匹配到的关键词（去重）
     * 
     * @param text 原始文本
     * @param keywords 关键词列表（多个关键词用逗号分隔）
     * @return 匹配到的关键词列表（逗号分隔）
     */
    public static String getMatchedKeywords(String text, String keywords) {
        if (text == null || text.isEmpty() || keywords == null || keywords.isEmpty()) {
            return "";
        }

        // 分割关键词
        String[] keywordArray = keywords.split(",");
        List<String> matchedKeywords = new ArrayList<>();
        
        for (String keyword : keywordArray) {
            String trimmed = keyword.trim();
            if (!trimmed.isEmpty() && text.contains(trimmed) && !matchedKeywords.contains(trimmed)) {
                matchedKeywords.add(trimmed);
            }
        }
        
        return String.join(",", matchedKeywords);
    }

    /**
     * 自定义高亮样式
     * 
     * @param text 原始文本
     * @param keywords 关键词列表
     * @param highlightStart 高亮开始标签
     * @param highlightEnd 高亮结束标签
     * @return 处理后的文本
     */
    public static String highlightKeywordsWithCustomStyle(String text, String keywords, 
                                                         String highlightStart, String highlightEnd) {
        if (text == null || text.isEmpty() || keywords == null || keywords.isEmpty()) {
            return text;
        }

        // 分割关键词
        String[] keywordArray = keywords.split(",");
        List<String> keywordList = new ArrayList<>();
        
        for (String keyword : keywordArray) {
            String trimmed = keyword.trim();
            if (!trimmed.isEmpty()) {
                keywordList.add(trimmed);
            }
        }

        if (keywordList.isEmpty()) {
            return text;
        }

        // 构建正则表达式模式
        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < keywordList.size(); i++) {
            if (i > 0) {
                patternBuilder.append("|");
            }
            patternBuilder.append(Pattern.quote(keywordList.get(i)));
        }
        
        Pattern pattern = Pattern.compile(patternBuilder.toString());
        Matcher matcher = pattern.matcher(text);
        
        // 使用StringBuffer进行替换
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String matchedKeyword = matcher.group();
            matcher.appendReplacement(result, highlightStart + matchedKeyword + highlightEnd);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}