/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.ui.motion.onboardingPageTransform
import com.miguelthemann.remarkable.ui.motion.snappySpring

private const val PageCount = 3

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    var previousPage by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        previousPage = page
        page = 2
    }

    fun goTo(next: Int) {
        previousPage = page
        page = next.coerceIn(0, PageCount - 1)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            PageIndicators(
                count = PageCount,
                selected = page,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 20.dp),
            )

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    onboardingPageTransform(forward = targetState >= previousPage)
                        .using(SizeTransform(clip = false))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboardingPages",
            ) { current ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (current) {
                        0 -> OnboardingPage(
                            icon = Icons.Outlined.WbSunny,
                            title = stringResource(R.string.onboarding_welcome_title),
                            body = stringResource(R.string.onboarding_welcome_body),
                        ) {
                            Button(onClick = { goTo(1) }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.onboarding_next))
                            }
                        }
                        1 -> OnboardingPage(
                            icon = Icons.Outlined.Security,
                            title = stringResource(R.string.onboarding_permissions_title),
                            body = stringResource(R.string.onboarding_permissions_body),
                        ) {
                            Button(
                                onClick = {
                                    val perms = buildList {
                                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        if (Build.VERSION.SDK_INT >= 33) {
                                            add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }.toTypedArray()
                                    permissionLauncher.launch(perms)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.onboarding_grant))
                            }
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = { goTo(2) }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.onboarding_skip))
                            }
                        }
                        else -> OnboardingPage(
                            icon = Icons.Outlined.TouchApp,
                            title = stringResource(R.string.onboarding_howto_title),
                            body = stringResource(R.string.onboarding_howto_body),
                        ) {
                            Button(onClick = onFinished, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.onboarding_start))
                            }
                            Spacer(Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = { goTo(1) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.onboarding_back))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    icon: ImageVector,
    title: String,
    body: String,
    actions: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
    Spacer(Modifier.height(28.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(14.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(36.dp))
    actions()
}

@Composable
private fun PageIndicators(
    count: Int,
    selected: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == selected
            val width by animateFloatAsState(
                targetValue = if (active) 28f else 8f,
                animationSpec = snappySpring(),
                label = "dotWidth",
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}
