// domain/user/domain/dto/response/MyPageInfoResponse.java
package com.drivingcoach.backend.domain.user.domain.dto.response;

import com.drivingcoach.backend.domain.user.domain.constant.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyPageInfoResponse {

    @Schema(description = "총 주행 횟수", example = "127")
    private Long allDriving;

    @Schema(description = "총 주행 시간(시간 단위)", example = "45.3")
    private Double allTime;

    @Schema(description = "안전 점수 평균", example = "85")
    private Float safeScore;

    @Schema(description = "성별", example = "MALE")
    private Gender gender;

    @Schema(description = "생년월일", example = "2001-01-01")
    private LocalDate birthDate; // 명세서의 birthday를 기존 코드의 birthDate로 매핑

    @Schema(description = "가입일", example = "2020-02-20")
    private LocalDate joinDay;

    @Schema(description = "총 발생 이벤트 수", example = "15")
    private Long totalEvents; // (추가!)
}