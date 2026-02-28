package com.shixin.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.shixin.entity.JwtUtil;
import com.shixin.entity.User;
import com.shixin.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 如果是 OPTIONS 请求，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从请求头获取 token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // 去掉 "Bearer " 前缀
            if (jwtUtil.validateToken(token)) {
                // 解析用户ID并存入请求属性，便于后续使用
                Long userId = jwtUtil.getUserIdFromToken(token);

                // 从数据库或Redis中获取用户信息，确保数据的实时性和准确性
                System.out.println("开始获取用户数据，用户ID: " + userId);
                User userInfo = userService.JWTFindUserForRedisOrMysql(userId);
                request.setAttribute("userInfo", userInfo);
                request.setAttribute("userId", userId);
                return true; // 放行
            }
        }
        // 验证失败，返回 401 Unauthorized
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"无效或缺失的Token\"}");
        return false;
    }
}