package com.drivingcoach.backend.domain.user.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordCheckRequest {
    @NotBlank
    @Schema(description = "확인할 비밀번호", example = "1234")
    private String password;
}