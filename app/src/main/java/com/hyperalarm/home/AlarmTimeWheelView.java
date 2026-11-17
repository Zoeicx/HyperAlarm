package com.hyperalarm.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.OverScroller;
import com.hyperalarm.R;

public class AlarmTimeWheelView extends View {
    public interface OnTimeChangedListener {
        void onTimeChanged(int hour, int minute);
    }

    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int hour = 8;
    private int minute = 0;
    private float itemHeight;
    private float hourOffset;
    private float minuteOffset;
    private float downY;
    private boolean draggingHour;
    private boolean dragging;
    private boolean flinging;
    private boolean flingHour;
    private float flingStartOffset;
    private OverScroller scroller;
    private VelocityTracker velocityTracker;
    private OnTimeChangedListener listener;

    public AlarmTimeWheelView(Context context) {
        this(context, null);
    }

    public AlarmTimeWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().scaledDensity;
        selectedPaint.setColor(0xffffffff);
        selectedPaint.setTextSize(38 * density);
        selectedPaint.setTextAlign(Paint.Align.CENTER);
        selectedPaint.setTypeface(Typeface.DEFAULT_BOLD);
        normalPaint.setColor(0xffffffff);
        normalPaint.setTextSize(20 * density);
        normalPaint.setTextAlign(Paint.Align.CENTER);
        normalPaint.setTypeface(Typeface.DEFAULT);
        labelPaint.setColor(0x80ffffff);
        labelPaint.setTextSize(13 * density);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.DEFAULT);
        itemHeight = 46 * getResources().getDisplayMetrics().density;
        scroller = new OverScroller(context);
        setFocusable(true);
    }

    public void setOnTimeChangedListener(OnTimeChangedListener listener) {
        this.listener = listener;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setTime(int hour, int minute) {
        this.hour = Math.floorMod(hour, 24);
        this.minute = Math.floorMod(minute, 60);
        hourOffset = 0;
        minuteOffset = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 4f;
        float minuteX = getWidth() * 3f / 4f;
        float labelY = 18 * getResources().getDisplayMetrics().density;
        canvas.drawText(getContext().getString(R.string.wheel_hour), centerX, labelY, labelPaint);
        canvas.drawText(getContext().getString(R.string.wheel_minute), minuteX, labelY, labelPaint);

        float centerY = getHeight() * 0.58f;
        float clipTop = 40 * getResources().getDisplayMetrics().density;
        canvas.save();
        canvas.clipRect(0, clipTop, getWidth(), getHeight());
        drawColumn(canvas, centerX, centerY, hour, 24, hourOffset);
        drawColumn(canvas, minuteX, centerY, minute, 60, minuteOffset);
        canvas.restore();
    }

    private void drawColumn(Canvas canvas, float x, float centerY, int value, int count, float offset) {
        int centerIndex = Math.round(-offset);
        float density = getResources().getDisplayMetrics().scaledDensity;
        for (int i = centerIndex - 2; i <= centerIndex + 2; i++) {
            float distance = Math.abs(i + offset);
            int current = Math.floorMod(value + i, count);
            String text = String.format(java.util.Locale.US, "%02d", current);
            float y = centerY + (i + offset) * itemHeight;
            if (distance < 0.5f) {
                Paint.FontMetrics fm = selectedPaint.getFontMetrics();
                canvas.drawText(text, x, y - (fm.ascent + fm.descent) / 2f, selectedPaint);
            } else {
                float t = Math.min(1f, distance / 2f);
                float colorFactor = t >= 0.5f
                        ? 0.85f + ((t - 0.5f) / 0.5f) * 0.15f
                        : (t / 0.5f) * 0.85f;
                normalPaint.setTextSize((38 - 18 * t) * density);
                normalPaint.setAlpha((int) (255 - 204 * colorFactor));
                Paint.FontMetrics fm = normalPaint.getFontMetrics();
                canvas.drawText(text, x, y - (fm.ascent + fm.descent) / 2f, normalPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (flinging) {
                    float current = flingStartOffset + scroller.getCurrY() / itemHeight;
                    scroller.forceFinished(true);
                    flinging = false;
                    commitOffset(current, flingHour);
                }
                dragging = true;
                downY = event.getY();
                draggingHour = event.getX() < getWidth() / 2f;
                hourOffset = 0;
                minuteOffset = 0;
                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    float next = (event.getY() - downY) / itemHeight;
                    if (draggingHour) {
                        hourOffset = next;
                    } else {
                        minuteOffset = next;
                    }
                    if (velocityTracker != null) velocityTracker.addMovement(event);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    dragging = false;
                    float activeOffset = draggingHour ? hourOffset : minuteOffset;
                    if (velocityTracker != null) {
                        velocityTracker.addMovement(event);
                        velocityTracker.computeCurrentVelocity(1000, getResources().getDisplayMetrics().density * 8000);
                        float velocityY = velocityTracker.getYVelocity();
                        velocityTracker.recycle();
                        velocityTracker = null;
                        if (Math.abs(velocityY) > getResources().getDisplayMetrics().density * 220) {
                            flinging = true;
                            flingHour = draggingHour;
                            flingStartOffset = activeOffset;
                            scroller.fling(0, 0, 0, (int) velocityY,
                                    0, 0, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);
                            postInvalidateOnAnimation();
                            return true;
                        }
                    }
                    commitOffset(activeOffset, draggingHour);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void commitOffset(float activeOffset, boolean hourColumn) {
        int shift = Math.round(activeOffset);
        if (shift != 0) {
            if (hourColumn) {
                hour = Math.floorMod(hour - shift, 24);
            } else {
                minute = Math.floorMod(minute - shift, 60);
            }
        }
        hourOffset = 0;
        minuteOffset = 0;
        invalidate();
        if (listener != null) listener.onTimeChanged(hour, minute);
        performClick();
    }

    @Override
    public void computeScroll() {
        if (!flinging) return;
        if (scroller.computeScrollOffset()) {
            float current = flingStartOffset + scroller.getCurrY() / itemHeight;
            if (flingHour) {
                hourOffset = current;
            } else {
                minuteOffset = current;
            }
            invalidate();
            postInvalidateOnAnimation();
        }
        if (scroller.isFinished() && flinging) {
            flinging = false;
            float activeOffset = flingHour ? hourOffset : minuteOffset;
            commitOffset(activeOffset, flingHour);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
