package com.drivingcoach.backend.domain.driving.service;

import com.drivingcoach.backend.domain.driving.domain.dto.request.HistoryDateRequest;
import com.drivingcoach.backend.domain.driving.domain.dto.response.HistoryDetailResponse;
import com.drivingcoach.backend.domain.driving.domain.dto.response.HistoryResponse;
import com.drivingcoach.backend.domain.driving.domain.entity.DrivingRecord;
import com.drivingcoach.backend.domain.driving.repository.DrivingRecordRepository;
import com.drivingcoach.backend.global.exception.CustomException;
import com.drivingcoach.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryService {

    private final DrivingRecordRepository drivingRecordRepository;

    /**
     * 최근순 주행 기록 조회 (전체)
     */
    public List<HistoryResponse> getHistoryRecent(Long userId) {
        // 페이징 없이 전체를 가져오기 위해 unpaged() 사용
        List<DrivingRecord> records = drivingRecordRepository.findAllByUserIdOrderByStartTimeDesc(userId, Pageable.unpaged()).getContent();

        return records.stream()
                .map(HistoryResponse::from)
                .toList();
    }

    /**
     * 운전 시간순 주행 기록 조회 (전체)
     */
    public List<HistoryResponse> getHistoryByDrivingTime(Long userId) {
        // Repository에 추가한 findAllByUserIdOrderByTotalTimeDesc 메서드 사용
        List<DrivingRecord> records = drivingRecordRepository.findAllByUserIdOrderByTotalTimeDesc(userId);

        return records.stream()
                .map(HistoryResponse::from)
                .toList();
    }

    /**
     * 최근순 주행 기록 조회 (날짜 필터)
     */
    public List<HistoryResponse> getHistoryRecentByDate(Long userId, HistoryDateRequest request) {
        List<DrivingRecord> records = drivingRecordRepository.findAllByDateOrderByStartTimeDesc(
                userId,
                request.getYear(),
                request.getMonth(),
                request.getDate() // DTO의 date 필드를 day 파라미터로 전달
        );

        return records.stream()
                .map(HistoryResponse::from)
                .toList();
    }

    /**
     * 운전 시간순 주행 기록 조회 (날짜 필터)
     */
    public List<HistoryResponse> getHistoryByTimeAndDate(Long userId, HistoryDateRequest request) {
        List<DrivingRecord> records = drivingRecordRepository.findAllByDateOrderByTotalTimeDesc(
                userId,
                request.getYear(),
                request.getMonth(),
                request.getDate()
        );

        return records.stream()
                .map(HistoryResponse::from)
                .toList();
    }

    /**
     * 주행 상세 조회
     */
    public HistoryDetailResponse getHistoryDetail(Long userId, Long drivingId) {
        // 상세 조회 시 이벤트 목록도 필요하므로 fetch join 쿼리 사용 (기존 Repository에 있는 메서드)
        DrivingRecord record = drivingRecordRepository.findDetailWithEvents(drivingId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "해당 주행 기록을 찾을 수 없습니다."));

        return HistoryDetailResponse.from(record);
    }
}