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

/** Material 3–ish emphasized curve. Not magic — just vibes with math. */
val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

private const val NavMs = 380
private const val PageMs = 340

/** Clock → Settings (forward). */
fun remarkableEnterForward(): EnterTransition =
    fadeIn(tween(NavMs, easing = EmphasizedDecelerate)) +
        slideInHorizontally(tween(NavMs, easing = EmphasizedDecelerate)) { it / 5 }

fun remarkableExitForward(): ExitTransition =
    fadeOut(tween(NavMs / 2, easing = EmphasizedAccelerate)) +
        slideOutHorizontally(tween(NavMs, easing = EmphasizedAccelerate)) { -it / 12 }

/** Settings → Clock (back). */
fun remarkableEnterPop(): EnterTransition =
    fadeIn(tween(NavMs, easing = EmphasizedDecelerate)) +
        slideInHorizontally(tween(NavMs, easing = EmphasizedDecelerate)) { -it / 12 }

fun remarkableExitPop(): ExitTransition =
    fadeOut(tween(NavMs / 2, easing = EmphasizedAccelerate)) +
        slideOutHorizontally(tween(NavMs, easing = EmphasizedAccelerate)) { it / 5 }

/** Onboarding → clock: soft reveal, no door slam. */
fun remarkableEnterFadeScale(): EnterTransition =
    fadeIn(tween(420, easing = EmphasizedDecelerate)) +
        scaleIn(
            animationSpec = tween(420, easing = EmphasizedDecelerate),
            initialScale = 0.96f,
        )

fun remarkableExitFadeScale(): ExitTransition =
    fadeOut(tween(220, easing = EmphasizedAccelerate)) +
        scaleOut(
            animationSpec = tween(220, easing = EmphasizedAccelerate),
            targetScale = 1.02f,
        )

/** Horizontal pager steps inside onboarding. */
fun onboardingPageTransform(forward: Boolean): ContentTransform {
    val enter = fadeIn(tween(PageMs, easing = EmphasizedDecelerate)) +
        slideInHorizontally(tween(PageMs, easing = EmphasizedDecelerate)) {
            if (forward) it / 4 else -it / 4
        }
    val exit = fadeOut(tween(PageMs / 2, easing = EmphasizedAccelerate)) +
        slideOutHorizontally(tween(PageMs, easing = EmphasizedAccelerate)) {
            if (forward) -it / 6 else it / 6
        }
    return enter togetherWith exit
}

/** Tiny spring for chips / dots — because linear is for spreadsheets. */
fun <T> snappySpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

fun sheetEnter(): EnterTransition =
    fadeIn(tween(280, easing = EmphasizedDecelerate)) +
        slideInVertically(tween(320, easing = EmphasizedDecelerate)) { it / 10 }

fun sheetExit(): ExitTransition =
    fadeOut(tween(180, easing = EmphasizedAccelerate)) +
        slideOutVertically(tween(220, easing = EmphasizedAccelerate)) { it / 14 }
