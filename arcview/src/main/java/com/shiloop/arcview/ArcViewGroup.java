package com.shiloop.arcview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * arc view group
 */
public class ArcViewGroup extends ViewGroup {

    private static final String TAG = ArcViewGroup.class.getSimpleName();
    private ArrayList<ArcView> mArcViews;

    public ArcViewGroup(Context context) {
        super(context);
        init(null, 0);
    }

    public ArcViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ArcViewGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d(TAG, "onLayout");
        mArcViews.clear();
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view.getVisibility() == GONE) continue;
            if (view instanceof ArcView) {
                mArcViews.add((ArcView) view);
            }
        }

        float mSweepAngle;
        float angle;
        if (mArcViews.size() >= 1) {
            mSweepAngle = 360 / mArcViews.size();
            angle = -90 - mSweepAngle / 2;
            for (ArcView view : mArcViews) {
                view.setStartAngle(angle);
                view.setSweepAngle(mSweepAngle);
                angle += mSweepAngle;
                view.layout(getPaddingLeft(), getPaddingTop(),
                        r - l - getPaddingRight(), b - t - getPaddingBottom());
            }
        }
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ArcViewGroup, defStyle, 0);


        a.recycle();

        mArcViews = new ArrayList<>();
    }
}
