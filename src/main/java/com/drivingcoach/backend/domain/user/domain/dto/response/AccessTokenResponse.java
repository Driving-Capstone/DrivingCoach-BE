package com.drivingcoach.backend.domain.user.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 리프레시로 새 액세스 토큰 발급 시 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessTokenResponse {

    @Schema(description = "새로 발급된 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
    private String accessToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";
}
