package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.UserActionLog;

public interface UserActionLogRepository extends JpaRepository<UserActionLog, Long> {
    long countByLoginEmailIgnoreCaseAndActionType(String loginEmail, String actionType);
}
