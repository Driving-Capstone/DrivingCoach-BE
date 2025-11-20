package com.drivingcoach.backend.domain.home.controller;

import com.drivingcoach.backend.domain.home.domain.dto.response.HomeMonthStatusResponse;
import com.drivingcoach.backend.domain.home.domain.dto.response.HomeRecentRecordResponse;
import com.drivingcoach.backend.domain.home.service.HomeService;
import com.drivingcoach.backend.domain.home.domain.dto.response.WeeklyStatusResponse;
import com.drivingcoach.backend.domain.user.domain.CustomUserDetails;
import com.drivingcoach.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import java.time.*;

/**
 * ✅ HomeController
 *
 * 역할
 *  - 앱 홈 화면에서 필요한 "기간 요약" 데이터를 제공하는 컨트롤러입니다.
 *  - 현재는 "주간 요약" 엔드포인트를 제공합니다.
 *
 * 설계 메모
 *  - 기간 계산은 서버 기준(타임존 고려). 프론트가 명시적으로 from/to 를 주면 그 값을 우선합니다.
 *  - 기본값은 "오늘을 끝점으로 과거 7일" 구간입니다. [from, to) 반개구간을 사용합니다.
 */
@Tag(name = "Home", description = "홈 화면 요약 API")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /**
     * 🗓️ 주간 요약
     *
     * - from, to(미포함) 파라미터를 받습니다. (둘 다 생략 시: 기본값=오늘 00:00 기준 과거 7일)
     * - 반환: 총 주행 시간(초), 평균 점수, 일자별 총 주행(초) 차트용 배열 등
     *
     * 예) GET /api/home/weekly-status
     * 예) GET /api/home/weekly-status?from=2025-09-22T00:00:00&to=2025-09-29T00:00:00
     */
    @Operation(summary = "주간 요약", description = "기간 내 총 주행 시간/평균 점수/일자별 통계를 제공합니다.")
    @GetMapping("/weekly-status")
    public ApiResponse<WeeklyStatusResponse> weeklyStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        Long userId = principal.getUserId();

        // 타임존 보정: 서버 로컬 타임존 기준으로 일자 경계 정렬
        ZoneId zone = ZoneId.systemDefault();

        // 기본 구간: 오늘 00:00을 to 로 보고 7일 전 00:00을 from 으로 사용 (반개구간 [from, to))
        if (to == null) {
            LocalDate today = LocalDate.now(zone);
            to = today.atStartOfDay(); // 오늘 00:00
        }
        if (from == null) {
            from = to.minusDays(7);
        }

        WeeklyStatusResponse resp = homeService.buildWeeklyStatus(userId, from, to);
        return ApiResponse.ok(resp);
    }

    @GetMapping("/monthStatus")
    public ApiResponse<HomeMonthStatusResponse> getMonthStatus(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(homeService.getMonthStatus(principal.getUserId()));
    }

    /**
     * 최근 주행 기록 5건 조회 (수정됨)
     * 반환 타입: ApiResponse<List<HomeRecentRecordResponse>>
     */
    @Operation(summary = "최근 주행 기록 (상위 5개)", description = "가장 최근 주행 기록 5건을 리스트로 반환합니다.")
    @GetMapping("/recentRecord")
    public ApiResponse<List<HomeRecentRecordResponse>> getRecentRecords(@AuthenticationPrincipal CustomUserDetails principal) {
        List<HomeRecentRecordResponse> response = homeService.getRecentRecords(principal.getUserId());
        return ApiResponse.ok(response);
    }
}
