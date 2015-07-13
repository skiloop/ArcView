package com.shiloop.arcview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


/**
 * arc view with text in center
 */
public class ArcView extends View {
    private static final String TAG = ArcView.class.getSimpleName();

    private String mArcText;
    private int mArcColor;
    private int mArcPressedColor;
    private int mInnerColor;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    private float mOuterRadius;
    private float mTextRadius;
    private float mStrokeWidth;
    private int mStrokeColor;

    private float mInnerRadiusRatio;

    private RectF mOuterArc;
    private RectF mInnerArc;
    private RectF mTextArc;

    private Paint mOuterPaint;
    private Paint mInnerPaint;

    private float mSweepAngle;
    private float mStartAngle;

    private Path mTextPath;
    private Paint mLinePaint;

    private float[] mLines;

    private float mCenterX;
    private float mCenterY;

    private float mMaxRadius;
    private float mInnerRadius;

    private boolean mIsInside = false;

    public ArcView(Context context) {
        super(context);
        init(null, 0);
    }

    public ArcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ArcView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ArcView, defStyle, 0);

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setAntiAlias(true);

        mArcText = a.getString(R.styleable.ArcView_arcText);
        setTextColor(a.getColor(R.styleable.ArcView_arcTextColor, Color.DKGRAY));
        mTextPaint.setTextSize(a.getDimension(R.styleable.ArcView_arcTextSize, 24));
        setArcColor(a.getColor(R.styleable.ArcView_arcColor, Color.WHITE));
        setPressedColor(a.getColor(R.styleable.ArcView_arcPressedColor, Color.LTGRAY));
        setInnerColor(a.getColor(R.styleable.ArcView_arcInnerColor, Color.LTGRAY));
        setMaxRadius(a.getDimension(R.styleable.ArcView_arcMaxRadius, 100));
        setStrokeWidth(a.getDimension(R.styleable.ArcView_arcStrokeWidth, 2));
        setStrokeColor(a.getColor(R.styleable.ArcView_arcStrokeColor, Color.GRAY));
        setSweepAngle(a.getFloat(R.styleable.ArcView_arcSweepAngle, 90));
        setStartAngle(a.getFloat(R.styleable.ArcView_arcStartAngle, 0));
        setInnerRadiusRatio(a.getFloat(R.styleable.ArcView_arcInnerRadiusRatio, 0.618f));
        setInnerRadiusRatio(Math.abs(getInnerRadiusRatio()));
        if (getInnerRadiusRatio() >= 1.00f) {
            setInnerRadiusRatio(0.618f);
        }
        a.recycle();

        // set up arc
        setOuterRadius(getMaxRadius());
        setInnerRadius(getOuterRadius() * getInnerRadiusRatio());
        mOuterArc = new RectF(0, 0, getOuterRadius(), getOuterRadius());
        float low = (getOuterRadius() - getInnerRadius()) / 2;
        float high = (low + getInnerRadius());
        mInnerArc = new RectF(low, low, high, high);
        setTextRadius((getOuterRadius() + getInnerRadius()) / 2);
        float textLow = (getOuterRadius() - getTextRadius()) / 2;
        float textHigh = textLow + getTextRadius();
        mTextArc = new RectF(textLow, textLow, textHigh, textHigh);

        // set up arc paint
        mOuterPaint = new Paint();
        mOuterPaint.setColor(getArcColor());
        mOuterPaint.setStyle(Paint.Style.STROKE);
        mOuterPaint.setAntiAlias(true);

        mInnerPaint = new Paint();
        mInnerPaint.setColor(getInnerColor());
        mInnerPaint.setAntiAlias(true);

        mLinePaint = new Paint();
        mLinePaint.setColor(getStrokeColor());
        mLinePaint.setStrokeWidth(getStrokeWidth());
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setAntiAlias(true);

        // set up text path
        mTextPath = new Path();
        mTextPath.addArc(mTextArc, getStartAngle(), getSweepAngle());

        mLines = new float[8];

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
        Log.d(TAG, "ratio:" + getInnerRadiusRatio());
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextWidth = mTextPaint.measureText(mArcText);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
        validateTextPath();
        validateTextArc();
    }

    protected void onMeasure(int width, int height) {
        Log.d(TAG, "onMeasure");
        super.onMeasure(width, height);
        int mPaddingLeft = getPaddingLeft();
        int mPaddingTop = getPaddingTop();
        int mPaddingRight = getPaddingRight();
        int mPaddingBottom = getPaddingBottom();
        int cWidth = getWidth();
        int cHeight = getHeight();
        int contentWidth = (cWidth - mPaddingLeft - mPaddingRight);
        int contentHeight = (cHeight - mPaddingBottom - mPaddingTop);
        changeLayout(cWidth / 2, cHeight / 2, contentWidth, contentHeight);
    }

    private void changeLayout(float center_x, float center_y, int contentWidth, int contentHeight) {
        Log.d(TAG, "changeLayout");
        setOuterRadius(Math.min(contentHeight, contentWidth) / 2);

        mCenterY = center_y;
        mCenterX = center_x;

        if (getOuterRadius() > getMaxRadius()) {
            setOuterRadius(getMaxRadius());
        }

        setInnerRadius(getOuterRadius() * getInnerRadiusRatio());

        float left;
        float top;
        float bottom, right;
        left = center_x - getOuterRadius();
        right = center_x + getOuterRadius();
        top = center_y - getOuterRadius();
        bottom = center_y + getOuterRadius();

        mOuterArc.set(left, top, right, bottom);
        mOuterPaint.setStrokeWidth(getOuterRadius() - getInnerRadius());

        left = center_x - getInnerRadius();
        right = center_x + getInnerRadius();
        top = center_y - getInnerRadius();
        bottom = center_y + getInnerRadius();
        mInnerArc.set(left, top, right, bottom);

        validateTextArc();
        validateTextPath();
        float cos1 = (float) (Math.cos(getStartAngle() * Math.PI / 180));
        float cos2 = (float) (Math.cos((getStartAngle() + getSweepAngle()) * Math.PI / 180));
        float sin1 = (float) (Math.sin(getStartAngle() * Math.PI / 180));
        float sin2 = (float) (Math.sin((getStartAngle() + getSweepAngle()) * Math.PI / 180));

        // line1
        mLines[0] = center_x + cos1 * getInnerRadius();
        mLines[1] = center_y + sin1 * getInnerRadius();
        mLines[2] = center_x + cos1 * getOuterRadius();
        mLines[3] = center_y + sin1 * getOuterRadius();
        // line 2
        mLines[4] = center_x + cos2 * getInnerRadius();
        mLines[5] = center_y + sin2 * getInnerRadius();
        mLines[6] = center_x + cos2 * getOuterRadius();
        mLines[7] = center_y + sin2 * getOuterRadius();

        Log.d(TAG, "startAngle:" + getStartAngle());
        Log.d(TAG, "sweepAngle:" + getSweepAngle());
    }

    private void validateTextArc() {
        setTextRadius((getOuterRadius() + getInnerRadius()) / 2);
        float left = mCenterX - getTextRadius();
        float right = mCenterX + getTextRadius();
        float top = mCenterY - getTextRadius();
        float bottom = mCenterY + getTextRadius();
        mTextArc.set(left, top, right, bottom);
    }

    private void validateTextPath() {
        mTextPath.reset();
        if (getSweepAngle() >= 360) {
            float textAngle = (float) (mTextWidth * 90 / Math.PI / getTextRadius());
            mTextPath.addArc(mTextArc, getStartAngle() + 180 - textAngle, textAngle * 2);
        } else {
            mTextPath.addArc(mTextArc, (float) (getStartAngle() + getSweepAngle() / 2 -
                    mTextWidth * 90 / Math.PI / getTextRadius()), getSweepAngle());
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!changed) return;
        Log.d(TAG, "onLayout");
        changeLayout((l + r) / 2f, (t + b) / 2f, r - l - getPaddingLeft() - getPaddingRight(),
                b - t - getPaddingTop() - getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw arc
        canvas.drawArc(mTextArc, getStartAngle(), getSweepAngle(), false, mOuterPaint);
//        canvas.drawArc(mInnerArc, getStartAngle(), getSweepAngle(), true, mInnerPaint);

        // draw lines
        canvas.drawArc(mOuterArc, getStartAngle(), getSweepAngle(), false, mLinePaint);
        canvas.drawArc(mInnerArc, getStartAngle(), getSweepAngle(), false, mLinePaint);
        if (getSweepAngle() < 360) canvas.drawLines(mLines, mLinePaint);

        // draw text
        canvas.drawTextOnPath(getText(), mTextPath, 0, mTextHeight, mTextPaint);
    }

    /**
     * Gets the text attribute value.
     *
     * @return The text attribute value.
     */
    public String getText() {
        return mArcText;
    }

    /**
     * Sets the view's text attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param text The text attribute value to use.
     */
    public void setText(String text) {
        mArcText = text;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the text color attribute value.
     *
     * @return The text color attribute value.
     */
    public int getTextColor() {
        return mTextPaint.getColor();
    }

    /**
     * Sets the view's text color attribute value. In the example view, this color
     * is the font color.
     *
     * @param color The text color attribute value to use.
     */
    public void setTextColor(int color) {
        mTextPaint.setColor(color);
    }

    /**
     * Gets the text size attribute value.
     *
     * @return The text size attribute value.
     */
    public float getTextSize() {
        return mTextPaint.getTextSize();
    }

    /**
     * Sets the view's text size attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param textSize The text size attribute value to use.
     */
    public void setTextSize(float textSize) {
        mTextPaint.setTextSize(textSize);
        invalidateTextPaintAndMeasurements();
    }

    public float getSweepAngle() {
        return mSweepAngle;
    }

    /**
     * set sweep angle
     *
     * @param sweepAngle [0,360] ,negative angle ((-360 0) ) is regard as angle+360,
     *                   angle >360 or angle <=-360 is regard as 360
     */
    public void setSweepAngle(float sweepAngle) {
        if (sweepAngle > 360 && sweepAngle <= -360) {
            mSweepAngle = 360;
        } else if (sweepAngle < 0) {
            mSweepAngle = sweepAngle + 360;
        } else {
            mSweepAngle = sweepAngle;
        }
    }

    public float getStartAngle() {
        return mStartAngle;
    }

    /**
     * <p>If the start angle is negative or >= 360, the start angle is treated
     * as start angle modulo 360.</p>
     *
     * @param startAngle angle
     */
    public void setStartAngle(float startAngle) {
        this.mStartAngle = restrict0to360(startAngle);
    }

    public void setPressedColor(int color) {
        mArcPressedColor = color;
    }

    public int getPressedColor() {
        return mArcPressedColor;
    }

    public void setArcColor(int color) {
        mArcColor = color;
    }

    public int getArcColor() {
        return mArcColor;
    }

    public int getInnerColor() {
        return mInnerColor;
    }

    public void setInnerColor(int mInnerColor) {
        this.mInnerColor = mInnerColor;
    }

    public float getOuterRadius() {
        return mOuterRadius;
    }

    public void setOuterRadius(float radius) {
        this.mOuterRadius = radius;
    }

    public float getInnerRadiusRatio() {
        return mInnerRadiusRatio;
    }

    public void setInnerRadiusRatio(float ratio) {
        this.mInnerRadiusRatio = ratio;
    }

    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    public void setStrokeWidth(float mStrokeWidth) {
        this.mStrokeWidth = mStrokeWidth;
    }

    public int getStrokeColor() {
        return mStrokeColor;
    }

    public void setStrokeColor(int mStrokeColor) {
        this.mStrokeColor = mStrokeColor;
    }

    public float getTextRadius() {
        return mTextRadius;
    }

    public void setTextRadius(float mTextRadius) {
        this.mTextRadius = mTextRadius;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isInside(event.getX(), event.getY())) {
                    mIsInside = true;
                    mOuterPaint.setColor(getPressedColor());
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isInside(event.getX(), event.getY()) && mIsInside) {
                    mIsInside = false;
                    mOuterPaint.setColor(getArcColor());
                    invalidate();
                    return false;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mIsInside) {
                    mIsInside = false;
                    mOuterPaint.setColor(getArcColor());
                    invalidate();
                    return false;
                }
                break;
        }

        return false;
    }

    /**
     * get angle in degree
     *
     * @param x x position
     * @param y y position
     * @return angle according to center from positive x axis
     */
    private float getAngle(float x, float y) {
        float ax = x - mCenterX;
        float ay = y - mCenterY;
        float sr = (float) Math.sqrt(ax * ax + ay * ay);
        if (sr < 0.00000001D) {
            return 0;
        }

        float angle = (float) (Math.acos(ax / sr) * 180 / Math.PI);
        return ay < 0 ? 360 - angle : angle;
    }

    private static float restrict0to360(float angle) {
        return (float) (angle - 360 * Math.floor(angle / 360));
    }

    public boolean isInside(float x, float y) {
        float radius = getRadius(x, y);
        if (radius >= getInnerRadius() && radius <= getOuterRadius()) {
            if (getSweepAngle() == 360) return true;
            float angle = getAngle(x, y);
            if (getStartAngle() + getSweepAngle() > 360) {
                return angle >= getStartAngle() && angle <= getSweepAngle() + getStartAngle() ||
                        angle + 360 >= getStartAngle() && angle + 360 <= getSweepAngle() + getStartAngle();
            }
            return angle >= getStartAngle() && angle <= getSweepAngle() + getStartAngle();
        }
        return false;
    }

    private float getRadius(float x, float y) {
        return (float) Math.sqrt((x - mCenterX) * (x - mCenterX) + (y - mCenterY) * (y - mCenterY));
    }

    public float getMaxRadius() {
        return mMaxRadius;
    }

    public void setMaxRadius(float radius) {
        this.mMaxRadius = radius;
    }

    public float getInnerRadius() {
        return mInnerRadius;
    }

    public void setInnerRadius(float innerRadius) {
        this.mInnerRadius = innerRadius;
    }
}
