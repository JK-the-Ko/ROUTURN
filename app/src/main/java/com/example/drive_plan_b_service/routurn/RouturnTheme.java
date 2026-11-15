package com.example.drive_plan_b_service.routurn;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.drive_plan_b_service.R;

/**
 * Builds the DRIVE PLAN B visual components from the pb_* color tokens.
 * Sizes follow the Pleos UI guide principles: large one-hand targets (56dp+), text 15sp+,
 * no motion, and at most three lines of copy per block.
 */
final class RouturnTheme {
    static final int RADIUS_DP = 16;
    static final int TARGET_DP = 56;

    private RouturnTheme() {
    }

    static int color(Context c, int id) {
        return c.getColor(id);
    }

    static int dp(Context c, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, c.getResources().getDisplayMetrics()));
    }

    static LinearLayout.LayoutParams matchWrap(Context c, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topDp);
        return lp;
    }

    static TextView text(Context c, String value, float sp, int colorId, boolean bold) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color(c, colorId));
        t.setLineSpacing(0, 1.25f);
        if (bold) {
            t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return t;
    }

    static GradientDrawable rounded(Context c, int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(c, radiusDp));
        d.setColor(fillColor);
        if (strokeColor != 0) {
            d.setStroke(dp(c, 1), strokeColor);
        }
        return d;
    }

    /** A card surface with a soft outline; use as a container for rows of text. */
    static LinearLayout card(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(c, 18);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(rounded(c, color(c, R.color.pb_surface),
                color(c, R.color.pb_outline), RADIUS_DP));
        return card;
    }

    /** Tappable card for a candidate or intent. Presses are shown with a ripple, not motion. */
    static LinearLayout tappableCard(Context c, View.OnClickListener listener) {
        LinearLayout card = card(c);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(listener);
        Drawable content = rounded(c, color(c, R.color.pb_surface),
                color(c, R.color.pb_outline), RADIUS_DP);
        card.setBackground(new RippleDrawable(
                ColorStateList.valueOf(color(c, R.color.pb_ripple)), content, null));
        return card;
    }

    /** Chip for a short status label. kind: POSITIVE, NEUTRAL, or WARNING. */
    static TextView chip(Context c, String value, Kind kind) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        int fg;
        int bg;
        switch (kind) {
            case POSITIVE:
                fg = R.color.pb_positive;
                bg = R.color.pb_positive_bg;
                break;
            case WARNING:
                fg = R.color.pb_warning;
                bg = R.color.pb_warning_bg;
                break;
            default:
                fg = R.color.pb_text_secondary;
                bg = R.color.pb_neutral_bg;
        }
        t.setTextColor(color(c, fg));
        t.setBackground(rounded(c, color(c, bg), 0, 8));
        int padH = dp(c, 10);
        int padV = dp(c, 4);
        t.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 8);
        lp.setMarginEnd(dp(c, 8));
        t.setLayoutParams(lp);
        return t;
    }

    enum Kind {
        POSITIVE, NEUTRAL, WARNING
    }

    /** Full-width primary action (accent fill). */
    static Button primaryButton(Context c, String label, View.OnClickListener listener) {
        return action(c, label, listener, true);
    }

    /** Full-width secondary action (outlined). */
    static Button secondaryButton(Context c, String label, View.OnClickListener listener) {
        return action(c, label, listener, false);
    }

    private static Button action(Context c, String label, View.OnClickListener listener,
                                 boolean primary) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        b.setMinHeight(dp(c, TARGET_DP));
        b.setMinimumHeight(dp(c, TARGET_DP));
        b.setGravity(Gravity.CENTER);
        int textColor = primary ? R.color.pb_accent_text : R.color.pb_text;
        b.setTextColor(color(c, textColor));
        int fill = primary ? R.color.pb_accent : R.color.pb_surface;
        int stroke = primary ? 0 : R.color.pb_outline;
        Drawable content = rounded(c, color(c, fill),
                stroke == 0 ? 0 : color(c, stroke), RADIUS_DP);
        b.setBackground(new RippleDrawable(
                ColorStateList.valueOf(color(c, R.color.pb_ripple)), content, null));
        b.setOnClickListener(listener);
        b.setLayoutParams(matchWrap(c, 12));
        return b;
    }
}
