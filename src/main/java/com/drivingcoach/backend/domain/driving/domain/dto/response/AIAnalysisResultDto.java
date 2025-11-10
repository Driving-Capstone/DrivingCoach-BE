package com.drivingcoach.backend.domain.driving.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * AI 서버의 AsyncCallbackData Pydantic 모델에 대응하는 Java DTO
 */
@Data
public class AIAnalysisResultDto {

    @JsonProperty("s3_file_key")
    private String s3FileKey;

    @JsonProperty("results_per_frame")
    private List<FrameDetectionOutputDto> resultsPerFrame;

    private String status;

    @Data
    public static class FrameDetectionOutputDto {
        @JsonProperty("frame_index")
        private int frameIndex;
        private List<DetectionBoxDto> detections;
    }

    @Data
    public static class DetectionBoxDto {
        @JsonProperty("class_name")
        private String className;
        private float confidence;

        @JsonProperty("box_xyxy")
        private List<Float> boxXyxy;
    }
}