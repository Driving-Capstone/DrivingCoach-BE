package com.drivingcoach.backend.domain.driving.domain.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AIAnalysisResultDto {

    private String s3FileKey;
    private String status;
    private int chunkIndex;

    // (추가!) AI가 탐지한 이벤트 ID 리스트 (예: [1, 11])
    // 해당 청크(2초) 내에서 발생한 이벤트 번호들
    private List<Integer> detectedEventIds;

    private List<FrameDetectionOutputDto> resultsPerFrame;

    @Data
    public static class FrameDetectionOutputDto {
        private int frameIndex;
        private List<DetectionBoxDto> detections;
    }

    @Data
    public static class DetectionBoxDto {
        private String className;
        private float confidence;
        private List<Float> boxXyxy;
    }
}