package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // 전체 조회 JPA
    public List<User> getUsers() {
        return userRepository.findAll();
    }

	// 전체 조회 MYBATIS
    public List<User> getUsersMybatis() {
        return userMapper.findAll();
    }

    // 저장
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}