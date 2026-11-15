package com.example.drive_plan_b_service.routurn;

import java.util.Arrays;
import java.util.List;

/**
 * Candidate data for the current demo build. Names read like real places. The screen carries one
 * footer note that the data is for demonstration; no per-item "sample" wording is shown.
 * Real parking, restaurant, and stop sources are still undecided (handoff open items).
 */
public final class SampleData {
    /** Original visit kept separately from the navigation destination (spec rule). */
    public static final CandidatePoi ORIGINAL_RESTAURANT = new CandidatePoi(
            "restaurant-original", "라 피아짜 식당", "식당", "서울 종로구 사직동",
            37.5729, 126.9710, false, false, null, null, null, null, true);

    private SampleData() {
    }

    /** All candidates for every scenario. Filter by intent category or stop category. */
    public static List<CandidatePoi> allCandidates() {
        return Arrays.asList(
                // 01-A: parking near the restaurant.
                new CandidatePoi("park-sajik-public", "사직로 노상 공영주차장", "주차장",
                        "서울 종로구 사직로", 37.5731, 126.9722, false, true,
                        3, 4, null, null, true),
                new CandidatePoi("park-gyeonghuigung-paid", "경희궁 앞 유료주차장", "주차장",
                        "서울 종로구 신무로", 37.5719, 126.9715, true, true,
                        5, 2, 3, "1분 전 확인", true),

                // 01-B: pasta places. Parking is required, so a place without parking is filtered out.
                new CandidatePoi("pasta-mare", "마레 파스타 바", "파스타",
                        "서울 종로구 자하문로", 37.5690, 126.9800, false, false,
                        4, null, null, null, true),
                new CandidatePoi("pasta-bomnal", "봄날 파스타 하우스", "파스타",
                        "서울 종로구 효자로", 37.5680, 126.9810, false, true,
                        6, null, null, null, true),
                new CandidatePoi("pasta-luna", "루나 파스타 키친", "파스타",
                        "서울 종로구 통인동", 37.5700, 126.9820, true, true,
                        10, null, null, null, true),

                // 02: stops along the way, by category. Usability is unknown until a real source confirms it.
                new CandidatePoi("restroom-gwanghwamun", "광화문광장 공중화장실", "화장실",
                        "서울 종로구 세종로", 37.5726, 126.9782, false, true,
                        4, null, null, null, true),
                new CandidatePoi("restroom-gyeongbokgung", "경복궁역 공중화장실", "화장실",
                        "서울 종로구 사직동", 37.5655, 126.9770, false, false,
                        9, null, null, null, true),
                new CandidatePoi("cafe-seonggwak", "성곽길 카페 라운지", "카페",
                        "서울 종로구 창의문로", 37.5745, 126.9735, false, true,
                        3, null, null, null, true),
                new CandidatePoi("cafe-seochon", "서촌 브루잉 카페", "카페",
                        "서울 종로구 자하문로", 37.5762, 126.9722, false, false,
                        7, null, null, null, true),
                new CandidatePoi("store-sajik-24", "사직동 24시 편의점", "편의점",
                        "서울 종로구 사직동", 37.5712, 126.9695, false, false,
                        2, null, null, null, true),
                new CandidatePoi("store-jongno-plus", "종로 편의점 플러스", "편의점",
                        "서울 종로구 종로", 37.5704, 126.9762, false, true,
                        5, null, null, null, true),
                new CandidatePoi("charger-gwanghwamun", "광화문 전기차 충전소", "충전소",
                        "서울 종로구 세종대로", 37.5719, 126.9768, false, true,
                        6, null, null, null, true),
                new CandidatePoi("charger-sejongno-fast", "세종대로 급속충전소", "충전소",
                        "서울 종로구 세종대로", 37.5701, 126.9790, false, true,
                        11, null, null, null, true));
    }
}
