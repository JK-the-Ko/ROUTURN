package com.example.drive_plan_b_service.routurn;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.function.Consumer;

import ai.pleos.playground.navi.constants.NaviErrorCode;
import ai.pleos.playground.navi.constants.RouteOption;
import ai.pleos.playground.navi.constants.WaypointIndex;
import ai.pleos.playground.navi.data.DestinationArrivedInfo;
import ai.pleos.playground.navi.data.DrivingInfo;
import ai.pleos.playground.navi.data.DrivingState;
import ai.pleos.playground.navi.data.RequestWaypointInfo;
import ai.pleos.playground.navi.data.WaypointArrivedInfo;
import ai.pleos.playground.navi.data.RouteInfo;
import ai.pleos.playground.navi.data.RouteStartInfo;
import ai.pleos.playground.navi.data.RouteWithWaypointsRequest;
import ai.pleos.playground.navi.helper.NaviHelper;
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener;
import kotlin.Unit;

/**
 * Thin wrapper around the NaviHelper SDK (3.0.5) for destination and waypoint changes.
 * Success is reported only from a route-level callback, never optimistically.
 * Lifecycle follows the Pleos docs order: initialize, addListener, request, release.
 */
public final class NavigationGateway {

    public interface Callback {
        void onRouteCompleted();

        void onRouteFailed(String reason);

        /**
         * No route-start callback yet after the timeout. The route may be waiting in the
         * navigation app's preview; the request stays open until start, error, or cancel.
         */
        void onRouteWaiting();
    }

    /** Live guidance events from the navigation app, delivered on the main thread. */
    public interface LiveListener {
        /** Remaining time (seconds) and distance (meters) to the final destination. */
        void onProgress(int remainingSeconds, int remainingMeters);

        void onDestinationArrived(String name);

        void onWaypointArrived(String name);

        /** The route was cancelled by the navigation app or the user. */
        void onCancelled();
    }

    @Nullable private LiveListener live;

    public void setLiveListener(@Nullable LiveListener listener) {
        this.live = listener;
    }

    /** Drops the pending request without reporting it (used after the user cancels the route). */
    public void clearPending() {
        pending = null;
        main.removeCallbacks(timeoutTask);
    }

    /** Cancels the active route through the SDK. The result arrives as onRouteCancelled on the listener. */
    public void cancelRoute() {
        if (initialized) {
            navi.cancelRoute();
        }
    }

    /** Asks the navigation app for the current destination state; the answer arrives via onDestinationInfo. */
    public void refreshStatus() {
        if (initialized) {
            navi.getDestinationInfo();
        }
    }

    /** Upper bound for waiting on a navigation callback before reporting failure. */
    static final long ROUTE_TIMEOUT_MS = 30_000L;

    private final NaviHelper navi;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final NaviHelperEventListener listener = new NaviHelperEventListener() {
        @Override
        public void onRoutingCompleted() {
            finish(true, null);
        }

        /**
         * On the emulator (NaviHelper 3.0.5, Pleos nav app) the route-set signal arrived as
         * onRouteStarted after the route was computed; onRoutingCompleted was not delivered.
         * Treated as completion here; confirm against official docs.
         */
        @Override
        public void onRouteStarted(RouteStartInfo info) {
            finish(true, null);
        }

        @Override
        public void onError(NaviErrorCode code) {
            finish(false, "내비게이션 오류: " + code);
        }

        // Live events. onRouteEnded is not used: the SDK sends it right after onRouteStarted,
        // so it cannot be read as the end of guidance. onRouteCancelled is.
        @Override
        public void onDrivingInfo(DrivingInfo info) {
            DrivingState dest = info.getDestination();
            if (dest != null) {
                int seconds = dest.getDuration();
                int meters = dest.getDistance();
                main.post(() -> {
                    if (live != null) {
                        live.onProgress(seconds, meters);
                    }
                });
            }
        }

        @Override
        public void onDestinationArrived(DestinationArrivedInfo info) {
            String name = info.getName();
            main.post(() -> {
                if (live != null) {
                    live.onDestinationArrived(name);
                }
            });
        }

        @Override
        public void onWayPointArrived(WaypointArrivedInfo info) {
            String name = info.getName();
            main.post(() -> {
                if (live != null) {
                    live.onWaypointArrived(name);
                }
            });
        }

        @Override
        public void onRouteCancelled() {
            main.post(() -> {
                if (live != null) {
                    live.onCancelled();
                }
            });
        }
    };
    private final Runnable timeoutTask = () -> {
        Callback waiting = this.pending;
        if (waiting != null) {
            waiting.onRouteWaiting();
        }
    };

    private Callback pending;
    private boolean initialized;

    public NavigationGateway(Context context) {
        navi = new NaviHelper(context.getApplicationContext());
    }

    public void initialize(Runnable ready, Consumer<String> failed) {
        navi.initialize(
                () -> {
                    navi.addListener(listener);
                    initialized = true;
                    main.post(ready);
                    return Unit.INSTANCE;
                },
                error -> {
                    String message = error.getMessage() != null ? error.getMessage() : error.toString();
                    main.post(() -> failed.accept(message));
                    return Unit.INSTANCE;
                });
    }

    /**
     * Sets the navigation destination to {@code destination}. When {@code waypoint} is non-null
     * (scenario 02), the waypoint is visited first and the destination remains the final stop.
     */
    public void requestRoute(CandidatePoi destination, @Nullable CandidatePoi waypoint,
                             Callback callback) {
        if (!initialized) {
            callback.onRouteFailed("내비게이션 연결 전입니다");
            return;
        }
        if (pending != null) {
            callback.onRouteFailed("이전 경로 요청이 아직 끝나지 않았습니다");
            return;
        }
        pending = callback;
        main.postDelayed(timeoutTask, ROUTE_TIMEOUT_MS);
        try {
            RouteInfo route = toRoute(destination);
            if (waypoint == null) {
                navi.requestRoute(route);
            } else {
                RequestWaypointInfo via = new RequestWaypointInfo(
                        waypoint.latitude,
                        waypoint.longitude,
                        waypoint.name,
                        WaypointIndex.FIRST,
                        waypoint.id,
                        waypoint.address,
                        "",
                        waypoint.demoData ? "demo" : "pleos");
                navi.requestRouteWithWaypoints(new RouteWithWaypointsRequest(
                        route, Collections.singletonList(via)));
            }
        } catch (RuntimeException e) {
            finish(false, "경로 요청 실패: " + e.getMessage());
        }
    }

    public void release() {
        main.removeCallbacksAndMessages(null);
        pending = null;
        if (initialized) {
            navi.removeListener(listener);
        }
        navi.release();
        initialized = false;
    }

    private static RouteInfo toRoute(CandidatePoi poi) {
        return new RouteInfo(
                poi.latitude,
                poi.longitude,
                null,
                null,
                poi.name,
                poi.id,
                poi.address,
                "",
                RouteOption.RECOMMENDED,
                poi.demoData ? "demo" : "pleos",
                false);
    }

    private void finish(boolean success, @Nullable String reason) {
        main.post(() -> {
            if (pending == null) {
                return;
            }
            Callback callback = pending;
            pending = null;
            main.removeCallbacks(timeoutTask);
            if (success) {
                callback.onRouteCompleted();
            } else {
                callback.onRouteFailed(reason);
            }
        });
    }
}
