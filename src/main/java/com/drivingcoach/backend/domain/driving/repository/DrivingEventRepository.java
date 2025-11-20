package com.drivingcoach.backend.domain.driving.repository;

import com.drivingcoach.backend.domain.driving.domain.entity.DrivingEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ DrivingEventRepository
 *
 * 역할
 *  - 운전 중 발생한 이벤트(급가속, 급감속, 차선이탈, 신호위반 등)에 대한
 *    CRUD 및 조회 편의 쿼리를 제공합니다.
 *
 * 설계 포인트
 *  1) "특정 운전기록(recordId)의 이벤트 목록"을 시간순으로 자주 조회 → recordId + eventTime 인덱스 권장
 *  2) 사용자 단위로 기간 필터/유형 필터를 적용한 통계 조회가 필요할 수 있음
 *  3) 목록 화면/타임라인 싱크를 위해 페이지네이션 + 정렬 조합을 빈번히 사용
 */
public interface DrivingEventRepository extends JpaRepository<DrivingEvent, Long> {

    /* ================== 기본 목록 조회 ================== */

    /**
     * 특정 운전기록에 속한 모든 이벤트를 시간 오름차순으로 조회
     * - 타임라인에 이벤트 마커를 시간 순으로 찍을 때 유용
     */
    @Query("""
           select e
             from DrivingEvent e
            where e.drivingRecord.id = :recordId
           order by e.eventTime asc
           """)
    List<DrivingEvent> findAllByRecordIdOrderByTimeAsc(@Param("recordId") Long recordId);

    /**
     * 특정 운전기록의 이벤트를 페이지네이션으로 조회 (기본 시간 내림차순)
     * - 이벤트가 많을 때 화면에서 스크롤 페이지네이션 구현 용이
     */
    @Query("""
           select e
             from DrivingEvent e
            where e.drivingRecord.id = :recordId
           order by e.eventTime desc
           """)
    Page<DrivingEvent> findPageByRecordId(@Param("recordId") Long recordId, Pageable pageable);

    /* ============== 기간/유형/심각도 필터 ============== */

    /**
     * 운전기록 기준 + 기간 필터 (시작 이상, 종료 미만)
     * - 일부 클라이언트에서 eventTime 을 서버 기준으로 업로드한다고 가정
     */
    @Query("""
           select e
             from DrivingEvent e
            where e.drivingRecord.id = :recordId
              and e.eventTime >= :from
              and e.eventTime < :to
           order by e.eventTime asc
           """)
    List<DrivingEvent> findByRecordIdAndPeriod(@Param("recordId") Long recordId,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

    /**
     * 운전기록 기준 + 이벤트 유형 필터
     * - 예: "급가속" 만 보거나, "lane_departure" 만 보거나 등
     */
    @Query("""
           select e
             from DrivingEvent e
            where e.drivingRecord.id = :recordId
              and e.eventType = :eventType
           order by e.eventTime asc
           """)
    List<DrivingEvent> findByRecordIdAndType(@Param("recordId") Long recordId,
                                             @Param("eventType") String eventType);

    /**
     * 운전기록 기준 + 심각도 필터
     * - 예: "high" 이벤트만 모아보기
     */
    @Query("""
           select e
             from DrivingEvent e
            where e.drivingRecord.id = :recordId
              and e.severity = :severity
           order by e.eventTime asc
           """)
    List<DrivingEvent> findByRecordIdAndSeverity(@Param("recordId") Long recordId,
                                                 @Param("severity") String severity);

    /* ============== 간단 집계 ============== */

    /**
     * 운전기록별 이벤트 총 개수
     * - 상세 화면 헤더에 "이벤트 N건" 같은 요약 표시용
     */
    @Query("""
           select count(e)
             from DrivingEvent e
            where e.drivingRecord.id = :recordId
           """)
    long countByRecordId(@Param("recordId") Long recordId);

    /**
     * 운전기록별 유형별 이벤트 개수
     * - 카테고리 차트(파이/바) 구성에 활용 가능
     */
    @Query("""
           select e.eventType as type, count(e) as cnt
             from DrivingEvent e
            where e.drivingRecord.id = :recordId
            group by e.eventType
           """)
    List<Object[]> countByType(@Param("recordId") Long recordId);

    /**
     * 운전기록별 심각도별 이벤트 개수
     * - 위험도 요약 카드(LOW/MEDIUM/HIGH) 등
     */
    @Query("""
           select e.severity as severity, count(e) as cnt
             from DrivingEvent e
            where e.drivingRecord.id = :recordId
            group by e.severity
           """)
    List<Object[]> countBySeverity(@Param("recordId") Long recordId);
    /**
     * (추가) 특정 유저의 모든 주행 기록에서 발생한 이벤트 총 개수
     */
    @Query("select count(e) from DrivingEvent e join e.drivingRecord dr where dr.user.id = :userId")
    long countAllEventsByUserId(@Param("userId") Long userId);
}
