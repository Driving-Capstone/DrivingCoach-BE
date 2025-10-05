package com.drivingcoach.backend.global.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {
    private int code;

    private String error;

    private String message;
}

