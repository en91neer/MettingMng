package com.example.demo.repository;

import com.example.demo.entity.MeetingMinutes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingMinutesRepository extends JpaRepository<MeetingMinutes, Long> {
    boolean existsByIdAndMeetingOwnerId(Long id, String meetingOwnerId);
}
