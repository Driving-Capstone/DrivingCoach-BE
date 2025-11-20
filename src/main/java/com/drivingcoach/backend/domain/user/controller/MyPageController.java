package com.drivingcoach.backend.domain.user.controller;

import com.drivingcoach.backend.domain.user.domain.CustomUserDetails;
import com.drivingcoach.backend.domain.user.domain.dto.request.ChangePasswordRequest;
import com.drivingcoach.backend.domain.user.domain.dto.request.PasswordCheckRequest;
import com.drivingcoach.backend.domain.user.domain.dto.request.UpdateUserProfileRequest;
import com.drivingcoach.backend.domain.user.domain.dto.response.MyPageInfoResponse;
import com.drivingcoach.backend.domain.user.domain.dto.response.PasswordCheckResponse;
import com.drivingcoach.backend.domain.user.domain.dto.response.UserProfileResponse;
import com.drivingcoach.backend.domain.user.service.UserService;
import com.drivingcoach.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MyPage", description = "마이페이지 API")
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;

    @Operation(summary = "마이페이지 정보 불러오기", description = "유저 정보와 주행 통계 데이터를 반환합니다.")
    @GetMapping("/load")
    public ApiResponse<MyPageInfoResponse> loadMyPage(@AuthenticationPrincipal CustomUserDetails principal) {
        MyPageInfoResponse response = userService.getMyPageInfo(principal.getUserId());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "비밀번호 확인", description = "정보 수정을 위해 현재 비밀번호가 맞는지 확인합니다.")
    @PostMapping("/passwordCheck")
    public ApiResponse<PasswordCheckResponse> checkPassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid PasswordCheckRequest request) {
        boolean isMatch = userService.checkPassword(principal.getUserId(), request.getPassword());
        return ApiResponse.ok(new PasswordCheckResponse(isMatch));
    }

    @Operation(summary = "비밀번호 변경", description = "새로운 비밀번호로 변경합니다.")
    @PostMapping("/passwordChange")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid ChangePasswordRequest request) {
        // 기존 UserService의 로직 재사용 (단, DTO 필드명 매핑 필요 시 수정)
        userService.changePassword(principal.getUserId(), request);
        return ApiResponse.ok();
    }

    @Operation(summary = "회원정보 수정", description = "닉네임, 성별, 생년월일 등을 수정합니다.")
    @PostMapping("/infoupdate")
    public ApiResponse<UserProfileResponse> updateInfo(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid UpdateUserProfileRequest request) {
        // 기존 UserService의 updateProfile 재사용
        UserProfileResponse response = userService.updateProfile(principal.getUserId(), request);
        return ApiResponse.ok(response);
    }
}