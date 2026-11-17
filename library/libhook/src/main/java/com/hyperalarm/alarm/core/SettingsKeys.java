package com.hyperalarm.alarm.core;

/**
 * Keys shared between the app and the clock process.
 *
 * <p>These live in system global settings on purpose: the clock process cannot read the module's
 * own preferences, and {@code Settings.Global} is the only store both sides can use without a
 * binder round trip. One key, one meaning - never mirror them into the module preferences.</p>
 */
public final class SettingsKeys {
    /** 1 = the clock hides {@code .sh} script alarms from its own list; scheduling is untouched. */
    public static final String CLOCK_FILTER = "hyperalarm_hide_script_alarms";

    /**
     * 1 = also hide ringing tasks ({@code @x.sh}) from the clock list.
     *
     * <p>Only meaningful while {@link #CLOCK_FILTER} is 1: silent tasks are hidden there, and this
     * says whether the ones that still ring should disappear as well. Default 0 keeps them visible,
     * because a task that rings is something the user expects to see.</p>
     */
    public static final String CLOCK_FILTER_RINGING = "hyperalarm_hide_ringing_task_alarms";

    private SettingsKeys() {
    }
}
