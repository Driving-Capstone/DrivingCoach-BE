package com.drivingcoach.backend.domain.user.domain.dto.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordCheckResponse {
    private boolean check;
}