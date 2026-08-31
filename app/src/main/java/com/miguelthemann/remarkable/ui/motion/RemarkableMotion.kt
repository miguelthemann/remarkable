/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

/** Material 3 emphasized curve. Not magic — just vibes with math. */
val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

private const val NavMs = 480
private const val PageMs = 340

/** Clock → Settings: obvious slide from the right. */
fun remarkableEnterForward(): EnterTransition =
    fadeIn(animationSpec = tween(NavMs, easing = EmphasizedDecelerate)) +
        slideInHorizontally(animationSpec = tween(NavMs, easing = EmphasizedDecelerate)) { full ->
            (full * 0.35f).toInt().coerceAtLeast(120)
        }

fun remarkableExitForward(): ExitTransition =
    fadeOut(animationSpec = tween(NavMs, easing = EmphasizedAccelerate)) +
        slideOutHorizontally(animationSpec = tween(NavMs, easing = EmphasizedAccelerate)) { full ->
            -(full * 0.12f).toInt().coerceAtLeast(48)
        }

/** Settings → Clock. */
fun remarkableEnterPop(): EnterTransition =
    fadeIn(animationSpec = tween(NavMs, easing = EmphasizedDecelerate)) +
        slideInHorizontally(animationSpec = tween(NavMs, easing = EmphasizedDecelerate)) { full ->
            -(full * 0.12f).toInt().coerceAtLeast(48)
        }

fun remarkableExitPop(): ExitTransition =
    fadeOut(animationSpec = tween(NavMs, easing = EmphasizedAccelerate)) +
        slideOutHorizontally(animationSpec = tween(NavMs, easing = EmphasizedAccelerate)) { full ->
            (full * 0.35f).toInt().coerceAtLeast(120)
        }

fun remarkableEnterFadeScale(): EnterTransition =
    fadeIn(animationSpec = tween(450, easing = EmphasizedDecelerate)) +
        scaleIn(
            animationSpec = tween(450, easing = EmphasizedDecelerate),
            initialScale = 0.92f,
        )

fun remarkableExitFadeScale(): ExitTransition =
    fadeOut(animationSpec = tween(280, easing = EmphasizedAccelerate)) +
        scaleOut(
            animationSpec = tween(280, easing = EmphasizedAccelerate),
            targetScale = 1.04f,
        )

fun onboardingPageTransform(forward: Boolean): ContentTransform {
    val enter = fadeIn(tween(PageMs, easing = EmphasizedDecelerate)) +
        slideInHorizontally(tween(PageMs, easing = EmphasizedDecelerate)) {
            if (forward) it / 3 else -it / 3
        }
    val exit = fadeOut(tween(PageMs / 2, easing = EmphasizedAccelerate)) +
        slideOutHorizontally(tween(PageMs, easing = EmphasizedAccelerate)) {
            if (forward) -it / 5 else it / 5
        }
    return enter togetherWith exit
}

fun <T> snappySpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

fun sheetEnter(): EnterTransition =
    fadeIn(tween(280, easing = EmphasizedDecelerate)) +
        slideInVertically(tween(320, easing = EmphasizedDecelerate)) { it / 8 }

fun sheetExit(): ExitTransition =
    fadeOut(tween(180, easing = EmphasizedAccelerate)) +
        slideOutVertically(tween(220, easing = EmphasizedAccelerate)) { it / 10 }
