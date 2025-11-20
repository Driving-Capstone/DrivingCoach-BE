package com.drivingcoach.backend.domain.user.controller;

import com.drivingcoach.backend.domain.user.domain.CustomUserDetails;
import com.drivingcoach.backend.domain.user.domain.dto.request.DuplicatedCheckRequest;
import com.drivingcoach.backend.domain.user.domain.dto.request.LoginRequest;
import com.drivingcoach.backend.domain.user.domain.dto.request.RefreshTokenRequest;
import com.drivingcoach.backend.domain.user.domain.dto.request.RegisterRequest;
import com.drivingcoach.backend.domain.user.domain.dto.response.AccessTokenResponse;
import com.drivingcoach.backend.domain.user.domain.dto.response.DuplicatedCheckResponse;
import com.drivingcoach.backend.domain.user.domain.dto.response.LoginResponse;
import com.drivingcoach.backend.domain.user.domain.dto.response.RegisterResponse;
import com.drivingcoach.backend.domain.user.service.AuthService;
import com.drivingcoach.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "회원가입/로그인/토큰 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "아이디 중복 체크", description = "loginId 중복 여부를 확인합니다.")
    @PostMapping("/duplicated")
    public ApiResponse<DuplicatedCheckResponse> checkDuplicated(
            @Valid @RequestBody DuplicatedCheckRequest request
    ) {
        boolean duplicated = authService.isDuplicatedLoginId(request.getLoginId());
        return ApiResponse.ok(new DuplicatedCheckResponse(duplicated));
    }

    @Operation(summary = "회원가입", description = "닉네임/성별/생년월일/로그인ID/비밀번호로 회원을 생성합니다.")
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        String loginId = authService.register(request);
        return ApiResponse.ok(new RegisterResponse(loginId));
    }

    @Operation(summary = "로그인", description = "loginId/password로 로그인하여 액세스 토큰을 발급합니다.")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse token = authService.login(request);
        return ApiResponse.ok(token);
    }

    @Operation(summary = "토큰 리프레시", description = "리프레시 토큰을 이용해 액세스 토큰을 재발급합니다.")
    @PostMapping("/reissue")
    public ApiResponse<AccessTokenResponse> reissue(@RequestBody RefreshTokenRequest request) {
        // TODO: 실제 리프레시 토큰 검증 로직 (DB/Redis 확인 등) 필요
        // 현재는 구조만 잡아드립니다.
        String newAccessToken = authService.reissueAccessToken(request.getRefreshToken());
        return ApiResponse.ok(new AccessTokenResponse(newAccessToken, "Bearer"));
    }

    @Operation(summary = "로그아웃", description = "로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails principal) {
        // JWT는 stateless하므로 서버측에서 할 일은 보통 Refresh Token 삭제 또는 Blacklist 등록입니다.
        // 여기서는 성공 응답만 반환합니다.
        return ApiResponse.ok();
    }

    @Operation(summary = "계정 삭제(탈퇴)", description = "회원 탈퇴를 진행합니다.")
    @PostMapping("/delete") // 명세서엔 logout으로 되어있으나 구분 필요
    public ApiResponse<Void> deleteAccount(@AuthenticationPrincipal CustomUserDetails principal) {
        authService.deleteAccount(principal.getUserId());
        return ApiResponse.ok();
    }

    // reissue
}
