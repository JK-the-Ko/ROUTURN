package com.example.drive_plan_b_service.routurn;

/** Screen states from the Route:Re handoff (SCR-00 to SCR-05). */
public enum RouturnState {
    IDLE,
    SEARCHING,
    RESULTS,
    CONFIRM,
    ROUTING,
    /** The route was requested but guidance has not started yet (preview in the navigation app). */
    WAITING_START,
    SUCCESS,
    ERROR
}
