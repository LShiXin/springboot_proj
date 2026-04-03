package com.shixin.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shixin.entity.ApiResponse;
import com.shixin.entity.JwtUtil;
import com.shixin.entity.User;
import com.shixin.service.UserService;

@RestController
@RequestMapping("/api")
public class AccountController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil  jwtUtil;


    // 登录接口
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        System.out.println("登录请求 - 用户名: " + username + ", 密码: " + password);

        Map<String, Object> response = new HashMap<>();

        // 模拟验证（实际应查询数据库或验证服务）
        User user = userService.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            // 生成并返回 token（实际应使用 JWT 等技术生成有效 token）
            response.put("token", jwtUtil.generateToken(user.getId()));
            System.out.println("生成的Token: " + response.get("token"));
            return ResponseEntity.ok(ApiResponse.success(response));
        } else{
            response.put("success", false);
            // 也可以返回 401 状态码，但前端仅依赖 success 字段判断，所以 200 也可
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.success(response));
        }
    }

    // 注册接口
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        System.out.println("注册请求 - 用户名: " + username + ", 密码: " + password);
        // 查询用户是否存在，
        if (userService.findByUsername(username) == null) {
            try {
                userService.addorUpdateUser(new User(username, password));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error(500, "注册失败: " + e.getMessage()));
            }
            return ResponseEntity.ok(ApiResponse.success("注册成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "用户名已存在"));
        }
    }
}
