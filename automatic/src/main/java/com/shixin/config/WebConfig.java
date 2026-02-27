package com.shixin.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.shixin.interceptor.JwtInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")          // 拦截所有 /api 开头的请求
                .excludePathPatterns("/login", "/register","/home"); // 排除登录注册接口
    }

    // 配置 CORS 规则
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")               // 对所有路径生效
                .allowedOrigins("*")              // 允许所有源（生产环境可指定具体域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的HTTP方法
                .allowedHeaders("*")               // 允许所有请求头
                .maxAge(3600);                      // 预检请求缓存时间（秒）
    }
}