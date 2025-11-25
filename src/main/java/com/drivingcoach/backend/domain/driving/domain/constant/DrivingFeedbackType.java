package com.drivingcoach.backend.domain.driving.domain.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum DrivingFeedbackType {
    SIGNAL_VIOLATION(1, "신호 위반 감지", "신호 위반입니다."),
    TURN_SIGNAL_MISSING_TURN(2, "회전 시 방향지시등 미점등", "좌회전 시 방향지시등을 켜주세요."), // 상황에 따라 좌/우 텍스트 변경 가능 로직 필요 시 확장
    SAFE_TURN_SUCCESS(3, "올바른 회전 탐지", "좋아요! 안전하게 회전하셨습니다."),
    LANE_CHANGE_MISSING_ACTION(4, "방향지시등 후 차선변경 미수행", "방향지시등이 켜져 있습니다. 차선 변경을 완료하거나 방향지시등을 끄세요."),
    LANE_CHANGE_MISSING_SIGNAL(5, "차선 변경 시 방향지시등 미점등", "차선 변경 시 방향지시등을 켜야합니다."),
    LANE_CHANGE_CORRECT_SIGNAL(6, "올바른 방향지시등 점등", "좋아요! 안전하게 차선 변경하셨습니다."), // 5번과 중복되는 피드백 텍스트 수정 제안 (긍정 피드백)
    EMERGENCY_LIGHT_SUCCESS(7, "악천후 시 비상등 사용", "악천후 시에는 비상등보다 전조등을 조절해주세요."),
    SPECIAL_CONDITION_GUIDE(8, "특수 상황 안내", "보행자가 없습니다. 천천히 우회전하실 수 있습니다."),
    THREATENING_DRIVING(9, "보행자 위협 운전", "보행자에게 경적을 울리는 것은 위협 운전입니다."),
    RECKLESS_DRIVING(10, "난폭 운전 경고", "차분하게 운전해 주세요. 난폭 운전은 사고로 이어질 수 있습니다."),
    TAILGATING_WARNING(11, "보복운전(꼬리물기) 경고", "앞차와의 거리가 너무 가깝습니다. 안전거리를 확보하세요.");

    private final int id;
    private final String eventName;
    private final String feedbackMessage;

    public static DrivingFeedbackType fromId(int id) {
        return Arrays.stream(values())
                .filter(type -> type.id == id)
                .findFirst()
                .orElse(null);
    }
}