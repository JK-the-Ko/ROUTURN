package com.example.drive_plan_b_service.routurn;

import androidx.annotation.Nullable;

/**
 * Pure state machine for the plan-B flow. SUCCESS can only be reached through
 * {@link #onRouteSucceeded()}, which the navigation completion callback drives.
 */
public final class RouturnSession {
    private RouturnState state = RouturnState.IDLE;
    @Nullable private RouturnIntent intent;
    @Nullable private String category;
    @Nullable private CandidatePoi selected;
    @Nullable private String errorMessage;

    public RouturnState getState() {
        return state;
    }

    @Nullable public RouturnIntent getIntent() {
        return intent;
    }

    /** Candidate category for this attempt (e.g. 화장실 or 카페 for scenario 02). */
    @Nullable public String getCategory() {
        return category;
    }

    @Nullable public CandidatePoi getSelected() {
        return selected;
    }

    @Nullable public String getErrorMessage() {
        return errorMessage;
    }

    /** Back to the intent picker. Allowed after a finished attempt or from the picker itself. */
    public void reset() {
        require(RouturnState.IDLE, RouturnState.RESULTS, RouturnState.ERROR, RouturnState.SUCCESS,
                RouturnState.WAITING_START);
        intent = null;
        category = null;
        selected = null;
        errorMessage = null;
        state = RouturnState.IDLE;
    }

    public void startSearch(RouturnIntent newIntent) {
        startSearch(newIntent, newIntent.category);
    }

    public void startSearch(RouturnIntent newIntent, String newCategory) {
        require(RouturnState.IDLE, RouturnState.RESULTS, RouturnState.ERROR, RouturnState.SUCCESS);
        intent = newIntent;
        category = newCategory;
        selected = null;
        errorMessage = null;
        state = RouturnState.SEARCHING;
    }

    public void showResults() {
        require(RouturnState.SEARCHING);
        state = RouturnState.RESULTS;
    }

    public void selectCandidate(CandidatePoi poi) {
        require(RouturnState.RESULTS);
        selected = poi;
        state = RouturnState.CONFIRM;
    }

    public void cancelConfirm() {
        require(RouturnState.CONFIRM);
        selected = null;
        state = RouturnState.RESULTS;
    }

    public void confirm() {
        require(RouturnState.CONFIRM);
        state = RouturnState.ROUTING;
    }

    /** The navigation app did not start guidance yet; the route may still be previewed there. */
    public void onRouteWaiting() {
        require(RouturnState.ROUTING);
        state = RouturnState.WAITING_START;
    }

    public void onRouteSucceeded() {
        require(RouturnState.ROUTING, RouturnState.WAITING_START);
        state = RouturnState.SUCCESS;
    }

    public void onRouteFailed(String message) {
        require(RouturnState.ROUTING, RouturnState.WAITING_START);
        errorMessage = message;
        state = RouturnState.ERROR;
    }

    private void require(RouturnState... allowed) {
        for (RouturnState s : allowed) {
            if (state == s) {
                return;
            }
        }
        throw new IllegalStateException("Invalid transition from " + state);
    }
}
