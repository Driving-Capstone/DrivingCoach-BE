package com.drivingcoach.backend.domain.driving.domain.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoryListResponse {
    private HistorySummaryResponse summary; // 상단 요약
    private List<HistoryResponse> records;  // 리스트 데이터 (페이징된 결과)
}