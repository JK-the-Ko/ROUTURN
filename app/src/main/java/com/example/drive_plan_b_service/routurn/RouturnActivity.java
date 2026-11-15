package com.example.drive_plan_b_service.routurn;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.drive_plan_b_service.R;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Plan-B flow for scenarios 01-A, 01-B and 02:
 * intent card -> candidate cards (up to 2) -> select -> confirm -> NaviHelper -> success or failure.
 */
public final class RouturnActivity extends AppCompatActivity {
    /** Simulated search delay so the busy state is visible. */
    private static final long SEARCH_DELAY_MS = 800L;
    private static final String[] RANK_LABELS = {"추천", "다음", "다른 곳"};
    private static final int TOP_RESERVED_DP = 96;
    private static final int BOTTOM_RESERVED_DP = 112;

    private final RouturnSession session = new RouturnSession();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<CandidatePoi> results = new ArrayList<>();

    private NavigationGateway gateway;
    private LinearLayout actions;
    private TextView statusView;
    private TextView detailView;
    private ProgressBar busy;
    private boolean navReady;
    private String navError;
    /** True while the scenario 02 stop picker is shown (화장실, 카페, 편의점, 충전소). */
    private boolean pickingStop;

    // Live guidance state, updated by NavigationGateway.LiveListener.
    private int remainingSeconds = -1;
    private int remainingMeters = -1;
    @Nullable private String arrivedStop;
    private boolean arrivedDestination;
    private boolean cancelled;
    @Nullable private TextView etaView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(RouturnTheme.color(this, R.color.pb_bg));
        scroll.setFillViewport(true);
        // Keep content out of the car's top status overlay and the bottom system dock.
        // Values measured on the Pleos emulator: status overlay ~88dp, bottom dock ~107dp.
        scroll.setPadding(0, RouturnTheme.dp(this, TOP_RESERVED_DP), 0, RouturnTheme.dp(this, BOTTOM_RESERVED_DP));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = RouturnTheme.dp(this, 20);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        root.addView(RouturnTheme.text(this, "ROUTURN", 14, R.color.pb_accent, true));

        // Original visit: always visible, stored separately from the navigation destination.
        LinearLayout visit = RouturnTheme.card(this);
        visit.setLayoutParams(RouturnTheme.matchWrap(this, 12));
        visit.addView(RouturnTheme.text(this, "원래 방문", 14, R.color.pb_text_secondary, false));
        visit.addView(RouturnTheme.text(this, SampleData.ORIGINAL_RESTAURANT.name,
                20, R.color.pb_text, true));
        root.addView(visit);

        statusView = RouturnTheme.text(this, "", 26, R.color.pb_text, true);
        statusView.setLayoutParams(RouturnTheme.matchWrap(this, 24));
        root.addView(statusView);

        detailView = RouturnTheme.text(this, "", 16, R.color.pb_text_secondary, false);
        detailView.setLayoutParams(RouturnTheme.matchWrap(this, 6));
        root.addView(detailView);

        busy = new ProgressBar(this);
        busy.setIndeterminate(true);
        busy.setIndeterminateTintList(ColorStateList.valueOf(
                RouturnTheme.color(this, R.color.pb_accent)));
        LinearLayout.LayoutParams busyLp = new LinearLayout.LayoutParams(
                RouturnTheme.dp(this, 40), RouturnTheme.dp(this, 40));
        busyLp.topMargin = RouturnTheme.dp(this, 16);
        busy.setLayoutParams(busyLp);
        root.addView(busy);

        actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setLayoutParams(RouturnTheme.matchWrap(this, 8));
        root.addView(actions);

        TextView footer = RouturnTheme.text(this, "시연용 데이터로 동작합니다. 실제 잔여면과 운영 정보는 확인되지 않은 상태로 표시됩니다.",
                13, R.color.pb_text_secondary, false);
        footer.setLayoutParams(RouturnTheme.matchWrap(this, 28));
        root.addView(footer);

        gateway = new NavigationGateway(this);
        gateway.setLiveListener(new NavigationGateway.LiveListener() {
            @Override
            public void onProgress(int seconds, int meters) {
                remainingSeconds = seconds;
                remainingMeters = meters;
                // Update only the text, so the buttons are not rebuilt on every update.
                if (etaView != null) {
                    etaView.setText(etaText());
                }
            }

            @Override
            public void onDestinationArrived(String name) {
                arrivedDestination = true;
                render();
            }

            @Override
            public void onWaypointArrived(String name) {
                arrivedStop = name;
                render();
            }

            @Override
            public void onCancelled() {
                RouturnState state = session.getState();
                if (state == RouturnState.WAITING_START) {
                    // Cancelled from the navigation preview: nothing was started, so go back to the picker.
                    gateway.clearPending();
                    pickingStop = false;
                    resetLive();
                    session.reset();
                } else if (state == RouturnState.ROUTING) {
                    gateway.clearPending();
                    session.onRouteFailed("경로 요청이 취소되었습니다.");
                } else {
                    // Cancelled during guidance: keep the SUCCESS screen and offer a way back.
                    cancelled = true;
                }
                render();
            }
        });
        gateway.initialize(
                () -> {
                    navReady = true;
                    navError = null;
                    render();
                },
                message -> {
                    navReady = false;
                    navError = message;
                    render();
                });

        render();
    }

    private void render() {
        actions.removeAllViews();
        RouturnState state = session.getState();
        RouturnIntent intent = session.getIntent();
        busy.setVisibility(android.view.View.GONE);
        switch (state) {
            case IDLE:
                statusView.setText("무엇을 바꿀까요?");
                detailView.setText(navReady
                        ? "아래에서 상황에 맞는 항목을 고르세요."
                        : navError != null ? "내비게이션 연결 실패: " + navError : "내비게이션에 연결하는 중입니다.");
                if (pickingStop) {
                    renderStopPicker();
                    break;
                }
                for (RouturnIntent option : RouturnIntent.values()) {
                    actions.addView(intentCard(option, navReady));
                }
                break;
            case SEARCHING:
                statusView.setText(searchingLabel(intent, session.getCategory()));
                detailView.setText("주변 후보를 찾는 중입니다.");
                busy.setVisibility(android.view.View.VISIBLE);
                break;
            case RESULTS:
                statusView.setText("후보 " + results.size() + "곳");
                detailView.setText(resultsHint(intent));
                for (int i = 0; i < results.size(); i++) {
                    actions.addView(candidateCard(intent, results.get(i), i));
                }
                actions.addView(RouturnTheme.secondaryButton(this, "다른 요청하기",
                        v -> onRequestAgain()));
                break;
            case CONFIRM: {
                CandidatePoi poi = session.getSelected();
                if (intent == RouturnIntent.RESTROOM_FIRST) {
                    statusView.setText(poi.name + "에\n먼저 들를까요?");
                    detailView.setText("최종 목적지 '" + SampleData.ORIGINAL_RESTAURANT.name
                            + "'은 그대로 유지됩니다.");
                    actions.addView(RouturnTheme.primaryButton(this, "예, 경유지 추가", v -> onConfirm()));
                } else {
                    statusView.setText(poi.name + "(으)로\n목적지를 바꿀까요?");
                    detailView.setText(intent == RouturnIntent.PARKING_KEEP_RESTAURANT
                            ? SampleData.ORIGINAL_RESTAURANT.name + "은(는) 그대로 유지됩니다."
                            : "원래 식당 '" + SampleData.ORIGINAL_RESTAURANT.name
                                    + "'은 목적지에서 빠집니다. 원래 방문 정보는 앱에 보관됩니다.");
                    actions.addView(RouturnTheme.primaryButton(this, "예, 목적지 변경", v -> onConfirm()));
                }
                actions.addView(RouturnTheme.secondaryButton(this, "아니오, 다시 고르기", v -> onCancel()));
                break;
            }
            case ROUTING:
                statusView.setText("경로를 설정하는 중");
                detailView.setText(session.getSelected().name);
                busy.setVisibility(android.view.View.VISIBLE);
                break;
            case WAITING_START:
                statusView.setText("네비게이션에서 경로를 확인하세요");
                detailView.setText("'안내 시작'을 누르면 안내가 시작됩니다. 취소하려면 아래 버튼을 누르세요.");
                actions.addView(RouturnTheme.primaryButton(this, "미리보기 취소 · 다시 고르기",
                        v -> gateway.cancelRoute()));
                break;
            case SUCCESS:
                statusView.setText(cancelled ? "안내가 취소되었습니다" : "경로 변경 완료");
                detailView.setText(cancelled
                        ? "ROUTURN 화면으로 돌아왔습니다. 아래 버튼으로 처음부터 다시 고르세요."
                        : "안내를 진행하고 있습니다.");
                if (cancelled) {
                    // Back-to-start is placed first so it is visible without scrolling.
                    actions.addView(RouturnTheme.primaryButton(this, "처음으로 돌아가기", v -> onRequestAgain()));
                }
                actions.addView(liveCard(intent, session.getSelected()));
                if (arrivedStop != null) {
                    actions.addView(RouturnTheme.primaryButton(this, "용무 완료 · 식당 안내 확인",
                            v -> onStopDone()));
                    actions.addView(RouturnTheme.secondaryButton(this, "다른 요청하기", v -> onRequestAgain()));
                } else if (!cancelled) {
                    actions.addView(RouturnTheme.primaryButton(this, "다른 요청하기", v -> onRequestAgain()));
                }
                if (isDebuggable() && !cancelled) {
                    // Test hook only: exercises the onRouteCancelled path. Not shipped in release builds.
                    actions.addView(RouturnTheme.secondaryButton(this, "[디버그] 안내 취소",
                            v -> gateway.cancelRoute()));
                }
                break;
            case ERROR:
                statusView.setText("경로 변경 실패");
                detailView.setText("이전 경로는 그대로 유지됩니다.");
                LinearLayout errorCard = RouturnTheme.card(this);
                errorCard.setLayoutParams(RouturnTheme.matchWrap(this, 8));
                errorCard.addView(RouturnTheme.chip(this, "확인 필요", RouturnTheme.Kind.WARNING));
                errorCard.addView(RouturnTheme.text(this, session.getErrorMessage(),
                        16, R.color.pb_text, false));
                actions.addView(errorCard);
                actions.addView(RouturnTheme.primaryButton(this, "다시 고르기", v -> onRequestAgain()));
                break;
            default:
                throw new IllegalStateException("Unhandled state " + state);
        }
    }

    private void renderStopPicker() {
        statusView.setText("먼저 들를 곳");
        detailView.setText("식당 목적지는 그대로 유지됩니다. 어디에 들를지 고르세요.");
        for (StopCategory stop : StopCategory.values()) {
            LinearLayout card = RouturnTheme.tappableCard(this, v -> {
                pickingStop = false;
                onRequest(RouturnIntent.RESTROOM_FIRST, stop.label);
            });
            card.setLayoutParams(RouturnTheme.matchWrap(this, 12));
            card.addView(RouturnTheme.text(this, stop.label, 20, R.color.pb_text, true));
            card.addView(RouturnTheme.text(this, stop.intro, 15, R.color.pb_text_secondary, false));
            actions.addView(card);
        }
        actions.addView(RouturnTheme.secondaryButton(this, "뒤로", v -> {
            pickingStop = false;
            render();
        }));
    }

    private static String searchingLabel(RouturnIntent intent, String category) {
        if (intent == RouturnIntent.RESTROOM_FIRST && category != null) {
            return category + " 찾는 중";
        }
        return intent.buttonLabel;
    }

    private LinearLayout intentCard(RouturnIntent option, boolean enabled) {
        LinearLayout card = RouturnTheme.tappableCard(this, v -> {
            if (option == RouturnIntent.RESTROOM_FIRST) {
                pickingStop = true;
                render();
            } else {
                onRequest(option, option.category);
            }
        });
        card.setEnabled(enabled);
        card.setLayoutParams(RouturnTheme.matchWrap(this, 12));
        card.addView(RouturnTheme.chip(this, option.scenarioId, RouturnTheme.Kind.NEUTRAL));
        card.addView(RouturnTheme.text(this, option.buttonLabel, 20, R.color.pb_text, true));
        card.addView(RouturnTheme.text(this, introFor(option), 15, R.color.pb_text_secondary, false));
        return card;
    }

    private LinearLayout candidateCard(RouturnIntent intent, CandidatePoi poi, int index) {
        LinearLayout card = RouturnTheme.tappableCard(this, v -> onSelect(poi));
        card.setLayoutParams(RouturnTheme.matchWrap(this, 12));
        LinearLayout badges = new LinearLayout(this);
        badges.setOrientation(LinearLayout.HORIZONTAL);
        badges.addView(RouturnTheme.chip(this,
                index < RANK_LABELS.length ? RANK_LABELS[index] : "", RouturnTheme.Kind.NEUTRAL));
        card.addView(badges);
        card.addView(RouturnTheme.text(this, poi.name, 20, R.color.pb_text, true));
        card.addView(RouturnTheme.text(this, factLine(intent, poi), 16, R.color.pb_text_secondary, false));
        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.HORIZONTAL);
        status.addView(statusChip(intent, poi));
        card.addView(status);
        return card;
    }

    /** Live guidance card: remaining time, stop and arrival status from the navigation app. */
    private LinearLayout liveCard(RouturnIntent intent, CandidatePoi poi) {
        LinearLayout card = RouturnTheme.card(this);
        card.setLayoutParams(RouturnTheme.matchWrap(this, 8));
        card.addView(RouturnTheme.text(this, "지금 안내 중", 14, R.color.pb_text_secondary, false));
        card.addView(RouturnTheme.text(this, poi.name, 20, R.color.pb_text, true));
        etaView = RouturnTheme.text(this, etaText(), 18, R.color.pb_text, false);
        card.addView(etaView);

        if (cancelled) {
            card.addView(RouturnTheme.chip(this, "안내 취소됨", RouturnTheme.Kind.WARNING));
        } else if (arrivedStop != null) {
            card.addView(RouturnTheme.chip(this, "경유지 도착", RouturnTheme.Kind.POSITIVE));
            card.addView(RouturnTheme.text(this,
                    arrivedStop + " 도착. 용무를 마치면 식당까지 안내를 이어갑니다.",
                    16, R.color.pb_text, false));
        } else if (arrivedDestination) {
            card.addView(RouturnTheme.chip(this, "도착", RouturnTheme.Kind.POSITIVE));
            String next = intent == RouturnIntent.PARKING_KEEP_RESTAURANT && poi.walkMinutes != null
                    ? "주차를 마쳤습니다. 식당까지 도보 약 " + poi.walkMinutes + "분입니다."
                    : "목적지에 도착했습니다.";
            card.addView(RouturnTheme.text(this, next, 16, R.color.pb_text, false));
        } else {
            String note = intent == RouturnIntent.RESTROOM_FIRST
                    ? "경유 후 '" + SampleData.ORIGINAL_RESTAURANT.name + "'(으)로 이동합니다."
                    : "원래 방문 정보는 앱에 보관되어 있습니다.";
            card.addView(RouturnTheme.text(this, note, 16, R.color.pb_text_secondary, false));
        }
        return card;
    }

    private String etaText() {
        if (remainingSeconds < 0) {
            return "경로 정보를 받는 중입니다.";
        }
        int minutes = (remainingSeconds + 59) / 60;
        return "남은 시간 " + minutes + "분 · " + String.format(Locale.KOREA, "%.1f km",
                remainingMeters / 1000.0);
    }

    /** True only for debug builds; used to show the test-only cancel hook. */
    private boolean isDebuggable() {
        return (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private void onStopDone() {
        // The waypoint is done: clear the arrival banner and ask the navigation app for the current state.
        arrivedStop = null;
        gateway.refreshStatus();
        render();
    }

    private void resetLive() {
        remainingSeconds = -1;
        remainingMeters = -1;
        arrivedStop = null;
        arrivedDestination = false;
        cancelled = false;
        etaView = null;
    }

    private static String introFor(RouturnIntent option) {
        switch (option) {
            case PARKING_KEEP_RESTAURANT:
                return "식당은 그대로 두고, 가까운 주차장을 찾습니다.";
            case PASTA_CHANGE_RESTAURANT:
                return "파스타 식사는 유지하고, 주차 가능한 다른 식당을 찾습니다.";
            case RESTROOM_FIRST:
                return "식당 목적지는 유지하고, 화장실이나 카페 등을 먼저 들릅니다.";
            default:
                return "";
        }
    }

    private static String resultsHint(RouturnIntent intent) {
        switch (intent) {
            case PARKING_KEEP_RESTAURANT:
                return "잔여면이 확인되지 않은 곳은 '미확인'으로 표시합니다.";
            case PASTA_CHANGE_RESTAURANT:
                return "주차시설이 있는 곳만 보여드립니다. 잔여면은 확인되지 않았습니다.";
            case RESTROOM_FIRST:
                return "이용 가능 여부는 확인되지 않았습니다. 도착이 빠른 순서입니다.";
            default:
                return "";
        }
    }

    private static String factLine(RouturnIntent intent, CandidatePoi poi) {
        String drive = poi.driveMinutes != null ? "차량 " + poi.driveMinutes + "분" : "차량 시간 -";
        switch (intent) {
            case PARKING_KEEP_RESTAURANT:
                String walk = poi.walkMinutes != null ? " · 도보 " + poi.walkMinutes + "분" : "";
                return drive + walk;
            case PASTA_CHANGE_RESTAURANT:
                return drive + " · 주차시설 있음";
            case RESTROOM_FIRST:
                return drive + " · 주차 " + (poi.hasParking ? "가능" : "없음");
            default:
                return drive;
        }
    }

    /** Status chip: only confirmed values get the positive style; everything else is "미확인". */
    private TextView statusChip(RouturnIntent intent, CandidatePoi poi) {
        if (intent == RouturnIntent.PARKING_KEEP_RESTAURANT && poi.hasConfirmedSpaces()) {
            return RouturnTheme.chip(this,
                    "잔여 " + poi.availableSpaces + "면 · " + poi.spacesUpdatedAt,
                    RouturnTheme.Kind.POSITIVE);
        }
        if (intent == RouturnIntent.RESTROOM_FIRST) {
            return RouturnTheme.chip(this, "이용 가능 여부 미확인", RouturnTheme.Kind.NEUTRAL);
        }
        return RouturnTheme.chip(this, "잔여면 미확인", RouturnTheme.Kind.NEUTRAL);
    }

    private void onRequest(RouturnIntent intent, String category) {
        resetLive();
        session.startSearch(intent, category);
        render();
        main.postDelayed(() -> {
            results.clear();
            results.addAll(CandidateRanker.rank(intent, category, SampleData.allCandidates()));
            session.showResults();
            render();
        }, SEARCH_DELAY_MS);
    }

    private void onRequestAgain() {
        // Returns to the intent picker; the previous route stays as it was.
        pickingStop = false;
        resetLive();
        session.reset();
        render();
    }

    private void onSelect(CandidatePoi poi) {
        session.selectCandidate(poi);
        render();
    }

    private void onCancel() {
        session.cancelConfirm();
        render();
    }

    private void onConfirm() {
        RouturnIntent intent = session.getIntent();
        CandidatePoi selected = session.getSelected();
        session.confirm();
        render();
        if (intent == RouturnIntent.RESTROOM_FIRST) {
            // 02: restaurant stays the final destination, restroom is the waypoint.
            gateway.requestRoute(SampleData.ORIGINAL_RESTAURANT, selected, routeCallback());
        } else {
            // 01-A: the parking lot becomes the destination; 01-B: the new restaurant does.
            gateway.requestRoute(selected, null, routeCallback());
        }
    }

    private NavigationGateway.Callback routeCallback() {
        return new NavigationGateway.Callback() {
            @Override
            public void onRouteCompleted() {
                session.onRouteSucceeded();
                render();
            }

            @Override
            public void onRouteFailed(String reason) {
                session.onRouteFailed(reason);
                render();
            }

            @Override
            public void onRouteWaiting() {
                session.onRouteWaiting();
                render();
            }
        };
    }

    @Override
    protected void onDestroy() {
        main.removeCallbacksAndMessages(null);
        if (gateway != null) {
            gateway.release();
            gateway = null;
        }
        super.onDestroy();
    }
}
