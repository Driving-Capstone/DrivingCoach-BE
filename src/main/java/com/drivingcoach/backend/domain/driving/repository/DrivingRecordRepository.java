package com.drivingcoach.backend.domain.driving.repository;

import com.drivingcoach.backend.domain.driving.domain.entity.DrivingRecord;
import com.drivingcoach.backend.domain.user.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ✅ DrivingRecordRepository
 *
 * 역할
 *  - 운전 기록(DrivingRecord)에 대한 표준 CRUD 및 자주 쓰이는 조회 쿼리를 제공합니다.
 *
 * 설계 포인트
 *  1) "내 기록만" 조회하는 케이스가 대부분 → userId 조건을 자주 사용
 *  2) 기간 필터(시작/종료 시각 기준) + 정렬/페이지네이션 조합이 흔함
 *  3) 상세 조회 시 이벤트 목록을 함께 보고 싶은 경우가 있어 fetch join 쿼리도 준비
 */
public interface DrivingRecordRepository extends JpaRepository<DrivingRecord, Long> {

    /* ==================== 단건 조회 ==================== */

    /**
     * 특정 사용자의 활성 기록 단건 조회
     * - 보안 관점에서 "id만 알면 남의 기록 조회"를 방지하기 위해 userId 를 함께 받는 패턴 권장
     */
    @Query("""
           select dr
             from DrivingRecord dr
            where dr.id = :recordId
              and dr.user.id = :userId
           """)
    Optional<DrivingRecord> findByIdAndUserId(@Param("recordId") Long recordId,
                                              @Param("userId") Long userId);

    /**
     * 상세 화면에서 이벤트까지 한 번에 가져오고 싶은 경우
     * - fetch join 사용 (단, 페이징과 함께 쓰지 않는 것을 권장)
     * - events 가 많아질 수 있으니 실제 사용처/부하를 고려해 선택적으로 사용
     */
    @Query("""
           select dr
             from DrivingRecord dr
             left join fetch dr.events e
            where dr.id = :recordId
              and dr.user.id = :userId
           """)
    Optional<DrivingRecord> findDetailWithEvents(@Param("recordId") Long recordId,
                                                 @Param("userId") Long userId);

    /* ==================== 리스트/페이지 조회 ==================== */

    /**
     * 사용자의 모든 운전 기록을 최신순으로 페이지네이션
     * - 모바일 앱의 "기록실" 목록 화면에 적합
     */
    @Query("""
           select dr
             from DrivingRecord dr
            where dr.user.id = :userId
           order by dr.startTime desc
           """)
    Page<DrivingRecord> findAllByUserIdOrderByStartTimeDesc(@Param("userId") Long userId,
                                                            Pageable pageable);

    /**
     * 기간 필터(시작~종료)를 적용한 페이지 조회
     * - 특정 월/주차/임의 구간의 기록만 불러올 때 사용
     * - endTime 이 null(진행중)인 케이스를 포함할지 여부는 요구사항에 따라 조정 가능
     */
    @Query("""
           select dr
             from DrivingRecord dr
            where dr.user.id = :userId
              and dr.startTime >= :from
              and dr.startTime < :to
           order by dr.startTime desc
           """)
    Page<DrivingRecord> findByUserIdAndStartTimeBetween(@Param("userId") Long userId,
                                                        @Param("from") LocalDateTime from,
                                                        @Param("to") LocalDateTime to,
                                                        Pageable pageable);

    /* ==================== 통계용 간단 집계 ==================== */

    /**
     * 특정 기간 동안의 총 주행 시간(초)을 합산
     * - 홈/요약 카드에서 "이번 주 총 주행 시간" 등 표시용
     * - null 방지를 위해 coalesce 사용 (JPQL에서는 function 호출로 표현)
     */
    @Query("""
           select coalesce(sum(dr.totalTime), 0)
             from DrivingRecord dr
            where dr.user.id = :userId
              and dr.startTime >= :from
              and dr.startTime < :to
           """)
    Integer sumTotalTimeByUserIdAndPeriod(@Param("userId") Long userId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    /**
     * 특정 기간 동안의 평균 점수(소수점) 계산
     * - 점수가 아직 null 인 기록은 제외되는 점에 유의
     */
    @Query("""
           select avg(dr.score)
             from DrivingRecord dr
            where dr.user.id = :userId
              and dr.startTime >= :from
              and dr.startTime < :to
              and dr.score is not null
           """)
    Double averageScoreByUserIdAndPeriod(@Param("userId") Long userId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    /* ==================== 기타 유틸 ==================== */

    /**
     * 가장 최근 주행 기록 1건 (시작 시각 기준)
     * - 홈 화면 카드 등에서 "마지막 주행" 노출용
     */
    @Query("""
           select dr
             from DrivingRecord dr
            where dr.user.id = :userId
           order by dr.startTime desc
           """)
    List<DrivingRecord> findTop1ByUserIdOrderByStartTimeDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 편의 메서드: 스프링 데이터 메서드 네이밍 기반의 빠른 조회
     * - 단, 위 JPQL 커스텀 쿼리들과 중복 사용은 지양하고 팀 컨벤션에 맞춰 통일하세요.
     */
    Page<DrivingRecord> findByUserOrderByStartTimeDesc(User user, Pageable pageable);

    /** 사용자별 총 주행 횟수 조회 */
    long countByUserId(Long userId);

    /** 사용자별 총 주행 시간(초) 합계 조회 */
    @Query("select coalesce(sum(dr.totalTime), 0) from DrivingRecord dr where dr.user.id = :userId")
    Long sumTotalTimeByUserId(@Param("userId") Long userId);

    /** 사용자별 평균 점수 조회 */
    @Query("select avg(dr.score) from DrivingRecord dr where dr.user.id = :userId")
    Double findAverageScoreByUserId(@Param("userId") Long userId);

    // 1. 이번 달 통계용 (월간 집계)
    @Query("select dr from DrivingRecord dr where dr.user.id = :userId and year(dr.startTime) = :year and month(dr.startTime) = :month")
    List<DrivingRecord> findByUserIdAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

    // 2. 주행 시간 순 정렬 (내림차순)
    @Query("select dr from DrivingRecord dr where dr.user.id = :userId order by dr.totalTime desc")
    List<DrivingRecord> findAllByUserIdOrderByTotalTimeDesc(@Param("userId") Long userId);

    // 3. 날짜 필터링 (연, 월, 일) + 최신순
    @Query("""
           select dr from DrivingRecord dr
           where dr.user.id = :userId
             and year(dr.startTime) = :year
             and month(dr.startTime) = :month
             and day(dr.startTime) = :day
           order by dr.startTime desc
           """)
    List<DrivingRecord> findAllByDateOrderByStartTimeDesc(@Param("userId") Long userId,
                                                          @Param("year") int year,
                                                          @Param("month") int month,
                                                          @Param("day") int day);

    // 4. 날짜 필터링 + 주행시간순
    @Query("""
           select dr from DrivingRecord dr
           where dr.user.id = :userId
             and year(dr.startTime) = :year
             and month(dr.startTime) = :month
             and day(dr.startTime) = :day
           order by dr.totalTime desc
           """)
    List<DrivingRecord> findAllByDateOrderByTotalTimeDesc(@Param("userId") Long userId,
                                                          @Param("year") int year,
                                                          @Param("month") int month,
                                                          @Param("day") int day);

    /**
     * (추가) 특정 기간 내 주행 횟수 카운트
     */
    long countByUserIdAndStartTimeBetween(Long userId, LocalDateTime from, LocalDateTime to);
    /**
     * (추가) 특정 기간 내 발생한 총 이벤트(DrivingEvent) 개수 카운트
     * - DrivingEvent와 Join하여 개수를 셉니다.
     */
    @Query("""
           select count(e)
             from DrivingEvent e
             join e.drivingRecord dr
            where dr.user.id = :userId
              and dr.startTime >= :from
              and dr.startTime < :to
           """)
    long countEventsByUserIdAndPeriod(@Param("userId") Long userId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    /** 단순 페이징 조회 (정렬은 Pageable에 포함됨) */
    Page<DrivingRecord> findAllByUserId(Long userId, Pageable pageable);
}
