package com.example.drive_plan_b_service.routurn;

import androidx.annotation.Nullable;

/**
 * A candidate place shown to the driver. "availableSpaces" is null unless a real data source
 * confirmed it; it must never be invented. "hasParking" means a parking facility exists and says
 * nothing about free spaces.
 */
public final class CandidatePoi {
    public final String id;
    public final String name;
    /** One of the {@link RouturnIntent#category} values: 주차장, 파스타, 화장실. */
    public final String category;
    public final String address;
    public final double latitude;
    public final double longitude;
    public final boolean paid;
    public final boolean hasParking;
    /** Driving time from the car to this place, in minutes. Null when no route estimate exists. */
    @Nullable public final Integer driveMinutes;
    /** Walking time to the original visit (restaurant), in minutes. */
    @Nullable public final Integer walkMinutes;
    /** Confirmed remaining spaces. Null means "미확인" (unconfirmed). */
    @Nullable public final Integer availableSpaces;
    /** Time the space count was last updated, or null. */
    @Nullable public final String spacesUpdatedAt;
    /** True when the values come from the demo dataset and must be labeled as such. */
    public final boolean demoData;

    public CandidatePoi(String id, String name, String category, String address,
                        double latitude, double longitude, boolean paid, boolean hasParking,
                        @Nullable Integer driveMinutes, @Nullable Integer walkMinutes,
                        @Nullable Integer availableSpaces, @Nullable String spacesUpdatedAt,
                        boolean demoData) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.paid = paid;
        this.hasParking = hasParking;
        this.driveMinutes = driveMinutes;
        this.walkMinutes = walkMinutes;
        this.availableSpaces = availableSpaces;
        this.spacesUpdatedAt = spacesUpdatedAt;
        this.demoData = demoData;
    }

    public boolean hasConfirmedSpaces() {
        return availableSpaces != null;
    }
}
