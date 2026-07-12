package com.peek.browser.util

import android.view.animation.Animation
import android.view.animation.Transformation

/**
 * An animation that controls the position and scale of an object. See the
 * android.view.animation full package description for details and sample code.
 *
 * Extended to added a TransformationListener
 */
class TranslateAnimationEx(
    fromXDelta: Float,
    toXDelta: Float,
    fromYDelta: Float,
    toYDelta: Float,
    private val mTransformationListener: TransformationListener?
) : Animation() {
    private val mFromXType = ABSOLUTE
    private val mToXType = ABSOLUTE
    private val mFromYType = ABSOLUTE
    private val mToYType = ABSOLUTE

    private val mFromXValue = fromXDelta
    private val mToXValue = toXDelta
    private val mFromYValue = fromYDelta
    private val mToYValue = toYDelta

    private var mFromXDelta = 0f
    private var mToXDelta = 0f
    private var mFromYDelta = 0f
    private var mToYDelta = 0f

    interface TransformationListener {
        fun onApplyTransform(interpolatedTime: Float, t: Transformation, dx: Float, dy: Float)
    }

    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        var dx = mFromXDelta
        var dy = mFromYDelta
        if (mFromXDelta != mToXDelta) {
            dx = mFromXDelta + ((mToXDelta - mFromXDelta) * interpolatedTime)
        }
        if (mFromYDelta != mToYDelta) {
            dy = mFromYDelta + ((mToYDelta - mFromYDelta) * interpolatedTime)
        }
        t.matrix.setTranslate(dx, dy)

        mTransformationListener?.onApplyTransform(interpolatedTime, t, dx, dy)
    }

    override fun initialize(width: Int, height: Int, parentWidth: Int, parentHeight: Int) {
        super.initialize(width, height, parentWidth, parentHeight)
        mFromXDelta = resolveSize(mFromXType, mFromXValue, width, parentWidth)
        mToXDelta = resolveSize(mToXType, mToXValue, width, parentWidth)
        mFromYDelta = resolveSize(mFromYType, mFromYValue, height, parentHeight)
        mToYDelta = resolveSize(mToYType, mToYValue, height, parentHeight)
    }
}
