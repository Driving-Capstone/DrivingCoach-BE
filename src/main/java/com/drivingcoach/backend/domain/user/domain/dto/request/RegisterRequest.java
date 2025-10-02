package com.drivingcoach.backend.domain.user.domain.dto.request;

import com.drivingcoach.backend.domain.user.domain.constant.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 회원가입 요청 DTO
 * - API 명세: nickname, gender, birthday(=birthDate), loginId, password, (optional) email
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @Schema(description = "닉네임(2~20자)", example = "도로위의승원")
    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    private String nickname;

    @Schema(description = "성별(MALE/FEMALE/UNKNOWN)", example = "MALE")
    @NotNull(message = "성별을 선택해 주세요.")
    private Gender gender;

    @Schema(description = "생년월일 (yyyy-MM-dd)", example = "2001-01-01")
    @NotNull(message = "생년월일을 입력해 주세요.")
    @PastOrPresent(message = "생년월일은 미래일 수 없습니다.")
    private LocalDate birthDate; // (명세의 birthday에 해당)

    @Schema(description = "로그인 ID(영문/숫자 4~20자)", example = "drive_king01")
    @NotBlank(message = "로그인 ID를 입력해 주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]{4,20}$", message = "로그인 ID는 영문/숫자/[_-] 4~20자여야 합니다.")
    private String loginId;

    @Schema(description = "비밀번호(8~64자)", example = "Password!234")
    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
    private String password;

    @Schema(description = "이메일(선택)", example = "user@example.com")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
}
