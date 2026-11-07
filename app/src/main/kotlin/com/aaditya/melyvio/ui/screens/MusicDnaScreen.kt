/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.aaditya.melyvio.LocalDatabase
import com.aaditya.melyvio.LocalPlayerAwareWindowInsets
import com.aaditya.melyvio.R
import com.aaditya.melyvio.constants.PauseListenHistoryKey
import com.aaditya.melyvio.ui.component.DnaHelix
import com.aaditya.melyvio.ui.component.IconButton
import com.aaditya.melyvio.ui.utils.backToMain
import com.aaditya.melyvio.utils.DnaArtist
import com.aaditya.melyvio.utils.DnaDay
import com.aaditya.melyvio.utils.DnaPeriod
import com.aaditya.melyvio.utils.DnaSong
import com.aaditya.melyvio.utils.MusicDna
import com.aaditya.melyvio.utils.MusicDnaData
import com.aaditya.melyvio.utils.MusicDnaImage
import com.aaditya.melyvio.utils.formatDnaDuration
import com.aaditya.melyvio.utils.rememberPreference
import kotlinx.coroutines.launch

/** "Your Music DNA": taste summary built from the on-device listening history, with a share picture. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicDnaScreen(navController: NavController) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    var period by rememberSaveable { mutableStateOf(DnaPeriod.MONTH) }
    var refresh by remember { mutableIntStateOf(0) }
    var sharing by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    val (pauseHistory, setPauseHistory) = rememberPreference(PauseListenHistoryKey, false)

    val dna by produceState<MusicDnaData?>(initialValue = null, period, refresh) {
        value = null
        value = MusicDna.compute(database, period)
    }
    val periodLabel =
        stringResource(
            when (period) {
                DnaPeriod.WEEK -> R.string.dna_period_week_long
                DnaPeriod.MONTH -> R.string.dna_period_month_long
                DnaPeriod.ALL -> R.string.dna_period_all_long
            },
        )
    val shareText = stringResource(R.string.dna_share_text)
    val accent = colors.primary.toArgb()

    if (showClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text(stringResource(R.string.dna_clear_history)) },
            text = { Text(stringResource(R.string.dna_clear_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClear = false
                        database.query { clearListenHistory() }
                        refresh++
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        // period chips
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                DnaPeriod.WEEK to R.string.dna_period_week,
                DnaPeriod.MONTH to R.string.dna_period_month,
                DnaPeriod.ALL to R.string.dna_period_all,
            ).forEach { (p, label) ->
                FilterChip(
                    selected = period == p,
                    onClick = { period = p },
                    label = { Text(stringResource(label)) },
                )
            }
        }

        val data = dna
        if (data == null) {
            Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            HeroCard(data)
            Spacer(Modifier.height(16.dp))

            if (data.isEmpty) {
                Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.library_music),
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.dna_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.dna_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(stringResource(R.string.dna_stat_time), formatDnaDuration(data.totalMinutes), Modifier.weight(1f))
                    StatCard(stringResource(R.string.dna_stat_plays), data.totalPlays.toString(), Modifier.weight(1f))
                    StatCard(stringResource(R.string.dna_stat_unique), data.uniqueSongs.toString(), Modifier.weight(1f))
                }

                Section(R.drawable.mic, stringResource(R.string.dna_top_artists)) {
                    data.topArtists.forEachIndexed { i, artist -> ArtistRow(i + 1, artist) }
                }
                Section(R.drawable.music_note, stringResource(R.string.dna_top_songs)) {
                    data.topSongs.forEachIndexed { i, song -> SongRow(i + 1, song) }
                }
                Section(R.drawable.tune, stringResource(R.string.dna_genres)) {
                    data.genres.forEach { GenreBar(it.name, it.percent) }
                    Text(
                        stringResource(R.string.dna_genres_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            Section(R.drawable.timer, stringResource(R.string.dna_weekly)) {
                Text(
                    text = formatDnaDuration(data.weeklyMinutes) + " " + stringResource(R.string.dna_this_week),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = colors.primary,
                )
                Spacer(Modifier.height(12.dp))
                WeeklyChart(data.weekly)
            }

            if (data.liked.isNotEmpty()) {
                Section(R.drawable.favorite, stringResource(R.string.dna_liked)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        data.liked.forEach { LikedTile(it) }
                    }
                }
            }

            Section(R.drawable.explore_outlined, stringResource(R.string.dna_discovered)) {
                if (data.discovered.isEmpty()) {
                    Text(
                        stringResource(R.string.dna_discovered_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        data.discovered.forEach { DiscoveredTile(it) }
                    }
                }
            }

            if (!data.isEmpty) {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (!sharing) {
                            sharing = true
                            scope.launch {
                                runCatching { MusicDnaImage.share(context, data, periodLabel, accent, shareText) }
                                sharing = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                ) {
                    if (sharing) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = colors.onPrimary)
                    } else {
                        Icon(painterResource(R.drawable.share), contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.dna_share), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // privacy
            Section(R.drawable.lock, stringResource(R.string.dna_privacy_title)) {
                Text(
                    stringResource(R.string.dna_privacy_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.dna_record_history), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.dna_record_history_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Switch(checked = !pauseHistory, onCheckedChange = { setPauseHistory(!it) })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showClear = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.delete), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.dna_clear_history))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.dna_title)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

@Composable
private fun HeroCard(data: MusicDnaData) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.05f)))),
    ) {
        DnaHelix(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp).size(width = 74.dp, height = 130.dp),
            strandA = Color.White,
            strandB = Color.White.copy(alpha = 0.55f),
            rungColor = Color.White.copy(alpha = 0.4f),
        )
        Column(modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 22.dp, end = 110.dp)) {
            Text(
                text = stringResource(R.string.dna_screen_title),
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 3.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(data.persona.icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = data.persona.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = data.persona.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Section(
    icon: Int,
    title: String,
    content: @Composable () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    Spacer(Modifier.height(12.dp))
    content()
}

@Composable
private fun Thumb(
    url: String?,
    size: androidx.compose.ui.unit.Dp,
    circle: Boolean,
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .size(size)
                .clip(if (circle) CircleShape else RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

@Composable
private fun ArtistRow(
    rank: Int,
    artist: DnaArtist,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = if (rank == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        Thumb(artist.thumbnailUrl, if (rank == 1) 56.dp else 48.dp, circle = true)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = if (rank == 1) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = if (rank == 1) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.dna_plays, artist.plays),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SongRow(
    rank: Int,
    song: DnaSong,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = if (rank == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        Thumb(song.thumbnailUrl, 52.dp, circle = false)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(R.string.dna_plays, song.plays),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun GenreBar(
    name: String,
    percent: Int,
) {
    val fraction by animateFloatAsState(percent / 100f, tween(800), label = "genre")
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("$percent%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
            )
        }
    }
}

@Composable
private fun WeeklyChart(days: List<DnaDay>) {
    val max = (days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            val barHeight by animateDpAsState((90.dp * day.minutes / max).coerceAtLeast(4.dp), tween(700), label = "bar")
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = if (day.minutes > 0) day.minutes.toString() + "m" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(22.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
                )
                Spacer(Modifier.height(6.dp))
                Text(day.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LikedTile(song: DnaSong) {
    Column(Modifier.width(110.dp)) {
        Thumb(song.thumbnailUrl, 110.dp, circle = false)
        Spacer(Modifier.height(6.dp))
        Text(song.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            song.artist,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DiscoveredTile(artist: DnaArtist) {
    Column(Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(72.dp)
                .border(2.dp, Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary)), CircleShape)
                .padding(4.dp),
        ) {
            Thumb(artist.thumbnailUrl, 64.dp, circle = true)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
