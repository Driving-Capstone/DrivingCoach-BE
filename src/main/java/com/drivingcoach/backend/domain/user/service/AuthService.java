package com.drivingcoach.backend.domain.user.service;

import com.drivingcoach.backend.domain.user.domain.CustomUserDetails;
import com.drivingcoach.backend.domain.user.domain.dto.request.LoginRequest;
import com.drivingcoach.backend.domain.user.domain.dto.request.RegisterRequest;
import com.drivingcoach.backend.domain.user.domain.dto.response.LoginResponse;
import com.drivingcoach.backend.domain.user.domain.entity.User;
import com.drivingcoach.backend.domain.user.repository.UserRepository;
import com.drivingcoach.backend.global.exception.CustomException;
import com.drivingcoach.backend.global.exception.ErrorCode;
import com.drivingcoach.backend.global.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;


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
        if (req.getLoginId() != null && userRepository.existsByLoginId(req.getLoginId())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .loginId(req.getLoginId())
                .password(passwordEncoder.encode(req.getPassword()))
                .nickname(req.getNickname())
                .gender(req.getGender())
                .birthDate(req.getBirthDate())
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
        // 인증 시도
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getLoginId(), req.getPassword())
        );

        // 인증 성공 시 사용자 정보 가져오기
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String accessToken = jwtUtil.createAccessToken(userDetails);
        String refreshToken = jwtUtil.createRefreshToken(userDetails);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }


}
