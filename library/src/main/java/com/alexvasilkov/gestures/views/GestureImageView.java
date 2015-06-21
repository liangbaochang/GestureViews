package com.alexvasilkov.gestures.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.alexvasilkov.gestures.GesturesController;
import com.alexvasilkov.gestures.GesturesControllerForPager;
import com.alexvasilkov.gestures.State;
import com.alexvasilkov.gestures.utils.Snapshot;

/**
 * Gestures controlled ImageView
 */
public class GestureImageView extends ImageView
        implements GesturesController.OnStateChangedListener {

    private final GesturesControllerForPager mController;
    private final Matrix mImageMatrix = new Matrix();
    private OnSnapshotLoadedListener mSnapshotListener;

    private final RectF mClipRect = new RectF(), mOldClipRect = new RectF();
    private boolean mIsClipping;

    public GestureImageView(Context context) {
        this(context, null, 0);
    }

    public GestureImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mController = new GesturesControllerForPager(context, this);

        setScaleType(ImageView.ScaleType.MATRIX);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mIsClipping) {
            canvas.save();
            canvas.clipRect(mClipRect);
        }

        super.draw(canvas);

        if (mIsClipping) {
            canvas.restore();
        }

        if (mSnapshotListener != null) {
            Snapshot snapshot = new Snapshot(mController.getSettings());
            super.draw(snapshot.getCanvas());
            mSnapshotListener.onSnapshotLoaded(snapshot.getBitmap());
            mSnapshotListener = null;
        }
    }

    /**
     * Enables scroll inside {@link android.support.v4.view.ViewPager}.
     */
    public void enableScrollInViewPager(ViewPager pager) {
        mController.enableScrollInViewPager(pager);
    }

    /**
     * Returns {@link com.alexvasilkov.gestures.GesturesController}
     * which is a main engine for {@link GestureImageView}.
     * <p/>
     * Use it to apply settings, modify image state and so on.
     */
    public GesturesController getController() {
        return mController;
    }

    public void getSnapshot(OnSnapshotLoadedListener listener) {
        mSnapshotListener = listener;
        invalidate();
    }

    /**
     * Clips layout so only part specified in {@code rect} will be drawn.
     * <p/>
     * Pass {@code null} to turn clipping off.
     */
    public void clipLayout(RectF rect) {
        if (rect == null) {
            mIsClipping = false;
            invalidate();
        } else {
            int w = getWidth(), h = getHeight();

            // Setting previous clip rect
            if (mIsClipping) {
                mOldClipRect.set(mClipRect);
            } else {
                mOldClipRect.set(0, 0, w, h);
            }

            mIsClipping = true;

            mClipRect.set(rect);

            // Invalidating only updated part
            int left = (int) Math.min(mClipRect.left, mOldClipRect.left);
            int top = (int) Math.min(mClipRect.top, mOldClipRect.top);
            int right = (int) Math.max(mClipRect.right, mOldClipRect.right) + 1;
            int bottom = (int) Math.max(mClipRect.bottom, mOldClipRect.bottom) + 1;
            invalidate(left, top, right, bottom);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return mController.onTouch(this, event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mController.getSettings().setViewport(w - getPaddingLeft() - getPaddingRight(),
                h - getPaddingTop() - getPaddingBottom());
        mController.updateState();
    }

    @Override
    public void setImageResource(int resId) {
        setImageDrawable(getDrawable(getContext(), resId));
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if (drawable == null) {
            mController.getSettings().setImage(0, 0);
        } else {
            mController.getSettings().setImage(
                    drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
        mController.resetState();
    }

    @Override
    public void onStateChanged(State state) {
        state.get(mImageMatrix);
        setImageMatrix(mImageMatrix);
    }

    @Override
    public void onStateReset(State oldState, State newState) {
        // No-op
    }


    @SuppressWarnings("deprecation")
    public static Drawable getDrawable(Context context, @DrawableRes int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getDrawable(id);
        } else {
            return context.getResources().getDrawable(id);
        }
    }


    public interface OnSnapshotLoadedListener {
        void onSnapshotLoaded(Bitmap bitmap);
    }

}
