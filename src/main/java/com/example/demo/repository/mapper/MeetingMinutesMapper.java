package com.example.demo.repository.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.dto.MeetingMinutesRequestDto;
import com.example.demo.entity.MeetingMinutesResult;

@Mapper
public interface MeetingMinutesMapper {
	// 회의록 조회
	List<MeetingMinutesResult> meetingMinutesList(MeetingMinutesRequestDto dto);
}
