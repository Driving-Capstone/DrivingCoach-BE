package com.drivingcoach.backend.domain.user.controller;

import com.drivingcoach.backend.domain.user.domain.CustomUserDetails;
import com.drivingcoach.backend.domain.user.domain.dto.request.ChangePasswordRequest;
import com.drivingcoach.backend.domain.user.domain.dto.request.UpdateUserProfileRequest;
import com.drivingcoach.backend.domain.user.domain.dto.response.UserProfileResponse;
import com.drivingcoach.backend.domain.user.service.UserService;
import com.drivingcoach.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 프로필/계정 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUserId();
        UserProfileResponse profile = userService.getProfile(userId);
        return ApiResponse.ok(profile);
    }

    @Operation(summary = "내 프로필 수정", description = "닉네임/성별/생년월일/이메일 등 프로필을 수정합니다.")
    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        Long userId = principal.getUserId();
        UserProfileResponse updated = userService.updateProfile(userId, request);
        return ApiResponse.ok(updated);
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 검증 후 새 비밀번호로 변경합니다.")
    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Long userId = principal.getUserId();
        userService.changePassword(userId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "회원 탈퇴(비활성화)", description = "계정을 비활성화합니다. 복구 정책은 기획에 따릅니다.")
    @DeleteMapping("/me")
    public ApiResponse<Void> deactivateMyAccount(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUserId();
        userService.deactivate(userId);
        return ApiResponse.ok();
    }
}
