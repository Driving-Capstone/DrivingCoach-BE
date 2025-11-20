// domain/driving/controller/HistoryController.java
package com.drivingcoach.backend.domain.driving.controller;

import com.drivingcoach.backend.domain.driving.domain.dto.request.HistoryDateRequest;
import com.drivingcoach.backend.domain.driving.domain.dto.request.HistoryDetailRequest;
import com.drivingcoach.backend.domain.driving.domain.dto.response.HistoryDetailResponse;
import com.drivingcoach.backend.domain.driving.domain.dto.response.HistoryResponse;
import com.drivingcoach.backend.domain.driving.service.HistoryService; // 서비스도 분리 추천
import com.drivingcoach.backend.domain.user.domain.CustomUserDetails;
import com.drivingcoach.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "History", description = "주행 기록 조회 API")
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @Operation(summary = "최근순 조회")
    @PostMapping("/recent")
    public ApiResponse<List<HistoryResponse>> getRecentHistory(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(historyService.getHistoryRecent(principal.getUserId()));
    }

    @Operation(summary = "운전시간순 조회")
    @PostMapping("/drivingtime")
    public ApiResponse<List<HistoryResponse>> getHistoryByTime(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(historyService.getHistoryByDrivingTime(principal.getUserId()));
    }

    @Operation(summary = "최근순 조회 (날짜 선택)")
    @PostMapping("/recent/date")
    public ApiResponse<List<HistoryResponse>> getRecentHistoryByDate(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody HistoryDateRequest request) {
        return ApiResponse.ok(historyService.getHistoryRecentByDate(principal.getUserId(), request));
    }

    @Operation(summary = "운전시간순 조회 (날짜 선택)")
    @PostMapping("/drivingtime/date")
    public ApiResponse<List<HistoryResponse>> getHistoryByTimeAndDate(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody HistoryDateRequest request) {
        return ApiResponse.ok(historyService.getHistoryByTimeAndDate(principal.getUserId(), request));
    }

    @Operation(summary = "주행 상세 조회")
    @PostMapping("/details")
    public ApiResponse<HistoryDetailResponse> getHistoryDetails(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody HistoryDetailRequest request) {
        return ApiResponse.ok(historyService.getHistoryDetail(principal.getUserId(), request.getDrivingId()));
    }
}