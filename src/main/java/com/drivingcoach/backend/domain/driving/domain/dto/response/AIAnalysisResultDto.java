package com.drivingcoach.backend.domain.driving.domain.dto.response;

// import com.fasterxml.jackson.annotation.JsonProperty; // (제거)
import lombok.Data;
import java.util.List;

@Data
public class AIAnalysisResultDto {

    // @JsonProperty("s3_file_key") // (제거!)
    private String s3FileKey; // AI가 camelCase(s3FileKey)로 보냄

    // @JsonProperty("results_per_frame") // (제거!)
    private List<FrameDetectionOutputDto> resultsPerFrame; // AI가 camelCase로 보냄

    private String status;
    private int chunkIndex;

    @Data
    public static class FrameDetectionOutputDto {
        // @JsonProperty("frame_index") // (제거!)
        private int frameIndex; // AI가 camelCase로 보냄
        private List<DetectionBoxDto> detections;
    }

    @Data
    public static class DetectionBoxDto {
        // @JsonProperty("class_name") // (제거!)
        private String className; // AI가 camelCase로 보냄
        private float confidence;

        // @JsonProperty("box_xyxy") // (제거!)
        private List<Float> boxXyxy; // AI가 camelCase로 보냄
    }
}