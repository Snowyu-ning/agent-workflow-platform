package com.zxr.controller;

import com.zxr.common.JwtUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JdbcTemplate jdbcTemplate;
    private final JwtUtil jwtUtil;

    public AuthController(JdbcTemplate jdbcTemplate, JwtUtil jwtUtil) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        Map<String, Object> result = new HashMap<>();

        // 去数据库查这个用户
        String sql = "SELECT id, password FROM user WHERE name = ?";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, username);

        if (users.isEmpty()) {
            result.put("error", "用户不存在");
            return result;
        }

        Map<String, Object> dbUser = users.get(0);
        String dbPassword = (String) dbUser.get("password");
        Long userId = ((Number) dbUser.get("id")).longValue();

        // 校验密码（真实生产环境这里应该用 BCrypt 比较哈希值）
        if (!password.equals(dbPassword)) {
            result.put("error", "密码错误");
            return result;
        }

        // 校验通过，签发 Token
        String token = jwtUtil.createToken(userId);
        result.put("token", token);
        result.put("userId", userId);
        result.put("msg", "登录成功");

        return result;
    }
}