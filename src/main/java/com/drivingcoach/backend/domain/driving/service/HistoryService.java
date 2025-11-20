package com.drivingcoach.backend.domain.driving.service;

import com.drivingcoach.backend.domain.driving.domain.dto.request.HistoryDateRequest;
import com.drivingcoach.backend.domain.driving.domain.dto.response.HistoryDetailResponse;
import com.drivingcoach.backend.domain.driving.domain.dto.response.HistoryListResponse;
import com.drivingcoach.backend.domain.driving.domain.dto.response.HistoryResponse;
import com.drivingcoach.backend.domain.driving.domain.dto.response.HistorySummaryResponse;
import com.drivingcoach.backend.domain.driving.domain.entity.DrivingRecord;
import com.drivingcoach.backend.domain.driving.repository.DrivingEventRepository;
import com.drivingcoach.backend.domain.driving.repository.DrivingRecordRepository;
import com.drivingcoach.backend.global.exception.CustomException;
import com.drivingcoach.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryService {


    private final DrivingRecordRepository drivingRecordRepository;
    private final DrivingEventRepository drivingEventRepository;

    /**
     * 기록실 목록 조회 (필터 + 정렬 + 페이징 + 요약)
     * @param filterDate 날짜 필터 (null이면 전체)
     * @param sortBy 정렬 기준 ("recent", "time", "events")
     * @param sortDir 정렬 방향 ("asc", "desc")
     */
    public HistoryListResponse getHistoryList(Long userId, LocalDate filterDate, String sortBy, String sortDir, int page, int size) {

        // 1. 정렬 객체 생성
        Sort sort = Sort.unsorted();
        if ("recent".equals(sortBy)) {
            sort = Sort.by("desc".equals(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, "startTime");
        } else if ("time".equals(sortBy)) {
            sort = Sort.by("desc".equals(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, "totalTime");
        }
        // "events" 정렬은 엔티티 필드가 아니므로 아래에서 별도 처리 필요 (일단 시간순 default)

        PageRequest pageable = PageRequest.of(page, size, sort);
        Page<DrivingRecord> recordPage;

        // 2. DB 조회 (날짜 필터 여부에 따라 분기)
        if (filterDate != null) {
            // 해당 날짜의 00:00 ~ 23:59
            LocalDateTime from = filterDate.atStartOfDay();
            LocalDateTime to = filterDate.plusDays(1).atStartOfDay();
            recordPage = drivingRecordRepository.findByUserIdAndStartTimeBetween(userId, from, to, pageable);
        } else {
            // 전체 조회
            recordPage = drivingRecordRepository.findAllByUserId(userId, pageable);
        }

        // *이벤트 순 정렬*은 DB 레벨에서 하려면 복잡하므로,
        // 데이터 양이 많지 않다면 가져온 페이지 내에서 정렬하거나,
        // @Query로 size(events) 정렬을 구현해야 합니다. (여기선 생략, 필요시 추가 요청)

        // 3. 리스트 변환
        List<HistoryResponse> list = recordPage.getContent().stream()
                .map(HistoryResponse::from)
                .collect(Collectors.toList());

        // 4. 상단 요약 정보 계산 (날짜 필터가 있어도 요약은 '전체' 기준인지, '필터된' 기준인지 기획에 따름. 보통은 전체 통계 보여줌)
        long totalCount = drivingRecordRepository.countByUserId(userId);
        Long totalTimeSec = drivingRecordRepository.sumTotalTimeByUserId(userId);
        long totalEvents = drivingEventRepository.countAllEventsByUserId(userId);

        double totalHours = (totalTimeSec != null) ? Math.round((totalTimeSec / 3600.0) * 10) / 10.0 : 0.0;

        HistorySummaryResponse summary = HistorySummaryResponse.builder()
                .totalDrivingCount(totalCount)
                .totalDrivingTime(totalHours + "시간")
                .totalEventCount(totalEvents)
                .build();

        return HistoryListResponse.builder()
                .summary(summary)
                .records(list)
                .build();
    }

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