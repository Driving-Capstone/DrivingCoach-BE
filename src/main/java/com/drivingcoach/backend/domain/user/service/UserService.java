package com.drivingcoach.backend.domain.user.service;

import com.drivingcoach.backend.domain.user.domain.dto.request.ChangePasswordRequest;
import com.drivingcoach.backend.domain.user.domain.dto.request.UpdateUserProfileRequest;
import com.drivingcoach.backend.domain.user.domain.dto.response.UserProfileResponse;
import com.drivingcoach.backend.domain.user.domain.entity.User;
import com.drivingcoach.backend.domain.user.repository.UserRepository;
import com.drivingcoach.backend.global.exception.CustomException;
import com.drivingcoach.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile(Long userId) {
        User user = getActiveUserOrThrow(userId);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = getActiveUserOrThrow(userId);

        // (선택) 이메일 중복 체크: null 아니고 변경하려는 값이 기존과 다를 때만 검사
        if (request.getEmail() != null
                && !request.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        user.updateProfile(
                request.getNickname(),
                request.getGender(),
                request.getBirthDate(),
                request.getEmail()
        );

        // 변경감지로 flush, 혹은 명시적 save
        User saved = userRepository.save(user);
        return UserProfileResponse.from(saved);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getActiveUserOrThrow(userId);

        // 현재 비밀번호 일치 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 동일 비밀번호 방지 (선택)
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_CANNOT_BE_SAME);
        }

        String encoded = passwordEncoder.encode(request.getNewPassword());
        user.changePassword(encoded);
        userRepository.save(user);
    }

    @Transactional
    public void deactivate(Long userId) {
        User user = getActiveUserOrThrow(userId);
        user.deactivate();
        userRepository.save(user);
    }

    /* ====== Internal Helpers ====== */

    private User getActiveUserOrThrow(Long userId) {
        return userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
