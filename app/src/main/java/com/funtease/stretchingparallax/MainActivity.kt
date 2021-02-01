package com.funtease.stretchingparallax

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View.OnTouchListener
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private var vOffset : Int = 0
    private var handler =  Handler()
    private var expandHeight : Int = 0
    @SuppressLint("ClickableViewAccessibility")
    var touchListener = OnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            handler.postDelayed(Runnable {
                if(vOffset >= expandHeight)
                    setStandardToolbar(vOffset)
            }, 100)
        }
        if (event.action == MotionEvent.ACTION_DOWN) {
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpToolBar()
    }

    private fun setUpToolBar() {
        cdlList.setOnTouchListener(touchListener)
        content_view.setOnTouchListener(touchListener)
        appbar.addOnOffsetChangedListener(OnOffsetChangedListener { _, verticalOffset ->
            vOffset = verticalOffset
            adjustImage(vOffset)
            Log.e("PARALLAX", "OFFSET: $vOffset")
            expandHeight = -appbar.totalScrollRange + 450
            Log.e("PARALLAX", "expandHeight $expandHeight");
        })
        setStandardToolbar(vOffset)



    }

    private fun setStandardToolbar(position: Int) {

        val params = appbar.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as AppBarLayout.Behavior?
        if (behavior != null) {
            val valueAnimator = ValueAnimator.ofInt()
            valueAnimator.interpolator = DecelerateInterpolator()
            valueAnimator.addUpdateListener { animation ->
                behavior.topAndBottomOffset = (animation.animatedValue as Int)
                appbar.requestLayout()
                adjustImage((animation.animatedValue as Int))
            }
            valueAnimator.addListener(object : AnimatorListenerAdapter() {
            })
            valueAnimator.setIntValues(position, expandHeight)
            valueAnimator.duration = 350
            valueAnimator.start()
        }
    }


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