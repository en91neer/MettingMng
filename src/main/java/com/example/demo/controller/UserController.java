package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    // 생성자 주입
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 전체 조회(jpa)
    @GetMapping("/getUsers")
    public List<User> getUsersJpa() {
        return userService.getUsers();
    }
    
    // 전체 조회(mybatis)
    @GetMapping("/getMUsers")
    public List<User> getUsersMybatis() {
        return userService.getUsersMybatis();
    }

    // 저장
    @PostMapping("/saveUser")
    public User saveUser(@RequestBody User user) {
        return userService.saveUser(user);
    }
}