package com.drivingcoach.backend.domain.home.service;

import com.drivingcoach.backend.domain.driving.domain.entity.DrivingRecord;
import com.drivingcoach.backend.domain.driving.repository.DrivingRecordRepository;
import com.drivingcoach.backend.domain.home.domain.dto.response.HomeMonthStatusResponse;
import com.drivingcoach.backend.domain.home.domain.dto.response.HomeRecentRecordResponse;
import com.drivingcoach.backend.domain.home.domain.dto.response.WeeklyStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ✅ HomeService
 *
 * 역할
 *  - 홈 화면의 "주간 요약" 데이터를 조합/반환합니다.
 *  - 총 주행 시간, 평균 점수, 일자별 집계(차트용)를 제공합니다.
 *
 * 설계 포인트
 *  1) 기간은 반개구간 [from, to) 로 처리하여 경계 중복을 방지합니다.
 *  2) 일자별 집계는 서버 타임존 기준으로 "자정 경계"를 사용해 버킷팅합니다.
 *  3) 저장된 totalTime(초)은 기록 단위의 총 주행 시간이며, 단순히 startTime 기준으로 버킷팅합니다.
 *     (정밀한 "일자별 분할"이 필요하면 start~end 구간을 쪼개 합산하는 고급 로직으로 확장 가능)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final DrivingRecordRepository drivingRecordRepository;

    /**
     * 주간(또는 임의 기간) 요약 생성
     *
     * @param userId 사용자 ID
     * @param from   포함 시작 (예: 2025-09-22T00:00:00)
     * @param to     제외 종료 (예: 2025-09-29T00:00:00)
     */
    public WeeklyStatusResponse buildWeeklyStatus(Long userId, LocalDateTime from, LocalDateTime to) {
        // 1) 총 주행 시간(초)
        int totalSec = drivingRecordRepository.sumTotalTimeByUserIdAndPeriod(userId, from, to);

        // 2) 평균 점수 (null 가능)
        Double avgScore = drivingRecordRepository.averageScoreByUserIdAndPeriod(userId, from, to);

        // 3) (추가) 총 주행 횟수
        long drivingCount = drivingRecordRepository.countByUserIdAndStartTimeBetween(userId, from, to);

        // 4) (추가) 총 이벤트 발생 횟수
        long eventCount = drivingRecordRepository.countEventsByUserIdAndPeriod(userId, from, to);

        // 5) 일자별 버킷 합계
        List<WeeklyStatusResponse.DayBucket> buckets = aggregateDailySeconds(userId, from, to);

        // 6) 마지막 주행(최근 1건) 요약 (선택: 홈 화면 카드용)
        DrivingRecord last = drivingRecordRepository
                .findTop1ByUserIdOrderByStartTimeDesc(userId, PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);

        WeeklyStatusResponse.LastDriving lastDriving = null;
        if (last != null) {
            lastDriving = WeeklyStatusResponse.LastDriving.builder()
                    .recordId(last.getId())
                    .startTime(last.getStartTime())
                    .endTime(last.getEndTime())
                    .totalSeconds(last.getTotalTime())
                    .score(last.getScore())
                    .build();
        }

        return WeeklyStatusResponse.builder()
                .from(from)
                .to(to)
                .totalSeconds(totalSec)
                .totalDrivingCount(drivingCount) // (추가)
                .totalEventCount(eventCount)     // (추가)
                .averageScore(avgScore)
                .dailySeconds(buckets)
                .lastDriving(lastDriving)
                .build();
    }

    /**
     * 일자별 집계
     * - 단순화: 각 DrivingRecord 의 startTime 날짜 버킷에 totalTime(초)을 더합니다.
     * - 더 정밀한 "일자 경계 분할"이 필요하면 start~end 구간을 날짜별로 나눠 누적하는 로직으로 확장하세요.
     */
    private List<WeeklyStatusResponse.DayBucket> aggregateDailySeconds(Long userId, LocalDateTime from, LocalDateTime to) {
        ZoneId zone = ZoneId.systemDefault();

        // 날짜 경계 계산: [fromDate, toDate) 일 단위 반복
        LocalDate fromDate = from.atZone(zone).toLocalDate();
        LocalDate toDate = to.atZone(zone).toLocalDate();

        // 버킷 미리 0으로 채워 초기화
        List<WeeklyStatusResponse.DayBucket> buckets = new ArrayList<>();
        for (LocalDate d = fromDate; d.isBefore(toDate); d = d.plusDays(1)) {
            buckets.add(new WeeklyStatusResponse.DayBucket(d, 0));
        }

        // 해당 기간의 기록 페이지 없이 전부 가져오고 싶다면 별도 조회가 필요하지만,
        // 여기서는 간단히 페이지 크기를 충분히 크게 잡거나, practical 하게 일자별로 합계를 구하는 쿼리를 추가하는 방법도 가능합니다.
        // 우선 간단 구현: 페이지 없이 from~to 범위의 레코드를 가져오도록 레포지토리 메서드 확장 없이 처리하려면,
        // 한 번에 많은 데이터가 로드될 수 있으므로 운영에서는 주의하세요.
        // -> 여기서는 범위 내 레코드가 과도하지 않다는 가정 하에 간단한 쿼리를 추가하지 않고 처리합니다.
        //    (원한다면 findByUserIdAndStartTimeBetween(...) 의 Page 대신, size를 크게 하거나 커스텀 findAll 메서드를 추가하세요)

        // 간단 접근: 31일 이내라는 가정을 두고 충분히 큰 페이지로 1회 로드
        var page = drivingRecordRepository.findByUserIdAndStartTimeBetween(
                userId, from, to, PageRequest.of(0, 1000));

        page.getContent().forEach(rec -> {
            LocalDate bucketDate = rec.getStartTime().atZone(zone).toLocalDate();
            int idx = (int) (bucketDate.toEpochDay() - fromDate.toEpochDay());
            if (idx >= 0 && idx < buckets.size()) {
                WeeklyStatusResponse.DayBucket prev = buckets.get(idx);
                buckets.set(idx, new WeeklyStatusResponse.DayBucket(prev.date(), prev.seconds() + (rec.getTotalTime() != null ? rec.getTotalTime() : 0)));
            }
        });

        return buckets;
    }

    public HomeMonthStatusResponse getMonthStatus(Long userId) {
        LocalDate now = LocalDate.now();
        List<DrivingRecord> records = drivingRecordRepository.findByUserIdAndMonth(userId, now.getYear(), now.getMonthValue());

        int totalDriving = records.size();
        long totalSeconds = records.stream().mapToLong(DrivingRecord::getTotalTime).sum();
        double hours = Math.round((totalSeconds / 3600.0) * 10) / 10.0;

        // 경고 알림 횟수는 Event의 개수 합이라고 가정
        int warningCount = records.stream()
                .mapToInt(r -> r.getEvents().size())
                .sum();

        return HomeMonthStatusResponse.builder()
                .totalDriving(totalDriving)
                .drivingHours(hours)
                .warningCount(warningCount)
                .build();
    }

    /**
     * 최근 주행 기록 5건 조회 (수정됨)
     */
    public List<HomeRecentRecordResponse> getRecentRecords(Long userId) {
        // 1. 최근순으로 5개 조회 (PageRequest 사용)
        List<DrivingRecord> records = drivingRecordRepository.findAllByUserIdOrderByStartTimeDesc(
                userId,
                PageRequest.of(0, 5) // 0페이지, 사이즈 5
        ).getContent();

        // 2. 엔티티 리스트 -> DTO 리스트 변환
        return records.stream()
                .map(record -> {
                    // 점수에 따른 메시지 로직 (기존 로직 유지 또는 수정)
                    String msg = (record.getScore() != null && record.getScore() >= 80) ? "안전" : "보통";
                    if (record.getScore() != null && record.getScore() < 60) msg = "주의"; // 예시 추가

                    return HomeRecentRecordResponse.builder()
                            .drivingId(record.getId())
                            .startYear(record.getStartTime().getYear())
                            .startMonth(record.getStartTime().getMonthValue())
                            .startDay(record.getStartTime().getDayOfMonth())
                            // 시:분 형식 포맷팅
                            .startTime(String.format("%02d:%02d", record.getStartTime().getHour(), record.getStartTime().getMinute()))
                            // 초 -> 분 변환 (예: 60초 -> 1분)
                            .drivingTime(record.getTotalTime() / 60)
                            .drivingScoreMessage(msg)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
