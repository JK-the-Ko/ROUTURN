package com.example.drive_plan_b_service.routurn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Per-scenario candidate selection and ranking, following the handoff's sort rules:
 * 01-A  walk access + drive time + parking data confidence
 * 01-B  pasta category + parking facility + drive time
 * 02    arrival time (drive time) for the restroom
 * The result is capped at 2 candidates for a short comparison.
 */
public final class CandidateRanker {
    /** Penalty (in score minutes) for an unconfirmed parking space count. */
    static final int UNCONFIRMED_SPACES_PENALTY = 5;
    static final int MAX_RESULTS = 2;
    private static final int UNKNOWN_MINUTES = Integer.MAX_VALUE / 4;

    private CandidateRanker() {
    }

    public static List<CandidatePoi> rank(RouturnIntent intent, List<CandidatePoi> all) {
        return rank(intent, intent.category, all);
    }

    /** {@code category} selects the candidate kind; scenario 02 passes the chosen stop category. */
    public static List<CandidatePoi> rank(RouturnIntent intent, String category,
                                          List<CandidatePoi> all) {
        List<CandidatePoi> filtered = new ArrayList<>();
        for (CandidatePoi poi : all) {
            if (!poi.category.equals(category)) {
                continue;
            }
            // 01-B requires a parking facility; the facility must exist, free spaces are not implied.
            if (intent == RouturnIntent.PASTA_CHANGE_RESTAURANT && !poi.hasParking) {
                continue;
            }
            filtered.add(poi);
        }
        Collections.sort(filtered, Comparator.comparingInt(poi -> score(intent, poi)));
        return new ArrayList<>(filtered.subList(0, Math.min(MAX_RESULTS, filtered.size())));
    }

    static int score(RouturnIntent intent, CandidatePoi poi) {
        int drive = poi.driveMinutes != null ? poi.driveMinutes : UNKNOWN_MINUTES;
        switch (intent) {
            case PARKING_KEEP_RESTAURANT: {
                int walk = poi.walkMinutes != null ? poi.walkMinutes : UNKNOWN_MINUTES;
                int penalty = poi.hasConfirmedSpaces() ? 0 : UNCONFIRMED_SPACES_PENALTY;
                return drive + walk + penalty;
            }
            case PASTA_CHANGE_RESTAURANT:
                return drive;
            case RESTROOM_FIRST:
                return drive;
            default:
                throw new IllegalArgumentException("Unhandled intent " + intent);
        }
    }
}
