package com.drivingcoach.backend.domain.driving.controller;

import com.drivingcoach.backend.domain.driving.domain.entity.DrivingEvent;
import com.drivingcoach.backend.domain.driving.domain.entity.DrivingRecord;
import com.drivingcoach.backend.domain.driving.service.DrivingService;
import com.drivingcoach.backend.domain.user.domain.CustomUserDetails;
import com.drivingcoach.backend.global.response.ApiResponse;
import com.drivingcoach.backend.domain.driving.domain.dto.request.AddEventRequest;
import com.drivingcoach.backend.domain.driving.domain.dto.request.EndDrivingRequest;
import com.drivingcoach.backend.domain.driving.domain.dto.request.StartDrivingRequest;
import com.drivingcoach.backend.domain.driving.domain.dto.response.DrivingEventResponse;
import com.drivingcoach.backend.domain.driving.domain.dto.response.DrivingRecordResponse;
import com.drivingcoach.backend.domain.driving.domain.dto.response.DrivingSummaryResponse;
import com.drivingcoach.backend.domain.driving.domain.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ DrivingController
 *
 * 역할
 *  - 주행 시작/종료
 *  - 주행 기록 조회(상세/목록/기간)
 *  - 이벤트 등록/조회
 *  - 간단 통계(기간 총 주행 시간, 평균 점수)
 *
 * 보안
 *  - 모든 엔드포인트는 인증 필요(SecurityConfig에서 /api/auth/** 만 permitAll)
 *  - userId는 토큰에서 꺼낸 인증 주체(@AuthenticationPrincipal)로 판단
 */
@Tag(name = "Driving", description = "주행 기록/이벤트 API")
@RestController
@RequestMapping("/api/driving")
@RequiredArgsConstructor
public class DrivingController {

    private final DrivingService drivingService;

    /* ======================= 주행 시작/종료 ======================= */

    @Operation(summary = "주행 시작", description = "주행을 시작하고 기록 ID를 반환합니다.")
    @PostMapping("/start")
    public ApiResponse<Long> startDriving(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody StartDrivingRequest request
    ) {
        Long userId = principal.getUserId();
        Long recordId = drivingService.startDriving(
                userId,
                request.getStartTime(),          // null이면 서비스에서 now 처리
                request.getVideoKeyOrUrl()       // 선택값
        );
        return ApiResponse.ok(recordId);
    }

    @Operation(summary = "주행 종료", description = "주행을 종료하고 총 시간/점수/영상키 등을 반영합니다.")
    @PostMapping("/end")
    public ApiResponse<Void> endDriving(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EndDrivingRequest request
    ) {
        Long userId = principal.getUserId();
        drivingService.endDriving(
                userId,
                request.getRecordId(),
                request.getEndTime(),             // null이면 서비스에서 now 처리
                request.getFinalScore(),          // 선택값
                request.getFinalVideoKeyOrUrl()   // 선택값
        );
        return ApiResponse.ok();
    }

    /* ======================= 이벤트 등록/조회 ======================= */

    @Operation(summary = "이벤트 등록", description = "특정 주행 기록에 이벤트(급가속 등)를 추가합니다.")
    @PostMapping("/{recordId}/events")
    public ApiResponse<DrivingEventResponse> addEvent(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long recordId,
            @Valid @RequestBody AddEventRequest request
    ) {
        Long userId = principal.getUserId();
        DrivingEvent saved = drivingService.addEvent(
                userId,
                recordId,
                request.getType(),
                request.getEventTime(),            // null이면 now
                request.getSeverity(),            // null이면 low
                request.getNote()
        );
        return ApiResponse.ok(DrivingEventResponse.from(saved));
    }

    @Operation(summary = "이벤트 목록(시간 오름차순)", description = "특정 주행 기록의 이벤트 전체를 시간 오름차순으로 반환합니다(소량일 때 권장).")
    @GetMapping("/{recordId}/events/asc")
    public ApiResponse<List<DrivingEventResponse>> listEventsAsc(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long recordId
    ) {
        Long userId = principal.getUserId();
        List<DrivingEvent> events = drivingService.listEventsAsc(userId, recordId);
        return ApiResponse.ok(events.stream().map(DrivingEventResponse::from).toList());
    }

    @Operation(summary = "이벤트 페이지(시간 내림차순)", description = "특정 주행 기록의 이벤트를 페이지네이션으로 조회합니다.")
    @GetMapping("/{recordId}/events")
    public ApiResponse<PageResponse<DrivingEventResponse>> pageEvents(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long recordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = principal.getUserId();
        Page<DrivingEvent> p = drivingService.pageEventsDesc(userId, recordId, org.springframework.data.domain.PageRequest.of(page, size));
        Page<DrivingEventResponse> mapped = new PageImpl<>(
                p.getContent().stream().map(DrivingEventResponse::from).toList(),
                p.getPageable(),
                p.getTotalElements()
        );
        return ApiResponse.ok(PageResponse.from(mapped));
    }

    /* ======================= 기록 상세/목록 ======================= */

    @Operation(summary = "주행 기록 상세", description = "단건 주행 기록을 조회합니다. withEvents=true면 이벤트까지 포함(fetch join 기반).")
    @GetMapping("/{recordId}")
    public ApiResponse<DrivingRecordResponse> getRecord(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long recordId,
            @RequestParam(defaultValue = "false") boolean withEvents
    ) {
        Long userId = principal.getUserId();
        DrivingRecord record = drivingService.getRecord(userId, recordId, withEvents);
        return ApiResponse.ok(DrivingRecordResponse.from(record, withEvents));
    }

    @Operation(summary = "내 주행 기록 목록(최신순)", description = "페이지네이션으로 내 주행 기록을 조회합니다.")
    @GetMapping
    public ApiResponse<PageResponse<DrivingRecordResponse>> pageMyRecords(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = principal.getUserId();
        Page<DrivingRecord> p = drivingService.pageMyRecords(userId, page, size);
        Page<DrivingRecordResponse> mapped = p.map(r -> DrivingRecordResponse.from(r, false));
        return ApiResponse.ok(PageResponse.from(mapped));
    }

    @Operation(summary = "기간별 내 주행 기록", description = "from~to(미포함) 기간으로 필터링해 페이지네이션합니다.")
    @GetMapping("/period")
    public ApiResponse<PageResponse<DrivingRecordResponse>> pageMyRecordsByPeriod(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = principal.getUserId();
        Page<DrivingRecord> p = drivingService.pageMyRecordsByPeriod(userId, from, to, page, size);
        Page<DrivingRecordResponse> mapped = p.map(r -> DrivingRecordResponse.from(r, false));
        return ApiResponse.ok(PageResponse.from(mapped));
    }

    /* ======================= 간단 통계 ======================= */

    @Operation(summary = "기간 요약(총 주행 시간/평균 점수)", description = "from~to(미포함) 기간의 총 주행 시간(초), 평균 점수를 반환합니다.")
    @GetMapping("/stats/summary")
    public ApiResponse<DrivingSummaryResponse> summary(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to
    ) {
        Long userId = principal.getUserId();
        int totalSeconds = drivingService.sumTotalSeconds(userId, from, to);
        Double avgScore = drivingService.avgScore(userId, from, to);
        return ApiResponse.ok(new DrivingSummaryResponse(totalSeconds, avgScore));
    }
}
