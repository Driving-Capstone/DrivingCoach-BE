package com.drivingcoach.backend.domain.driving.service;

import com.drivingcoach.backend.domain.driving.domain.entity.DrivingEvent;
import com.drivingcoach.backend.domain.driving.domain.entity.DrivingRecord;
import com.drivingcoach.backend.domain.driving.repository.DrivingEventRepository;
import com.drivingcoach.backend.domain.driving.repository.DrivingRecordRepository;
import com.drivingcoach.backend.domain.user.domain.entity.User;
import com.drivingcoach.backend.domain.user.repository.UserRepository;
import com.drivingcoach.backend.global.exception.CustomException;
import com.drivingcoach.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ DrivingService
 *
 * 역할
 *  - 운전 기록 생성/종료, 이벤트 등록, 목록/상세 조회 등
 *  - 컨트롤러에서 받은 userId 를 기준으로 "내 데이터만" 안전하게 처리
 *
 * 트랜잭션 전략
 *  - 클래스 기본: readOnly = true (조회 성능 최적화)
 *  - 변경이 있는 메서드(start/end/addEvent)는 @Transactional(readOnly = false)로 오버라이드
 *
 * 예외 처리
 *  - 존재하지 않는 사용자/기록 → USER_NOT_FOUND / NOT_FOUND
 *  - 소유자 불일치 방지: 조회 시 항상 userId 조건을 함께 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DrivingService {

    private final DrivingRecordRepository drivingRecordRepository;
    private final DrivingEventRepository drivingEventRepository;
    private final UserRepository userRepository;

    /* ======================= 기록 시작/종료 ======================= */

    /**
     * 주행 시작
     *
     * @param userId     내 사용자 ID
     * @param startTime  시작 시각(없으면 now)
     * @param videoKeyOrUrl S3 키 또는 URL(선택) — 없으면 null
     * @return 생성된 DrivingRecord ID
     */
    @Transactional
    public Long startDriving(Long userId, LocalDateTime startTime, String videoKeyOrUrl) {
        User user = getActiveUserOrThrow(userId);

        DrivingRecord record = DrivingRecord.builder()
                .user(user)
                .startTime(startTime != null ? startTime : LocalDateTime.now())
                .videoUrl(videoKeyOrUrl)
                .build();

        drivingRecordRepository.save(record);
        log.info("[DRIVING] start: userId={}, recordId={}", userId, record.getId());
        return record.getId();
    }

    /**
     * 주행 종료
     *
     * @param userId       내 사용자 ID
     * @param recordId     종료할 기록 ID
     * @param endTime      종료 시각(없으면 now)
     * @param finalScore   분석 점수(선택)
     * @param finalVideoKeyOrUrl 마지막 저장된 영상/zip 키나 URL(선택)
     */
    @Transactional
    public void endDriving(Long userId, Long recordId, LocalDateTime endTime, Float finalScore, String finalVideoKeyOrUrl) {
        DrivingRecord record = getRecordOrThrow(userId, recordId);

        record.endDriving(endTime != null ? endTime : LocalDateTime.now());
        record.updateScore(finalScore);
        if (finalVideoKeyOrUrl != null && !finalVideoKeyOrUrl.isBlank()) {
            record.updateVideoUrl(finalVideoKeyOrUrl);
        }

        drivingRecordRepository.save(record);
        log.info("[DRIVING] end: userId={}, recordId={}, totalSec={}", userId, recordId, record.getTotalTime());
    }

    /* ======================= 이벤트 등록/조회 ======================= */

    /**
     * 이벤트 한 건 추가
     *
     * @param userId    내 사용자 ID
     * @param recordId  대상 기록 ID(내 기록이어야 함)
     * @param type      이벤트 종류(예: "급가속", "lane_departure")
     * @param eventTime 이벤트 발생 시각(없으면 now)
     * @param severity  심각도("low","medium","high" 등)
     * @param note      메모(선택)
     * @return 저장된 DrivingEvent
     */
    @Transactional
    public DrivingEvent addEvent(Long userId, Long recordId, String type, LocalDateTime eventTime, String severity, String note) {
        DrivingRecord record = getRecordOrThrow(userId, recordId);

        DrivingEvent event = DrivingEvent.builder()
                .drivingRecord(record)
                .eventType(type)
                .eventTime(eventTime != null ? eventTime : LocalDateTime.now())
                .severity(severity != null ? severity : "low")
                .note(note)
                .build();

        // 연관 편의 메서드를 사용해도 되지만, 여기서는 명시적으로 저장
        drivingEventRepository.save(event);
        log.info("[EVENT] added: recordId={}, type={}, time={}", recordId, type, event.getEventTime());
        return event;
    }

    /**
     * 특정 기록의 이벤트 목록(시간 오름차순)
     */
    public List<DrivingEvent> listEventsAsc(Long userId, Long recordId) {
        // 소유권 확인
        getRecordOrThrow(userId, recordId);
        return drivingEventRepository.findAllByRecordIdOrderByTimeAsc(recordId);
    }

    /**
     * 특정 기록의 이벤트 페이지(시간 내림차순)
     */
    public Page<DrivingEvent> pageEventsDesc(Long userId, Long recordId, Pageable pageable) {
        getRecordOrThrow(userId, recordId);
        return drivingEventRepository.findPageByRecordId(recordId, pageable);
    }

    /* ======================= 기록 상세/목록 ======================= */

    /**
     * 기록 상세 조회 (필요 시 이벤트까지 fetch join)
     */
    public DrivingRecord getRecord(Long userId, Long recordId, boolean withEvents) {
        if (withEvents) {
            return drivingRecordRepository.findDetailWithEvents(recordId, userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "운전기록을 찾을 수 없습니다."));
        }
        return getRecordOrThrow(userId, recordId);
    }

    /**
     * 목록 조회(최신순)
     */
    public Page<DrivingRecord> pageMyRecords(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
        return drivingRecordRepository.findAllByUserIdOrderByStartTimeDesc(userId, pageable);
    }

    /**
     * 기간 필터 목록 조회(시작 시각 기준)
     */
    public Page<DrivingRecord> pageMyRecordsByPeriod(Long userId, LocalDateTime from, LocalDateTime to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
        return drivingRecordRepository.findByUserIdAndStartTimeBetween(userId, from, to, pageable);
    }

    /* ======================= 통계/요약 ======================= */

    /**
     * 기간 총 주행 시간(초)
     */
    public int sumTotalSeconds(Long userId, LocalDateTime from, LocalDateTime to) {
        Integer sum = drivingRecordRepository.sumTotalTimeByUserIdAndPeriod(userId, from, to);
        return sum != null ? sum : 0;
    }

    /**
     * 기간 평균 점수 (null 가능: 점수가 하나도 없으면)
     */
    public Double avgScore(Long userId, LocalDateTime from, LocalDateTime to) {
        return drivingRecordRepository.averageScoreByUserIdAndPeriod(userId, from, to);
    }

    /* ======================= 내부 헬퍼 ======================= */

    private User getActiveUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private DrivingRecord getRecordOrThrow(Long userId, Long recordId) {
        return drivingRecordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "운전기록을 찾을 수 없습니다."));
    }
}
