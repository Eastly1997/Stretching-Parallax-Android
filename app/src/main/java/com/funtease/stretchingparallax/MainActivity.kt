package com.funtease.stretchingparallax

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private var vOffset : Int = 0
    private var handler =  Handler()
    private var expandHeight : Int = 0
    private val valueAnimator: ValueAnimator = ValueAnimator.ofInt()
    private var isTouching: Boolean = false
    private var isFade: Boolean = false

    private var fadeOut:AlphaAnimation = AlphaAnimation(1f, 0.0f)
    private var fadeIn:AlphaAnimation = AlphaAnimation(0.0f, 1f)


    @SuppressLint("ClickableViewAccessibility")
    var touchListener = OnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            isTouching = false
            handler.postDelayed({
                if (vOffset >= expandHeight)
                    setStandardToolbar(vOffset)
            }, 100)
        } else if (event.action == MotionEvent.ACTION_DOWN) {
            isTouching = true
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpToolBar()

        setUpAnimation()
    }

    private fun setUpAnimation() {
        fadeOut.duration = 300
        fadeIn.duration = 350
        fadeOut.interpolator = AccelerateInterpolator()
        fadeIn.interpolator = DecelerateInterpolator()

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                rest_details_back.setColorFilter(
                    ContextCompat.getColor(baseContext, R.color.black),
                    PorterDuff.Mode.SRC_IN
                )
                ivSearch.setColorFilter(
                    ContextCompat.getColor(baseContext, R.color.black),
                    PorterDuff.Mode.MULTIPLY
                )
                ivWishList.setColorFilter(
                    ContextCompat.getColor(baseContext, R.color.black),
                    PorterDuff.Mode.MULTIPLY
                )
            }

            override fun onAnimationEnd(animation: Animation) {
                ivWishList_background.visibility = View.INVISIBLE
                rest_details_back_background.visibility = View.INVISIBLE
                ivSearch_background.visibility = View.INVISIBLE
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                ivWishList_background.visibility = View.VISIBLE
                rest_details_back_background.visibility = View.VISIBLE
                ivSearch_background.visibility = View.VISIBLE
                rest_details_back.setColorFilter(
                    ContextCompat.getColor(baseContext, R.color.white),
                    PorterDuff.Mode.SRC_IN
                )
                ivSearch.setColorFilter(
                    ContextCompat.getColor(baseContext, R.color.white),
                    PorterDuff.Mode.SRC_IN
                )
                ivWishList.setColorFilter(
                    ContextCompat.getColor(baseContext, R.color.white),
                    PorterDuff.Mode.SRC_IN
                )
            }

            override fun onAnimationEnd(animation: Animation) {}

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    private fun setUpToolBar() {
        cdlList.setOnTouchListener(touchListener)`
        content_view.setOnTouchListener(touchListener)
        appbar.addOnOffsetChangedListener(OnOffsetChangedListener { _, verticalOffset ->
            vOffset = verticalOffset
            adjustImage(vOffset)

            if (abs(verticalOffset) == appbar.totalScrollRange) {
                // Collapsed
                tvRestaurantNameToolbar.visibility = View.VISIBLE
                if (!isFade) {
                    isFade = true;
                    ivSearch_background.startAnimation(fadeOut)
                    ivWishList_background.startAnimation(fadeOut)
                    rest_details_back_background.startAnimation(fadeOut)
                }
            } else {
                // Expanded
                if (isFade) {
                    isFade = false;
                    ivSearch_background.startAnimation(fadeIn)
                    ivWishList_background.startAnimation(fadeIn)
                    rest_details_back_background.startAnimation(fadeIn)
                }
                tvRestaurantNameToolbar.visibility = View.GONE
            }
        })

        //delaying this because for some unknown reason behavior is null
        handler.postDelayed({
            // 450 = Minimum height of the image
            expandHeight = -appbar.totalScrollRange + 450
            setStandardToolbar(0)
        }, 150)
    }

    private fun setStandardToolbar(position: Int) {
        val params = appbar.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as AppBarLayout.Behavior?
        if (behavior != null) {
            if(!valueAnimator.isRunning) {
                valueAnimator.interpolator = DecelerateInterpolator()
                valueAnimator.addUpdateListener { animation ->
                    //prevent animating when user is actively scrolling
                    if (isTouching)
                        return@addUpdateListener

                    behavior.topAndBottomOffset = (animation.animatedValue as Int)
                    appbar.requestLayout()
                }
                valueAnimator.addListener(object : AnimatorListenerAdapter() {
                })
                valueAnimator.setIntValues(position, expandHeight)
                valueAnimator.duration = 350
                valueAnimator.start()
            }
        }
    }



    //credits this method to stack overflow I forgot the link
    private fun adjustImage(verticalOffset: Int) {
        val matrix = Matrix(ivBanner.imageMatrix)

        //get image's width and height
        if (ivBanner.drawable == null) return
        val dWidth = ivBanner.drawable.intrinsicWidth
        val dHeight = ivBanner.drawable.intrinsicHeight

        //get view's width and height
        val vWidth = ivBanner.width - ivBanner.paddingLeft - ivBanner.paddingRight
        var vHeight = ivBanner.height - ivBanner.paddingTop - ivBanner.paddingBottom
        val scale: Float
        var dx = 0f
        val dy: Float
        val parallaxMultiplier = (ivBanner.layoutParams as CollapsingToolbarLayout.LayoutParams).parallaxMultiplier

        //maintain the image's aspect ratio depending on offset
        if (dWidth * vHeight > vWidth * dHeight) {
            vHeight += verticalOffset //calculate view height depending on offset
            scale = vHeight.toFloat() / dHeight.toFloat() //calculate scale
            dx =
                (vWidth - dWidth * scale) * 0.5f //calculate x value of the center point of scaled drawable
            dy =
                -verticalOffset * (1 - parallaxMultiplier) //calculate y value by compensating parallaxMultiplier
        } else {
            scale = vWidth.toFloat() / dWidth.toFloat()
            dy = (vHeight - dHeight * scale) * 0.5f
        }
        val currentWidth =
            (scale * dWidth).roundToInt() //calculate current intrinsic width of the drawable
        if (vWidth <= currentWidth) { //compare view width and drawable width to decide, should we scale more or not
            matrix.setScale(scale, scale)
            matrix.postTranslate(dx.roundToInt().toFloat(), dy.roundToInt().toFloat())
            ivBanner.imageMatrix = matrix
        }
    }
}