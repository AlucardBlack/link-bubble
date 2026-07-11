/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.physics

import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import com.linkbubble.MainController
import com.linkbubble.util.CrashTracking
import com.linkbubble.util.Util

class DraggableHelper(
        private val mView: View,
        private val mWindowManagerParams: WindowManager.LayoutParams,
        setOnTouchListener: Boolean,
        private val mOnTouchActionEventListener: OnTouchActionEventListener?
) {

    enum class AnimationType {
        Linear,
        SmallOvershoot,
        MediumOvershoot,
        LargeOvershoot,
        DistanceProportion
    }

    interface OnTouchActionEventListener {
        fun onActionDown(event: TouchEvent)
        fun onActionMove(event: MoveEvent)
        fun onActionUp(event: ReleaseEvent)
    }

    class TouchEvent {
        @JvmField var posX = 0
        @JvmField var posY = 0
        @JvmField var rawX = 0f
        @JvmField var rawY = 0f
    }

    class MoveEvent {
        @JvmField var dx = 0
        @JvmField var dy = 0
        @JvmField var rawX = 0f
        @JvmField var rawY = 0f
    }

    class ReleaseEvent {
        @JvmField var posX = 0
        @JvmField var posY = 0
        @JvmField var vx = 0f
        @JvmField var vy = 0f
        @JvmField var rawX = 0f
        @JvmField var rawY = 0f
    }

    private class InternalMoveEvent(var mX: Float, var mY: Float, var mTime: Long)

    // Reusable events
    val mTouchEvent = TouchEvent()
    val mMoveEvent = MoveEvent()
    val mReleaseEvent = ReleaseEvent()

    private var mAlive: Boolean

    // Move animation state
    private var mInitialX = 0
    private var mInitialY = 0
    private var mTargetX = 0
    private var mTargetY = 0
    private var mAnimPeriod = 0f
    private var mAnimTime = 0f
    private var mAnimType: AnimationType? = null
    private val mLinearInterpolator = LinearInterpolator()
    private val mOvershootInterpolatorSmall = OvershootInterpolator(0.5f)
    private val mOvershootInterpolatorMedium = OvershootInterpolator(1.5f)
    private val mOvershootInterpolatorLarge = OvershootInterpolator(2.0f)

    private val mStartTouchRaw: InternalMoveEvent
    private val mEndTouchRaw: InternalMoveEvent

    private var mFlingTracker: FlingTracker? = null
    private var mStartTouchX = -1
    private var mStartTouchY = -1
    private var mAnimationListener: AnimationEventListener? = null

    interface AnimationEventListener {
        fun onAnimationComplete()
        fun onCancel()
    }

    private val mOnTouchListener = View.OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                onTouchActionDown(event)
            }
            MotionEvent.ACTION_MOVE -> {
                onTouchActionMove(event)
            }
            MotionEvent.ACTION_UP -> {
                onTouchActionUp(event)
            }
            //case MotionEvent.ACTION_CANCEL: {
            //    return true;
            //}
            else -> false
        }
    }

    init {
        mAlive = true

        mStartTouchRaw = InternalMoveEvent(0f, 0f, 0)
        mEndTouchRaw = InternalMoveEvent(0f, 0f, 0)


        if (setOnTouchListener) {
            mView.setOnTouchListener(mOnTouchListener)
        }
    }

    private fun addMoveEvent(x: Float, y: Float, t: Long) {
        if (mStartTouchRaw.mTime == 0L) {
            mStartTouchRaw.mTime = t
            mStartTouchRaw.mX = x
            mStartTouchRaw.mY = y
        }
        mEndTouchRaw.mTime = t
        mEndTouchRaw.mX = x
        mEndTouchRaw.mY = y
    }

    fun onTouchActionDown(event: MotionEvent): Boolean {
        mTouchEvent.posX = mWindowManagerParams.x
        mTouchEvent.posY = mWindowManagerParams.y
        mTouchEvent.rawX = event.rawX
        mTouchEvent.rawY = event.rawY

        addMoveEvent(event.rawX, event.rawY, event.eventTime)

        mOnTouchActionEventListener?.onActionDown(mTouchEvent)

        mStartTouchX = mWindowManagerParams.x
        mStartTouchY = mWindowManagerParams.y

        val flingTracker = FlingTracker.obtain()
        mFlingTracker = flingTracker
        flingTracker.addMovement(event)

        return true
    }

    fun onTouchActionMove(event: MotionEvent): Boolean {
        if (mStartTouchX == -1 && mStartTouchY == -1) {
            onTouchActionDown(event)
        }

        val touchXRaw = event.rawX
        val touchYRaw = event.rawY

        val deltaX = (touchXRaw - mStartTouchRaw.mX).toInt()
        val deltaY = (touchYRaw - mStartTouchRaw.mY).toInt()

        addMoveEvent(touchXRaw, touchYRaw, event.eventTime)

        mMoveEvent.dx = deltaX
        mMoveEvent.dy = deltaY
        mMoveEvent.rawX = touchXRaw
        mMoveEvent.rawY = touchYRaw
        mOnTouchActionEventListener?.onActionMove(mMoveEvent)

        event.offsetLocation((mWindowManagerParams.x - mStartTouchX).toFloat(), (mWindowManagerParams.y - mStartTouchY).toFloat())
        mFlingTracker!!.addMovement(event)

        return true
    }

    fun hasAtLeast2TouchEvents(): Boolean {
        return mStartTouchRaw.mTime != 0L && mEndTouchRaw.mTime != 0L && mEndTouchRaw.mTime != mStartTouchRaw.mTime
    }

    fun onTouchActionUp(event: MotionEvent): Boolean {
        mReleaseEvent.posX = mWindowManagerParams.x
        mReleaseEvent.posY = mWindowManagerParams.y
        mReleaseEvent.vx = 0.0f
        mReleaseEvent.vy = 0.0f
        mReleaseEvent.rawX = event.rawX
        mReleaseEvent.rawY = event.rawY

        if (hasAtLeast2TouchEvents()) {
            val touchTime = (mEndTouchRaw.mTime - mStartTouchRaw.mTime) / 1000.0f
            mReleaseEvent.vx = (mEndTouchRaw.mX - mStartTouchRaw.mX) / touchTime
            mReleaseEvent.vy = (mEndTouchRaw.mY - mStartTouchRaw.mY) / touchTime
        }

        // *Should* always be true, but under certain circumstances, is not. #384
        val flingTracker = mFlingTracker
        if (flingTracker != null) {
            flingTracker.computeCurrentVelocity(1000)
            val fvx = flingTracker.getXVelocity()
            val fvy = flingTracker.getYVelocity()

            mReleaseEvent.vx = fvx
            mReleaseEvent.vy = fvy

            flingTracker.recycle()
        }

        mOnTouchActionEventListener?.onActionUp(mReleaseEvent)

        mStartTouchX = -1
        mStartTouchY = -1
        mStartTouchRaw.mTime = 0
        mEndTouchRaw.mTime = 0
        return true
    }

    fun cancelAnimation() {
        val listener = mAnimationListener
        mAnimationListener = null

        clearTargetPos()

        listener?.onCancel()
    }

    fun getAnimCompleteFraction(): Float {
        var f = 1.0f

        if (mAnimPeriod > 0.0f) {
            f = Util.clamp(0.0f, mAnimTime / mAnimPeriod, 1.0f)
        }

        return f
    }

    fun clearTargetPos() {
        // TODO: This probably fires. It can be disabled temporarily if a pain, but should be fixed.
        Util.Assert(mAnimationListener == null, "non-null mAnimationListener")

        mInitialX = -1
        mInitialY = -1

        mTargetX = mWindowManagerParams.x
        mTargetY = mWindowManagerParams.y

        mAnimPeriod = 0.0f
        mAnimTime = 0.0f
    }

    fun setExactPos(x: Int, y: Int) {
        if (mWindowManagerParams.x == x && mWindowManagerParams.y == y) {
            return
        }
        mWindowManagerParams.x = x
        mWindowManagerParams.y = y
        mTargetX = x
        mTargetY = y

        if (mAlive) {
            MainController.updateRootWindowLayout(mView, mWindowManagerParams)
        }
    }

    fun setTargetPos(x: Int, y: Int, t: Float, type: AnimationType, listener: AnimationEventListener?) {
        var xIn = x
        var yIn = y
        var tIn = t
        var typeIn = type
        try {
            Util.Assert(mAnimationListener == null, "non-null mAnimationListener")
        } catch (exc: AssertionError) {
            CrashTracking.logHandledException(exc)
        }
        mAnimationListener = listener

        if (xIn != mTargetX || yIn != mTargetY) {

            if (typeIn == AnimationType.DistanceProportion) {
                // Something > 0.016 will have a high likelihood of causing < 60fps
                val maxTime = 0.005f
                val maxDistance = 50.0f

                val d = Util.distance(xIn.toFloat(), yIn.toFloat(), mWindowManagerParams.x.toFloat(), mWindowManagerParams.y.toFloat())
                tIn = maxTime * d / maxDistance
                tIn = maxTime - Util.clamp(0.0f, tIn, maxTime)
                typeIn = AnimationType.Linear
            }

            if (tIn < 0.0001f) {
                clearTargetPos()
                setExactPos(xIn, yIn)
            } else {
                mAnimType = typeIn

                mInitialX = mWindowManagerParams.x
                mInitialY = mWindowManagerParams.y

                mTargetX = xIn
                mTargetY = yIn

                mAnimPeriod = tIn
                mAnimTime = 0.0f
            }

            MainController.get()?.scheduleUpdate()
        } else if (listener != null) {
            mAnimationListener = null
            listener.onAnimationComplete()
        }
    }

    fun getXPos(): Int {
        return mWindowManagerParams.x
    }

    fun getYPos(): Int {
        return mWindowManagerParams.y
    }

    fun getWindowManagerParams(): WindowManager.LayoutParams {
        return mWindowManagerParams
    }

    fun isAlive(): Boolean {
        return mAlive
    }

    fun getView(): View {
        return mView
    }

    fun update(dt: Float): Boolean {
        if (mAnimTime < mAnimPeriod) {
            Util.Assert(mAnimPeriod > 0.0f, "mAnimPeriod:$mAnimPeriod")

            mAnimTime = Util.clamp(0.0f, mAnimTime + dt, mAnimPeriod)

            val tf = mAnimTime / mAnimPeriod
            var interpolatedFraction = 0.0f
            when (mAnimType) {
                AnimationType.Linear ->
                    interpolatedFraction = mLinearInterpolator.getInterpolation(tf)
                AnimationType.SmallOvershoot ->
                    interpolatedFraction = mOvershootInterpolatorSmall.getInterpolation(tf)
                AnimationType.MediumOvershoot ->
                    interpolatedFraction = mOvershootInterpolatorMedium.getInterpolation(tf)
                AnimationType.LargeOvershoot ->
                    interpolatedFraction = mOvershootInterpolatorLarge.getInterpolation(tf)
                else -> {}
            }

            val x = (mInitialX + (mTargetX - mInitialX) * interpolatedFraction).toInt()
            val y = (mInitialY + (mTargetY - mInitialY) * interpolatedFraction).toInt()

            if (mWindowManagerParams.x != x || mWindowManagerParams.y != y) {
                mWindowManagerParams.x = x
                mWindowManagerParams.y = y
                MainController.updateRootWindowLayout(mView, mWindowManagerParams)
            }

            MainController.get()!!.scheduleUpdate()

            if (mAnimTime >= mAnimPeriod) {
                mAnimTime = 0.0f
                mAnimPeriod = 0.0f
                val l = mAnimationListener
                if (l != null) {
                    mAnimationListener = null
                    l.onAnimationComplete()
                }
            }

            return true
        }

        return false
    }

    fun destroy() {
        MainController.removeRootWindow(mView)
        mAlive = false
    }
}
