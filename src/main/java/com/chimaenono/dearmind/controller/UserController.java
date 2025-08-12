package com.chimaenono.dearmind.controller;

import com.chimaenono.dearmind.entity.User;
import com.chimaenono.dearmind.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 테스트 사용자 생성
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setEmail("test@example.com");
            
            User savedUser = userRepository.save(testUser);
            
            response.put("status", "success");
            response.put("message", "Database connection successful!");
            response.put("user", savedUser);
            response.put("totalUsers", userRepository.count());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Database connection failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<User> users = userRepository.findAll();
            response.put("status", "success");
            response.put("users", users);
            response.put("count", users.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to get users: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        try {
            User savedUser = userRepository.save(user);
            response.put("status", "success");
            response.put("message", "User created successfully");
            response.put("user", savedUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to create user: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
} 