package com.drivingcoach.backend.domain.driving.domain.dto.request;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryDateRequest {
    private Integer year;
    private Integer month;
    private Integer date;
}
