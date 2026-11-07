/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.ui.graphics.graphicsLayer
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaditya.melyvio.LocalPlayerConnection
import com.aaditya.melyvio.R
import com.aaditya.melyvio.innertube.YouTube
import com.aaditya.melyvio.innertube.models.SongItem
import com.aaditya.melyvio.models.toMediaMetadata
import com.aaditya.melyvio.playback.queues.YouTubeQueue
import com.aaditya.melyvio.ui.utils.DayPart
import com.aaditya.melyvio.ui.utils.rememberDayPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sin

private enum class Body { SUN_LOW, SUN_HIGH, MOON, NONE }

private data class HeroSlide(
    val label: String,
    val title: String,
    val subtitle: String,
    val query: String,
    val top: Color,
    val bottom: Color,
    val body: Body,
)

private val STARS =
    listOf(
        0.06f to 0.14f, 0.16f to 0.32f, 0.27f to 0.10f, 0.38f to 0.26f, 0.47f to 0.08f,
        0.58f to 0.20f, 0.66f to 0.34f, 0.73f to 0.10f, 0.90f to 0.16f, 0.95f to 0.38f,
        0.34f to 0.44f, 0.12f to 0.52f,
    )

// x, width, height (fractions of the card) of the skyline blocks
private val SKYLINE =
    listOf(
        Triple(0.46f, 0.05f, 0.18f), Triple(0.51f, 0.04f, 0.28f), Triple(0.55f, 0.06f, 0.20f),
        Triple(0.61f, 0.045f, 0.34f), Triple(0.655f, 0.05f, 0.22f), Triple(0.705f, 0.06f, 0.30f),
        Triple(0.765f, 0.05f, 0.18f), Triple(0.815f, 0.07f, 0.26f), Triple(0.885f, 0.05f, 0.20f),
        Triple(0.935f, 0.07f, 0.30f),
    )

/**
 * "Music for calm evenings" hero banner. Text follows the phone's time of day, slides rotate on
 * their own, and "Play Now" auto-picks songs and keeps playing similar ones (radio).
 */
@Composable
fun HomeHeroBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val dayPart = rememberDayPart()

    val first =
        when (dayPart) {
            DayPart.MORNING ->
                HeroSlide(
                    stringResource(R.string.hero_label_morning),
                    stringResource(R.string.hero_title_morning),
                    stringResource(R.string.hero_sub_morning),
                    "morning fresh feel good hindi songs",
                    Color(0xFFF6A04D), Color(0xFF6B4E8C), Body.SUN_LOW,
                )
            DayPart.AFTERNOON ->
                HeroSlide(
                    stringResource(R.string.hero_label_afternoon),
                    stringResource(R.string.hero_title_afternoon),
                    stringResource(R.string.hero_sub_afternoon),
                    "upbeat bollywood hits afternoon playlist",
                    Color(0xFF3B82F6), Color(0xFF1E3A8A), Body.SUN_HIGH,
                )
            DayPart.EVENING ->
                HeroSlide(
                    stringResource(R.string.hero_label_evening),
                    stringResource(R.string.hero_title_evening),
                    stringResource(R.string.hero_sub_evening),
                    "calm evening chill hindi songs",
                    Color(0xFF3A2A5C), Color(0xFFE8793A), Body.SUN_LOW,
                )
            DayPart.NIGHT ->
                HeroSlide(
                    stringResource(R.string.hero_label_night),
                    stringResource(R.string.hero_title_night),
                    stringResource(R.string.hero_sub_night),
                    "late night lofi hindi songs",
                    Color(0xFF070B1E), Color(0xFF1B2A57), Body.MOON,
                )
        }
    val slides =
        listOf(
            first,
            HeroSlide(
                stringResource(R.string.hero_label_trending),
                stringResource(R.string.hero_title_trending),
                stringResource(R.string.hero_sub_trending),
                "trending songs india",
                Color(0xFF1F1147), Color(0xFFB5179E), Body.NONE,
            ),
            HeroSlide(
                stringResource(R.string.hero_label_chill),
                stringResource(R.string.hero_title_chill),
                stringResource(R.string.hero_sub_chill),
                "chill relaxing songs playlist",
                Color(0xFF0F3D3E), Color(0xFF2A9D8F), Body.NONE,
            ),
            HeroSlide(
                stringResource(R.string.hero_label_feelgood),
                stringResource(R.string.hero_title_feelgood),
                stringResource(R.string.hero_sub_feelgood),
                "feel good happy hindi songs",
                Color(0xFF7F1D1D), Color(0xFFF59E0B), Body.NONE,
            ),
            HeroSlide(
                stringResource(R.string.hero_label_romance),
                stringResource(R.string.hero_title_romance),
                stringResource(R.string.hero_sub_romance),
                "romantic hindi love songs",
                Color(0xFF5B1A3A), Color(0xFFE5466B), Body.NONE,
            ),
            HeroSlide(
                stringResource(R.string.hero_label_workout),
                stringResource(R.string.hero_title_workout),
                stringResource(R.string.hero_sub_workout),
                "workout gym motivation songs",
                Color(0xFF1B1B1B), Color(0xFFFF6B1A), Body.NONE,
            ),
            HeroSlide(
                stringResource(R.string.hero_label_party),
                stringResource(R.string.hero_title_party),
                stringResource(R.string.hero_sub_party),
                "party dance songs bollywood punjabi",
                Color(0xFF2A0B4D), Color(0xFF00A8B5), Body.NONE,
            ),
            HeroSlide(
                stringResource(R.string.hero_label_focus),
                stringResource(R.string.hero_title_focus),
                stringResource(R.string.hero_sub_focus),
                "focus study instrumental lofi songs",
                Color(0xFF10232E), Color(0xFF3B6E8F), Body.NONE,
            ),
            HeroSlide(
                stringResource(R.string.hero_label_throwback),
                stringResource(R.string.hero_title_throwback),
                stringResource(R.string.hero_sub_throwback),
                "90s bollywood evergreen hits",
                Color(0xFF4A3000), Color(0xFFC98B2B), Body.NONE,
            ),
            HeroSlide(
                stringResource(R.string.hero_label_peace),
                stringResource(R.string.hero_title_peace),
                stringResource(R.string.hero_sub_peace),
                "devotional bhajan peaceful songs",
                Color(0xFF0E2A2F), Color(0xFF6CA68A), Body.NONE,
            ),
        )

    val pagerState = rememberPagerState(pageCount = { slides.size })
    var loadingIndex by remember { mutableIntStateOf(-1) }
    val errorText = stringResource(R.string.hero_play_error)

    LaunchedEffect(pagerState) {
        while (true) {
            delay(6000)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(
                    page = (pagerState.currentPage + 1) % slides.size,
                    animationSpec = tween(800),
                )
            }
        }
    }

    fun play(
        index: Int,
        query: String,
    ) {
        if (loadingIndex != -1) return
        loadingIndex = index
        scope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                }
            val song =
                result
                    ?.items
                    ?.filterIsInstance<SongItem>()
                    ?.take(8)
                    ?.randomOrNull()
            if (song != null && playerConnection != null) {
                // radio = keeps auto-picking similar songs after this one
                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
            } else {
                Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
            }
            loadingIndex = -1
        }
    }

    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(350)) }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = appear.value },
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            HeroCard(
                slide = slides[page],
                loading = loadingIndex == page,
                onPlay = { play(page, slides[page].query) },
            )
        }
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 38.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(slides.size) { i ->
                val isSelected = pagerState.currentPage == i
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 22.dp else 8.dp,
                    animationSpec = tween(300),
                    label = "heroDot",
                )
                Box(
                    Modifier
                        .height(8.dp)
                        .width(dotWidth)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.45f)),
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    slide: HeroSlide,
    loading: Boolean,
    onPlay: () -> Unit,
) {
    val twinkle = rememberInfiniteTransition(label = "heroTwinkle")
    val t by twinkle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "t",
    )
    val shape = RoundedCornerShape(28.dp)
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(196.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(slide.top, slide.bottom)))
                .drawBehind { drawScene(slide, t) }
                .border(1.dp, Color.White.copy(alpha = 0.12f), shape),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.50f), Color.Transparent))),
        )
        Column(
            modifier = Modifier.padding(start = 22.dp, top = 20.dp, bottom = 20.dp, end = 22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = slide.label,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(0.62f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = slide.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(0.62f),
                )
            }
            Row(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                        .clickable(enabled = !loading, role = Role.Button, onClick = onPlay)
                        .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(primary),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.hero_play_now),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

private fun DrawScope.drawBody(
    center: Offset,
    radius: Float,
    color: Color,
) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), center = center, radius = radius * 3.2f),
        radius = radius * 3.2f,
        center = center,
    )
    drawCircle(color = color, radius = radius, center = center)
}

private fun DrawScope.drawScene(
    slide: HeroSlide,
    t: Float,
) {
    val w = size.width
    val h = size.height
    when (slide.body) {
        Body.SUN_LOW -> drawBody(Offset(w * 0.78f, h * 0.66f), h * 0.13f, Color(0xFFFFE08A))
        Body.SUN_HIGH -> drawBody(Offset(w * 0.80f, h * 0.28f), h * 0.12f, Color(0xFFFFF3B0))
        Body.MOON -> {
            drawBody(Offset(w * 0.80f, h * 0.26f), h * 0.10f, Color(0xFFEDEBFF))
            STARS.forEachIndexed { i, (x, y) ->
                val a = 0.30f + 0.70f * abs(sin((t * Math.PI.toFloat()) + i))
                drawCircle(Color.White.copy(alpha = a), radius = if (i % 3 == 0) 2.6f else 1.7f, center = Offset(w * x, h * y))
            }
        }
        Body.NONE -> {
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), center = Offset(w * 0.8f, h * 0.3f), radius = h * 0.8f),
                radius = h * 0.8f,
                center = Offset(w * 0.8f, h * 0.3f),
            )
        }
    }
    val windows = slide.body == Body.MOON || slide.body == Body.SUN_LOW
    SKYLINE.forEachIndexed { i, (x, bw, bh) ->
        val left = w * x
        val width = w * bw
        val height = h * bh
        val top = h - height
        drawRect(Color.Black.copy(alpha = 0.58f), Offset(left, top), Size(width, height))
        if (windows) {
            val rows = (height / 16f).toInt().coerceAtLeast(1)
            for (r in 0 until rows) {
                if ((r + i) % 2 == 0) {
                    drawRect(
                        Color(0xFFFFD27A).copy(alpha = if ((r + i) % 3 == 0) 0.9f else 0.5f),
                        Offset(left + width * 0.25f, top + 6f + r * 16f),
                        Size(width * 0.18f, 5f),
                    )
                }
            }
        }
    }
}
