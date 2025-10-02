package com.drivingcoach.backend.domain.user.domain.constant;

import lombok.Getter;

/**
 * 사용자 성별 Enum
 */
@Getter
public enum Gender {
    MALE("남성"),
    FEMALE("여성"),
    UNKNOWN("미응답");

    private final String description;

    Gender(String description) {
        this.description = description;
    }
}
