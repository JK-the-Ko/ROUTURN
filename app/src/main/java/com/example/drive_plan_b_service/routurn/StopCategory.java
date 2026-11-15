package com.example.drive_plan_b_service.routurn;

/**
 * What the driver wants to stop for first (scenario 02). Each value filters candidates by
 * {@link CandidatePoi#category}. Usability of each place is unknown until a real source confirms it.
 */
public enum StopCategory {
    RESTROOM("화장실", "화장실을 먼저 들릅니다."),
    CAFE("카페", "커피나 휴식을 위해 먼저 들릅니다."),
    CONVENIENCE("편의점", "물이나 간단한 물건을 먼저 사러 갑니다."),
    CHARGER("충전소", "충전을 먼저 하고 식당으로 이동합니다.");

    public final String label;
    public final String intro;

    StopCategory(String label, String intro) {
        this.label = label;
        this.intro = intro;
    }
}
