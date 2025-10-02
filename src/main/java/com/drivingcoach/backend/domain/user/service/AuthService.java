package com.drivingcoach.backend.domain.user.service;

import com.drivingcoach.backend.domain.user.domain.dto.request.LoginRequest;
import com.drivingcoach.backend.domain.user.domain.dto.request.RegisterRequest;
import com.drivingcoach.backend.domain.user.domain.dto.response.LoginResponse;
import com.drivingcoach.backend.domain.user.domain.entity.User;
import com.drivingcoach.backend.domain.user.repository.UserRepository;
import com.drivingcoach.backend.global.exception.CustomException;
import com.drivingcoach.backend.global.exception.ErrorCode;
import com.drivingcoach.backend.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** loginId 중복 여부 */
    public boolean isDuplicatedLoginId(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    /** 회원가입 */
    @Transactional
    public String register(RegisterRequest req) {
        if (userRepository.existsByLoginId(req.getLoginId())) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (req.getEmail() != null && userRepository.existsByEmail(req.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .loginId(req.getLoginId())
                .password(passwordEncoder.encode(req.getPassword()))
                .nickname(req.getNickname())
                .gender(req.getGender())
                .birthDate(req.getBirthDate())
                .email(req.getEmail())
                .role("ROLE_USER")
                .active(true)
                .build();

        userRepository.save(user);
        return user.getLoginId();
    }

    /** 로그인 → 액세스/리프레시 토큰 발급 */
    @Transactional
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByLoginId(req.getLoginId())
                .filter(User::isActive)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getLoginId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getLoginId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    /** 리프레시 토큰 검증 → 새 액세스 토큰 발급 */
    public String refreshAccessToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String loginId = jwtUtil.getLoginIdFromToken(refreshToken);
        User user = userRepository.findByLoginId(loginId)
                .filter(User::isActive)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return jwtUtil.generateAccessToken(user.getId(), user.getLoginId(), user.getRole());
    }
}
