package com.drivingcoach.backend.domain.driving.domain.dto.request;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryDetailRequest {
    private Long drivingId;
}