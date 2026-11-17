package com.hyperalarm.home;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.hyperalarm.R;
import com.hyperalarm.common.utils.RootShell;

import fan.slidingwidget.widget.SlidingButton;

public class AlarmPreference extends Preference {
    public interface OnOpenListener {
        void onOpen();
    }

    public interface SelectionHost {
        boolean isSelectionMode();
        boolean isSelected(int alarmId);
        void onClick(int alarmId);
        void onLongClick(int alarmId);
    }

    private final int id;
    private final int hour;
    private final int minute;
    private final int days;
    private final String label;
    private final String shiftInfo;
    private final int shiftGroupRowId;
    private final SelectionHost selectionHost;
    private boolean selected;
    private boolean enabled;
    private final OnOpenListener openListener;

    public AlarmPreference(Context context, int id, int hour, int minute, int days,
                           String label, boolean enabled, String shiftInfo, int shiftGroupRowId,
                           SelectionHost selectionHost, OnOpenListener openListener) {
        super(context);
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.days = days;
        this.label = label == null ? "" : label;
        this.shiftInfo = shiftInfo == null ? "" : shiftInfo;
        this.shiftGroupRowId = shiftGroupRowId;
        this.selectionHost = selectionHost;
        this.enabled = enabled;
        this.openListener = openListener;
        setLayoutResource(R.layout.item_alarm_clock);
        setKey("alarm-" + id);
        setPersistent(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View item = holder.itemView;
        View card = item.findViewById(R.id.alarm_card);
        if (card == null) card = item;
        final View cardView = card;
        TextView time = (TextView) item.findViewById(R.id.clock_time);
        TextView summary = (TextView) item.findViewById(R.id.clock_summary);
        SlidingButton toggle = (SlidingButton) item.findViewById(R.id.clock_switch);
        CheckBox select = (CheckBox) item.findViewById(R.id.clock_select);

        time.setText(String.format("%02d:%02d", hour, minute));
        String repeat = days == 512 && !shiftInfo.isEmpty() ? shiftInfo : AlarmRepeatText.get(getContext(), days);
        String visibleLabel = label.startsWith("@") ? label.substring(1) : label;
        summary.setText(visibleLabel.isEmpty() ? repeat : repeat + " | " + visibleLabel);
        cardView.setAlpha(selected ? 1f : (enabled ? 1f : 0.4f));
        item.setBackgroundColor(selected ? 0xFF4A4A4A : 0x00000000);
        cardView.setBackgroundResource(selected
                ? R.drawable.bg_alarm_list_item_selected : R.drawable.bg_alarm_list_item);

        boolean selectionMode = selectionHost != null && selectionHost.isSelectionMode();
        toggle.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
        select.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        select.setChecked(selected);
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(enabled);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            enabled = checked;
            cardView.setAlpha(checked ? 1f : 0.4f);
            AlarmHookWriter.setEnabled(getContext(), id, checked, shiftGroupRowId > 0);
        });

        item.setOnClickListener(v -> {
            if (selectionHost != null && selectionHost.isSelectionMode()) {
                selectionHost.onClick(id);
            } else if (openListener != null) {
                openListener.onOpen();
            }
        });
        item.setOnLongClickListener(v -> {
            if (selectionHost != null) {
                selectionHost.onLongClick(id);
                return true;
            }
            return false;
        });
    }



    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void refresh() {
        notifyChanged();
    }
}
