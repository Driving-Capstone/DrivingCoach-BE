package com.drivingcoach.backend.domain.user.domain.dto.request;

import com.drivingcoach.backend.domain.user.domain.constant.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * 프로필 수정 요청 DTO
 * - 모든 필드는 선택(Optional) 입력으로 간주합니다.
 * - null 로 전달된 필드는 변경하지 않습니다(서비스 레이어에서 patch 처리).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserProfileRequest {

    @Schema(description = "닉네임 (2~20자)", example = "도로위의승원")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    private String nickname;

    @Schema(description = "성별(MALE/FEMALE/UNKNOWN)", example = "MALE")
    private Gender gender; // enum은 user.domain.constant.Gender 로 별도 정의 예정

    @Schema(description = "생년월일 (yyyy-MM-dd)", example = "2001-01-01")
    @PastOrPresent(message = "생년월일은 미래일 수 없습니다.")
    private LocalDate birthDate;

    @Schema(description = "이메일", example = "user@example.com")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
}
