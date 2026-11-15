package com.example.drive_plan_b_service.routurn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class RouturnSessionTest {

    private static CandidatePoi firstCandidate(RouturnIntent intent) {
        return CandidateRanker.rank(intent, SampleData.allCandidates()).get(0);
    }

    @Test
    public void happyPathReachesSuccessOnlyAfterCompletion() {
        RouturnSession s = new RouturnSession();
        CandidatePoi poi = firstCandidate(RouturnIntent.PARKING_KEEP_RESTAURANT);

        s.startSearch(RouturnIntent.PARKING_KEEP_RESTAURANT);
        assertEquals(RouturnState.SEARCHING, s.getState());
        s.showResults();
        s.selectCandidate(poi);
        assertEquals(RouturnState.CONFIRM, s.getState());
        s.confirm();
        assertEquals(RouturnState.ROUTING, s.getState()); // no optimistic success while routing
        s.onRouteSucceeded();
        assertEquals(RouturnState.SUCCESS, s.getState());
        assertEquals(poi, s.getSelected());
    }

    @Test
    public void intentIsKeptForTheWholeAttempt() {
        RouturnSession s = new RouturnSession();
        s.startSearch(RouturnIntent.RESTROOM_FIRST);
        s.showResults();
        s.selectCandidate(firstCandidate(RouturnIntent.RESTROOM_FIRST));
        s.confirm();
        assertEquals(RouturnIntent.RESTROOM_FIRST, s.getIntent());
    }

    @Test
    public void cancelReturnsToResultsWithoutSelection() {
        RouturnSession s = new RouturnSession();
        s.startSearch(RouturnIntent.PASTA_CHANGE_RESTAURANT);
        s.showResults();
        s.selectCandidate(firstCandidate(RouturnIntent.PASTA_CHANGE_RESTAURANT));
        s.cancelConfirm();
        assertEquals(RouturnState.RESULTS, s.getState());
        assertNull(s.getSelected());
    }

    @Test
    public void routeFailureKeepsMessageAndAllowsReset() {
        RouturnSession s = new RouturnSession();
        s.startSearch(RouturnIntent.PARKING_KEEP_RESTAURANT);
        s.showResults();
        s.selectCandidate(firstCandidate(RouturnIntent.PARKING_KEEP_RESTAURANT));
        s.confirm();
        s.onRouteFailed("응답 시간 초과");
        assertEquals(RouturnState.ERROR, s.getState());
        assertEquals("응답 시간 초과", s.getErrorMessage());
        s.reset();
        assertEquals(RouturnState.IDLE, s.getState());
        assertNull(s.getErrorMessage());
        assertNull(s.getIntent());
    }

    @Test
    public void previewWaitingCanStartLaterOrBeCancelled() {
        RouturnSession s = new RouturnSession();
        s.startSearch(RouturnIntent.PARKING_KEEP_RESTAURANT);
        s.showResults();
        s.selectCandidate(firstCandidate(RouturnIntent.PARKING_KEEP_RESTAURANT));
        s.confirm();
        s.onRouteWaiting();
        assertEquals(RouturnState.WAITING_START, s.getState());
        s.onRouteSucceeded(); // user pressed 안내 시작 in the navigation app
        assertEquals(RouturnState.SUCCESS, s.getState());
    }

    @Test
    public void previewCancelReturnsToIdle() {
        RouturnSession s = new RouturnSession();
        s.startSearch(RouturnIntent.RESTROOM_FIRST, "카페");
        s.showResults();
        s.selectCandidate(firstCandidate(RouturnIntent.RESTROOM_FIRST));
        s.confirm();
        s.onRouteWaiting();
        s.reset();
        assertEquals(RouturnState.IDLE, s.getState());
    }

    @Test
    public void errorWhileWaitingIsReported() {
        RouturnSession s = new RouturnSession();
        s.startSearch(RouturnIntent.PARKING_KEEP_RESTAURANT);
        s.showResults();
        s.selectCandidate(firstCandidate(RouturnIntent.PARKING_KEEP_RESTAURANT));
        s.confirm();
        s.onRouteWaiting();
        s.onRouteFailed("내비게이션 오류: CAUSE_SEARCH_ROUTE");
        assertEquals(RouturnState.ERROR, s.getState());
    }

    @Test
    public void cannotConfirmWithoutSelection() {
        RouturnSession s = new RouturnSession();
        assertThrows(IllegalStateException.class, s::confirm);
        s.startSearch(RouturnIntent.PARKING_KEEP_RESTAURANT);
        s.showResults();
        assertThrows(IllegalStateException.class, s::confirm);
    }

    @Test
    public void cannotSucceedWithoutRouting() {
        RouturnSession s = new RouturnSession();
        s.startSearch(RouturnIntent.RESTROOM_FIRST);
        assertThrows(IllegalStateException.class, s::onRouteSucceeded);
    }
}
