package com.shixin.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shixin.entity.ApiResponse;
import com.shixin.entity.User;
import com.shixin.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    // 刷新用户数据，用户在跳转到新页面后，会调用这个接口来获取最新的用户数据
    // 这个接口会从数据库中查询用户数据，并返回给前端
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCurrentUserInfo(HttpServletRequest request) {

        // JWT过滤器已经将用户ID存入请求属性中，这里直接获取即可
        Long userId = (Long) request.getAttribute("userId");
        User user=userService.findById(userId);
        System.out.println("查询用户数据，用户ID：" + user.toString());
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("enabled", user.getEnabled().toString());
        System.out.println("刷新用户数据，用户ID：" + userId);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }
}
