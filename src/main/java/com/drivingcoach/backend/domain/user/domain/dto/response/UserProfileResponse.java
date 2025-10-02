package com.drivingcoach.backend.domain.user.domain.dto.response;

import com.drivingcoach.backend.domain.user.domain.constant.Gender;
import com.drivingcoach.backend.domain.user.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 프로필 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "로그인 ID", example = "drive_king01")
    private String loginId;

    @Schema(description = "닉네임", example = "도로위의승원")
    private String nickname;

    @Schema(description = "성별(MALE/FEMALE/UNKNOWN)", example = "MALE")
    private Gender gender;

    @Schema(description = "생년월일", example = "2001-01-01")
    private LocalDate birthDate;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "활성화 여부", example = "true")
    private boolean active;

    @Schema(description = "생성일시", example = "2025-09-26T12:34:56")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-09-27T08:10:11")
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환 헬퍼
     */
    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .email(user.getEmail())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
