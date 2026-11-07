/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.utils

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex

/**
 * Book-page turn for a HorizontalPager page: the page being left hinges on its left edge and
 * swings away like a page of a book, while the next page waits underneath.
 */
fun Modifier.bookPageFlip(
    pagerState: PagerState,
    page: Int,
    pageSpacing: Dp,
): Modifier =
    this
        .zIndex(-page.toFloat())
        .graphicsLayer {
            val offset = pagerState.currentPage + pagerState.currentPageOffsetFraction - page
            if (offset <= -1f || offset >= 1f) {
                alpha = 0f
            } else {
                // keep the page pinned in place (the pager would otherwise slide it sideways)
                translationX = offset * (size.width + pageSpacing.toPx())
                cameraDistance = 14f * density
                if (offset > 0f) {
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    // smooth-step: starts and ends gently, and at offset 1 the page is edge-on (invisible),
                    // so the turn keeps animating until the page has completely gone - no sudden pop.
                    val eased = offset * offset * (3f - 2f * offset)
                    rotationY = -eased * 90f
                } else {
                    val o = -offset
                    val s = 0.92f + 0.08f * (1f - o)
                    scaleX = s
                    scaleY = s
                }
            }
        }
