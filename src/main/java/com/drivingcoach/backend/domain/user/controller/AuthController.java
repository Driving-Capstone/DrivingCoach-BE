package com.drivingcoach.backend.domain.user.controller;

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

    // reissue
}
