/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.time

/** Night window shared by backgrounds, widgets, and weather icons. */
fun isNightHour(hour: Int): Boolean = hour >= 20 || hour < 5

fun isNightHour(hour: Float): Boolean = isNightHour(hour.toInt())
