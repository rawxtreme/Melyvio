/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.screens.settings

import com.aaditya.melyvio.ui.component.MelyvioMark
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.border
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.aaditya.melyvio.innertube.models.WatchEndpoint
import com.aaditya.melyvio.BuildConfig
import com.aaditya.melyvio.LocalPlayerAwareWindowInsets
import com.aaditya.melyvio.LocalPlayerConnection
import com.aaditya.melyvio.R
import com.aaditya.melyvio.playback.PlayerConnection
import com.aaditya.melyvio.playback.queues.YouTubeQueue
import com.aaditya.melyvio.ui.component.IconButton
import com.aaditya.melyvio.ui.component.Material3SettingsGroup
import com.aaditya.melyvio.ui.component.Material3SettingsItem
import com.aaditya.melyvio.ui.utils.backToMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

private data class Contributor(
    val name: String,
    val roleRes: Int,
    val githubHandle: String,
    val avatarUrl: String = "https://github.com/$githubHandle.png",
    val githubUrl: String = "https://github.com/$githubHandle",
    val sponsorUrl: String? = null,
    val polygon: RoundedPolygon? = null,
    val favoriteSongVideoId: String? = null
)

private data class CommunityLink(
    val labelRes: Int,
    val iconRes: Int,
    val url: String
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val leadDeveloper = Contributor(
    name = "Aaditya Shukla",
    roleRes = R.string.credits_lead_developer,
    githubHandle = "rawxtreme",
    polygon = MaterialShapes.Cookie9Sided
)

private val communityLinks = listOf(
    CommunityLink(R.string.credits_view_repo, R.drawable.github, "https://github.com/rawxtreme/Melyvio"),
    CommunityLink(R.string.credits_license_name, R.drawable.info, "")
)

private fun handleEasterEggClick(
    clickCount: Int,
    favoriteSongVideoId: String?,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    playerConnection: PlayerConnection?,
    wannaPlayStr: String,
    yeahStr: String,
    onCountUpdate: (Int) -> Unit
) {
    if (favoriteSongVideoId != null) {
        val newCount = clickCount + 1
        onCountUpdate(newCount)
        if (newCount >= 3) {
            onCountUpdate(0)
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = wannaPlayStr,
                    actionLabel = yeahStr,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = favoriteSongVideoId)))
                }
            }
        }
    }
}

/** Cookie-shaped ring around the developer photo. It starts turning the moment the screen opens. */
@Composable
private fun RotatingRing(
    shape: Shape,
    content: @Composable () -> Unit,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            rotation.animateTo(rotation.value + 360f, tween(8000, easing = LinearEasing))
            rotation.snapTo(rotation.value % 360f)
        }
    }
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotation.value }
                    .border(
                        3.dp,
                        Brush.sweepGradient(listOf(primary, tertiary, primary.copy(alpha = 0.25f), primary)),
                        shape,
                    ),
        )
        content()
    }
}

@Composable
private fun AnimatedAboutLogo() {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    val transition = rememberInfiniteTransition(label = "aboutLogo")
    val floatY by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY",
    )
    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val sweep by transition.animateFloat(
        initialValue = -0.6f,
        targetValue = -0.6f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes<Float> {
                        durationMillis = 4200
                        -0.6f at 0
                        1.6f at 1400
                        1.6f at 4200
                    },
                repeatMode = RepeatMode.Restart,
            ),
        label = "sweep",
    )
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        alpha = glow * enter.value
                        val s = 0.8f + 0.2f * enter.value
                        scaleX = s
                        scaleY = s
                    }.background(Brush.radialGradient(listOf(primary.copy(alpha = 0.6f), Color.Transparent)), CircleShape),
        )
        Image(
            painter = painterResource(R.drawable.about_logo),
            contentDescription = null,
            modifier =
                Modifier
                    .size(132.dp)
                    .graphicsLayer {
                        val s = 0.5f + 0.5f * enter.value
                        scaleX = s
                        scaleY = s
                        alpha = enter.value.coerceIn(0f, 1f)
                        translationY = floatY.dp.toPx()
                        compositingStrategy = CompositingStrategy.Offscreen
                    }.drawWithContent {
                        drawContent()
                        // colour the white logo with the theme gradient (keeps its shape)
                        drawRect(
                            brush = Brush.linearGradient(listOf(primary, tertiary), start = Offset(0f, 0f), end = Offset(size.width, size.height)),
                            blendMode = BlendMode.SrcIn,
                        )
                        // a light sweep that passes over the logo every few seconds
                        val x = size.width * sweep
                        drawRect(
                            brush =
                                Brush.linearGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent),
                                    start = Offset(x - size.width * 0.25f, 0f),
                                    end = Offset(x + size.width * 0.25f, size.height * 0.4f),
                                ),
                            blendMode = BlendMode.SrcAtop,
                        )
                    },
        )
    }
}


@Composable
private fun ContributorAvatar(
    avatarUrl: String,
    sizeDp: Int,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier.size(sizeDp.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // single-colour mark shows behind the photo while it loads (or if it fails)
            MelyvioMark(size = (sizeDp * 0.5f).dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            AsyncImage(
                model = avatarUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DeveloperSocials(
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalButton(
            onClick = { uriHandler.openUri("https://github.com/rawxtreme") },
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Icon(painterResource(R.drawable.github), contentDescription = null)
        }
        FilledTonalButton(
            onClick = { uriHandler.openUri("https://www.instagram.com/carryon.aditya?stkn=bjY1ZHk1MzdibHFo") },
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Icon(painterResource(R.drawable.instagram), contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    navController: NavController,
) {
    val uriHandler = LocalUriHandler.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val wannaPlayStr = stringResource(R.string.wanna_play_favorite_song)
    val yeahStr = stringResource(R.string.yeah)
    
    val windowInsets = LocalPlayerAwareWindowInsets.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                windowInsets.only(WindowInsetsSides.Top)
            )
        )

        Spacer(Modifier.height(16.dp))

        // App Header Section
        ElevatedCard(
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                MelyvioMark(
                    size = 64.dp,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    tile = MaterialTheme.colorScheme.primaryContainer,
                )
        
                Spacer(Modifier.width(20.dp))
        
                Column {
                    val metrolistName = stringResource(R.string.metrolist)
                        .lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                    Text(
                        text = metrolistName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
            
                    Spacer(Modifier.height(8.dp))
            
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = BuildConfig.ARCHITECTURE.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (BuildConfig.DEBUG) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "DEBUG",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Lead Developer Hero Card
        ElevatedCard(
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var leadClickCount by remember(leadDeveloper.name) { mutableIntStateOf(0) }
            
                    RotatingRing(shape = leadDeveloper.polygon?.toShape() ?: CircleShape) {
                        ContributorAvatar(
                            avatarUrl = leadDeveloper.avatarUrl,
                            sizeDp = 110,
                            shape = leadDeveloper.polygon?.toShape() ?: CircleShape,
                            contentDescription = leadDeveloper.name,
                            onClick = {
                                handleEasterEggClick(
                                    clickCount = leadClickCount,
                                    favoriteSongVideoId = leadDeveloper.favoriteSongVideoId,
                                    coroutineScope = coroutineScope,
                                    snackbarHostState = snackbarHostState,
                                    playerConnection = playerConnection,
                                    wannaPlayStr = wannaPlayStr,
                                    yeahStr = yeahStr,
                                    onCountUpdate = { leadClickCount = it }
                                )
                            }
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = leadDeveloper.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 38.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = stringResource(R.string.credits_lead_developer),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                DeveloperSocials(uriHandler)
                

            }
        }

        Spacer(Modifier.height(32.dp))
        
        // Community & Info using standard Group
        Material3SettingsGroup(
            title = stringResource(R.string.community_and_info),
            items = communityLinks.map { link ->
                Material3SettingsItem(
                    icon = painterResource(link.iconRes),
                    title = { Text(stringResource(link.labelRes), fontWeight = FontWeight.SemiBold) },
                    description = if (link.labelRes == R.string.credits_license_name) {
                        { Text(stringResource(R.string.credits_license_desc)) }
                    } else null,
                    // The licence row is informational only; only "View repo" opens GitHub.
                    onClick = if (link.url.isBlank()) null else ({ uriHandler.openUri(link.url) })
                )
            }
        )

        Spacer(Modifier.height(48.dp))
        
        AnimatedAboutLogo()
        
        Spacer(Modifier.height(48.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.about)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        }
    )

    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
        )
    }
}

