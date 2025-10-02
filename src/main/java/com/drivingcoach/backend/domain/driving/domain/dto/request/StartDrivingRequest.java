package com.drivingcoach.backend.domain.driving.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ✅ StartDrivingRequest
 *
 * 용도
 *  - "주행 시작" API 요청 바디에 사용됩니다. (POST /api/driving/start)
 *
 * 필드 설명
 *  - startTime        : (선택) 주행 시작 시각. null 이면 서버에서 현재시각(LocalDateTime.now())로 대체합니다.
 *                       - 클라이언트/서버 시간 차이가 큰 환경이라면, 서버 시간을 신뢰하도록 null 전송을 권장합니다.
 *  - videoKeyOrUrl    : (선택) 시작 시점에 이미 확보한 S3 키나 URL이 있다면 전달합니다.
 *                       - 앱에서 웹소켓/스트리밍으로 청크 업로드를 병행한다면, 여기서는 null 로 두고
 *                         종료 시점(EndDrivingRequest)에서 최종 키를 업데이트하는 흐름도 가능합니다.
 *
 * 검증 정책
 *  - 모든 값이 선택 입력(Optional)입니다. (서버에서 보수적으로 처리)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartDrivingRequest {

    @Schema(description = "주행 시작 시각(옵션). 미전달 시 서버 now 적용", example = "2025-09-28T10:11:12")
    private LocalDateTime startTime;

    @Schema(description = "영상/zip의 S3 키 또는 URL(옵션)", example = "driving/abc-uuid/video.mp4")
    @Size(max = 512, message = "videoKeyOrUrl 은 512자를 넘을 수 없습니다.")
    private String videoKeyOrUrl;
}
