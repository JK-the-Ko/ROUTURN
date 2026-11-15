package com.example.drive_plan_b_service.routurn;

/**
 * Explicit user intents from the Route:Re handoff. The app never infers these from
 * sensors; the driver picks one (button, or voice later).
 */
public enum RouturnIntent {
    /** 01-A: keep the restaurant, change the parking lot. */
    PARKING_KEEP_RESTAURANT("다른 주차장 찾아줘", "주차장", "01-A"),
    /** 01-B: keep the pasta meal, change the restaurant. Parking required. */
    PASTA_CHANGE_RESTAURANT("다른 파스타집 찾아줘", "파스타", "01-B"),
    /** 02: add a restroom as a waypoint, keep the restaurant as final destination. */
    RESTROOM_FIRST("먼저 들를 곳", StopCategory.RESTROOM.label, "02");

    public final String buttonLabel;
    /** Value of {@link CandidatePoi#category} used to filter candidates. */
    public final String category;
    public final String scenarioId;

    RouturnIntent(String buttonLabel, String category, String scenarioId) {
        this.buttonLabel = buttonLabel;
        this.category = category;
        this.scenarioId = scenarioId;
    }
}
