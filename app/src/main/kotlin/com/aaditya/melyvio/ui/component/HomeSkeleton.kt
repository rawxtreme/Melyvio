/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.aaditya.melyvio.ui.component.shimmer.ShimmerHost

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
) {
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.onSurface))
}

/** Shimmering placeholder shown on first launch while the home feed is still loading. */
@Composable
fun HomeSkeleton() {
    ShimmerHost(showGradient = false) {
        SkeletonBlock(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(190.dp),
            shape = RoundedCornerShape(28.dp),
        )
        Spacer(Modifier.height(12.dp))
        SkeletonBlock(
            modifier = Modifier.padding(horizontal = 16.dp).width(150.dp).height(26.dp),
            shape = RoundedCornerShape(8.dp),
        )
        Spacer(Modifier.height(10.dp))
        repeat(3) {
            Row(Modifier.padding(horizontal = 12.dp)) {
                repeat(3) {
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(4.dp)) {
                        SkeletonBlock(Modifier.fillMaxSize())
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        SkeletonBlock(
            modifier = Modifier.padding(horizontal = 16.dp).width(210.dp).height(26.dp),
            shape = RoundedCornerShape(8.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 16.dp)) {
            repeat(2) {
                Box(Modifier.weight(1f).aspectRatio(1f).padding(end = 12.dp)) {
                    SkeletonBlock(Modifier.fillMaxSize())
                }
            }
        }
    }
}
