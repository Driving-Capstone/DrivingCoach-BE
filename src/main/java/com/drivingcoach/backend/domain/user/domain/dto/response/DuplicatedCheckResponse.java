package com.drivingcoach.backend.domain.user.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 로그인ID 중복 체크 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuplicatedCheckResponse {

    @Schema(description = "중복 여부", example = "true")
    private boolean duplicated;
}
