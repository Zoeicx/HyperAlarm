package com.hyperalarm.home;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.hyperalarm.R;

/**
 * The "next alarm" card at the top of the list.
 *
 * <p>The countdown it shows is relative ("10 hours 12 minutes from now"), so it goes stale on its
 * own: a value painted when the list was built is wrong one minute later. The card therefore owns a
 * tick that repaints on every minute boundary while it is attached, and asks its supplier for the
 * text instead of keeping a snapshot.</p>
 */
public class NextAlarmPreference extends Preference {

    /** Produces the two lines for the current moment. */
    public interface TextSupplier {
        String get();
    }

    private String text = "";
    private TextSupplier supplier;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (supplier != null) {
                text = supplier.get();
                notifyChanged();
            }
            scheduleTick();
        }
    };

    public NextAlarmPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.item_alarm_next);
        setSelectable(false);
        setPersistent(false);
        setKey("next-alarm");
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        notifyChanged();
    }

    /**
     * Makes the card repaint itself every minute.
     *
     * @param supplier called on each tick and on attach; must compute from current data
     */
    public void setTextSupplier(TextSupplier supplier) {
        this.supplier = supplier;
        scheduleTick();
    }

    /** Drops the supplier and the pending tick; called when the owning list goes away. */
    public void stopTicking() {
        supplier = null;
        handler.removeCallbacks(tick);
    }

    private void scheduleTick() {
        handler.removeCallbacks(tick);
        if (supplier == null) return;
        // Land just after the boundary so the new minute is already in effect when we read it.
        long now = System.currentTimeMillis();
        long delay = 60_000L - (now % 60_000L) + 200L;
        handler.postDelayed(tick, delay);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        scheduleTick();
    }

    @Override
    public void onDetached() {
        handler.removeCallbacks(tick);
        super.onDetached();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setBackgroundColor(0xFF000000);
        holder.itemView.setPadding(0, 0, 0, 0);
        String[] parts = text.split("\n", 2);
        TextView time = holder.itemView.findViewById(R.id.next_alarm_time);
        TextView script = holder.itemView.findViewById(R.id.next_alarm_script);
        if (time != null) time.setText(parts.length > 0 ? parts[0] : "");
        if (script != null) {
            String action = parts.length > 1 ? parts[1] : "";
            script.setText(action);
            int size = 26;
            if (action.length() > 34) size = 15;
            else if (action.length() > 26) size = 18;
            else if (action.length() > 18) size = 21;
            else if (action.length() > 12) size = 24;
            script.setTextSize(size);
        }
    }
}
