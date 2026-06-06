package com.example.demo.repository.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    // 전체조회
    List<User> findAll();

    // 단건조회
    User findById(Long id);

    // 저장
    int insertUser(User user);

    // 수정
    int updateUser(User user);

    // 삭제
    int deleteUser(Long id);
}