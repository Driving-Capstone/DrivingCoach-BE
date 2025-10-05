package com.drivingcoach.backend.domain.setting.controller;

import com.drivingcoach.backend.domain.setting.domain.entity.Setting;
import com.drivingcoach.backend.domain.setting.domain.dto.request.UpdateVibrationRequest;
import com.drivingcoach.backend.domain.setting.domain.dto.request.UpdateVoiceRequest;
import com.drivingcoach.backend.domain.setting.domain.dto.request.UpsertSettingRequest;
import com.drivingcoach.backend.domain.setting.domain.dto.response.SettingResponse;
import com.drivingcoach.backend.domain.setting.service.SettingService;
import com.drivingcoach.backend.domain.user.domain.CustomUserDetails;
import com.drivingcoach.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * ✅ SettingController
 *
 * 역할
 *  - 사용자 환경설정(Setting)에 대한 조회/수정 REST API를 제공합니다.
 *  - 인증된 사용자(@AuthenticationPrincipal CustomUserDetails)의 userId로 동작합니다.
 *
 * 엔드포인트 개요
 *  - GET    /api/setting/me              : 내 설정 조회 (없으면 기본값 생성 후 반환)
 *  - PATCH  /api/setting/vibration       : 진동 on/off 변경 (부분 업데이트)
 *  - PATCH  /api/setting/voice           : 음성 피드백 모드 변경 (부분 업데이트)
 *  - PUT    /api/setting                 : 설정 일괄 업서트(존재하지 않으면 생성, 있으면 갱신)
 *
 * 주의
 *  - 컨트롤러는 얇게 유지하고, 검증/업무 규칙은 서비스에 위임합니다.
 */
@Tag(name = "Setting", description = "사용자 환경설정 API")
@RestController
@RequestMapping("/api/setting")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    /* ========================= 조회 ========================= */

    @Operation(summary = "내 설정 조회", description = "현재 로그인한 사용자의 환경설정을 반환합니다. 없으면 기본값으로 생성합니다.")
    @GetMapping("/me")
    public ApiResponse<SettingResponse> getMySetting(@AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getUserId();
        Setting setting = settingService.getMySettingOrCreate(userId);
        return ApiResponse.ok(SettingResponse.from(setting));
    }

    /* ========================= 부분 업데이트 ========================= */

    @Operation(summary = "진동 on/off 변경", description = "진동 알림 사용 여부를 변경합니다.")
    @PatchMapping("/vibration")
    public ApiResponse<SettingResponse> updateVibration(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateVibrationRequest request
    ) {
        Long userId = principal.getUserId();
        Setting updated = settingService.updateVibration(userId, request.isEnabled());
        return ApiResponse.ok(SettingResponse.from(updated));
    }

    @Operation(summary = "음성 피드백 모드 변경", description = "음성 피드백 모드를 변경합니다. 예: off / male / female / tts_ko")
    @PatchMapping("/voice")
    public ApiResponse<SettingResponse> updateVoice(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateVoiceRequest request
    ) {
        Long userId = principal.getUserId();
        Setting updated = settingService.updateFeedbackVoice(userId, request.getVoiceMode());
        return ApiResponse.ok(SettingResponse.from(updated));
    }

    /* ========================= 일괄 업서트 ========================= */

    @Operation(summary = "설정 일괄 업서트", description = "존재하지 않으면 생성하고, 있으면 전달된 값만 갱신합니다.")
    @PutMapping
    public ApiResponse<SettingResponse> upsertAll(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpsertSettingRequest request
    ) {
        Long userId = principal.getUserId();
        Setting updated = settingService.upsertAll(userId, request.getVibrationEnabled(), request.getFeedbackVoice());
        return ApiResponse.ok(SettingResponse.from(updated));
    }
}
