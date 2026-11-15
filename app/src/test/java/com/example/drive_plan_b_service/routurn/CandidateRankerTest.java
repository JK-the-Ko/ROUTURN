package com.example.drive_plan_b_service.routurn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class CandidateRankerTest {

    private static CandidatePoi poi(String id, String category, boolean hasParking,
                                    Integer drive, Integer walk, Integer spaces) {
        return new CandidatePoi(id, id, category, "", 0, 0, false, hasParking,
                drive, walk, spaces, spaces == null ? null : "now", false);
    }

    @Test
    public void parkingRanksConfirmedNearbyLotFirst() {
        List<CandidatePoi> ranked = CandidateRanker.rank(
                RouturnIntent.PARKING_KEEP_RESTAURANT, SampleData.allCandidates());
        assertEquals(2, ranked.size());
        // Paid lot: 5 + 2 + 0 = 7. Public lot: 3 + 4 + unconfirmed penalty 5 = 12.
        assertEquals("park-gyeonghuigung-paid", ranked.get(0).id);
        assertEquals("park-sajik-public", ranked.get(1).id);
    }

    @Test
    public void unconfirmedPublicLotHasNoSpaceCount() {
        CandidatePoi publicLot = SampleData.allCandidates().get(0);
        assertTrue(!publicLot.hasConfirmedSpaces());
        assertNull(publicLot.availableSpaces);
    }

    @Test
    public void pastaRequiresParkingAndRanksByDriveTime() {
        List<CandidatePoi> ranked = CandidateRanker.rank(
                RouturnIntent.PASTA_CHANGE_RESTAURANT, SampleData.allCandidates());
        // Pasta A has no parking, so it is excluded. B (6 min) comes before C (10 min).
        assertEquals(2, ranked.size());
        assertEquals("pasta-bomnal", ranked.get(0).id);
        assertEquals("pasta-luna", ranked.get(1).id);
    }

    @Test
    public void restroomRanksByArrivalTime() {
        List<CandidatePoi> ranked = CandidateRanker.rank(
                RouturnIntent.RESTROOM_FIRST, SampleData.allCandidates());
        assertEquals(2, ranked.size());
        assertEquals("restroom-gwanghwamun", ranked.get(0).id); // 4 min
        assertEquals("restroom-gyeongbokgung", ranked.get(1).id); // 9 min
    }

    @Test
    public void stopCategoryFiltersCandidatesForScenario02() {
        List<CandidatePoi> cafes = CandidateRanker.rank(
                RouturnIntent.RESTROOM_FIRST, StopCategory.CAFE.label, SampleData.allCandidates());
        assertEquals(2, cafes.size());
        assertEquals("cafe-seonggwak", cafes.get(0).id); // 3 min
        assertEquals("cafe-seochon", cafes.get(1).id);   // 7 min

        List<CandidatePoi> chargers = CandidateRanker.rank(
                RouturnIntent.RESTROOM_FIRST, StopCategory.CHARGER.label, SampleData.allCandidates());
        assertEquals("charger-gwanghwamun", chargers.get(0).id); // 6 min
    }

    @Test
    public void onlyCandidatesOfTheRequestedCategoryAreReturned() {
        List<CandidatePoi> all = new ArrayList<>();
        all.add(poi("p1", "주차장", true, 1, 1, null));
        all.add(poi("r1", "화장실", true, 1, null, null));
        List<CandidatePoi> ranked = CandidateRanker.rank(RouturnIntent.RESTROOM_FIRST, all);
        assertEquals(1, ranked.size());
        assertEquals("r1", ranked.get(0).id);
    }

    @Test
    public void limitsResultsToTwo() {
        List<CandidatePoi> all = new ArrayList<>();
        all.add(poi("a", "화장실", true, 1, null, null));
        all.add(poi("b", "화장실", true, 2, null, null));
        all.add(poi("c", "화장실", true, 3, null, null));
        List<CandidatePoi> ranked = CandidateRanker.rank(RouturnIntent.RESTROOM_FIRST, all);
        assertEquals(2, ranked.size());
        assertEquals("a", ranked.get(0).id);
        assertEquals("b", ranked.get(1).id);
    }
}
