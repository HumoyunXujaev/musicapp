package com.humoyun.musicapp.utils

import android.view.View
import android.view.animation.OvershootInterpolator

fun View.bounceClick(onEnd: () -> Unit = {}) {
    this.animate()
        .scaleX(0.8f)
        .scaleY(0.8f)
        .setDuration(100)
        .setInterpolator(OvershootInterpolator())
        .withEndAction {
            this.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(100)
                .setInterpolator(OvershootInterpolator())
                .withEndAction {
                    onEnd()
                }
                .start()
        }
        .start()
}