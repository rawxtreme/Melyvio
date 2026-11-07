/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.screens

import com.aaditya.melyvio.ui.component.ProfileFieldIcon
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aaditya.melyvio.LocalPlayerAwareWindowInsets
import com.aaditya.melyvio.R
import com.aaditya.melyvio.constants.GreetingNameKey
import com.aaditya.melyvio.constants.ProfilePhotoVersionKey
import com.aaditya.melyvio.ui.component.rememberProfileBitmap
import com.aaditya.melyvio.ui.utils.greetingAnnotated
import com.aaditya.melyvio.ui.utils.rememberDayPart
import com.aaditya.melyvio.utils.ProfilePhoto
import com.aaditya.melyvio.utils.rememberPreference
import com.aaditya.melyvio.utils.safeDataStoreEdit
import kotlinx.coroutines.launch

private const val MAX_NAME_LENGTH = 10

/**
 * "How would you like us to greet you?" - a login-style screen without any login. The user picks a
 * display name (max 10 characters) and, if they like, a profile photo. Everything stays on the phone.
 */
@Composable
fun GreetingScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    val dayPart = rememberDayPart()
    val primary = colors.primary
    val tertiary = colors.tertiary

    val (savedName) = rememberPreference(GreetingNameKey, "")
    val (photoVersion) = rememberPreference(ProfilePhotoVersionKey, 0L)
    val hasPhoto = photoVersion > 0L
    val photo = rememberProfileBitmap(large = true)
    var name by remember { mutableStateOf("") }
    var touched by remember { mutableStateOf(false) }
    LaunchedEffect(savedName) { if (!touched) name = savedName }

    val photoError = stringResource(R.string.greeting_photo_error)
    val pickPhoto =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                scope.launch {
                    if (ProfilePhoto.save(context, uri)) {
                        context.safeDataStoreEdit { it[ProfilePhotoVersionKey] = System.currentTimeMillis() }
                    } else {
                        Toast.makeText(context, photoError, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    fun openPicker() {
        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun save(value: String) {
        focusManager.clearFocus()
        scope.launch {
            context.safeDataStoreEdit { it[GreetingNameKey] = value.trim().take(MAX_NAME_LENGTH) }
            navController.navigateUp()
        }
    }

    // One short fade for the whole page (no per-item delays), so text shows up straight away.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(240)) }

    val infinite = rememberInfiniteTransition(label = "greeting")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    val wave by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes<Float> {
                        durationMillis = 3000
                        0f at 0
                        18f at 250
                        -8f at 500
                        16f at 750
                        -6f at 1000
                        10f at 1250
                        0f at 1500
                        0f at 3000
                    },
                repeatMode = RepeatMode.Restart,
            ),
        label = "wave",
    )

    Box(modifier = Modifier.fillMaxSize().background(colors.surface)) {
        // soft drifting colour blobs (moved on the GPU, no redraw every frame)
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-90).dp, y = (-70).dp)
                    .size(340.dp)
                    .graphicsLayer {
                        translationX = 70.dp.toPx() * drift
                        translationY = 30.dp.toPx() * drift
                    }.background(Brush.radialGradient(listOf(primary.copy(alpha = 0.26f), Color.Transparent)), CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 90.dp, y = 60.dp)
                    .size(340.dp)
                    .graphicsLayer {
                        translationX = (-60).dp.toPx() * drift
                        translationY = (-20).dp.toPx() * drift
                    }.background(Brush.radialGradient(listOf(tertiary.copy(alpha = 0.22f), Color.Transparent)), CircleShape),
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Vertical + WindowInsetsSides.Horizontal))
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .graphicsLayer {
                        alpha = appear.value
                        translationY = (1f - appear.value) * 20.dp.toPx()
                    }.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(56.dp))

            // Avatar: user's photo (bigger here) or the waving hand
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(148.dp)) {
                Box(
                    Modifier
                        .size(112.dp)
                        .graphicsLayer {
                            val s = 1f + 0.4f * pulse
                            scaleX = s
                            scaleY = s
                            alpha = 0.32f * (1f - pulse)
                        }.background(primary, CircleShape),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(112.dp)
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(primary, tertiary)))
                            .clickable { openPicker() },
                ) {
                    if (photo != null) {
                        Image(
                            bitmap = photo,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = "\uD83D\uDC4B",
                            fontSize = 50.sp,
                            modifier =
                                Modifier.graphicsLayer {
                                    transformOrigin = TransformOrigin(0.7f, 0.9f)
                                    rotationZ = wave
                                },
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp)
                            .size(36.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(colors.primaryContainer)
                            .clickable { openPicker() },
                ) {
                    Icon(
                        painter = painterResource(if (hasPhoto) R.drawable.edit else R.drawable.add),
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Add / change / remove photo (remove only shows when a photo is set)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { openPicker() }) {
                    Text(stringResource(if (hasPhoto) R.string.greeting_change_photo else R.string.greeting_add_photo))
                }
                if (hasPhoto) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                ProfilePhoto.remove(context)
                                context.safeDataStoreEdit { it[ProfilePhotoVersionKey] = 0L }
                            }
                        },
                    ) {
                        Text(stringResource(R.string.greeting_remove_photo))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.greeting_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.greeting_screen_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { value ->
                    touched = true
                    name = value.filter { it != '\n' }.take(MAX_NAME_LENGTH)
                },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.greeting_name_hint)) },
                leadingIcon = { ProfileFieldIcon() },
                supportingText = {
                    Text(
                        text = "${name.length}/$MAX_NAME_LENGTH",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
                shape = RoundedCornerShape(20.dp),
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions = KeyboardActions(onDone = { save(name) }),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Text(
                        text = stringResource(R.string.greeting_preview),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = greetingAnnotated(name, dayPart),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { save(name) },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(painterResource(R.drawable.check), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.greeting_save), fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = {
                    touched = true
                    name = ""
                    save("")
                },
            ) {
                Text(stringResource(R.string.greeting_use_default))
            }

            Spacer(Modifier.height(32.dp))
        }

        // Back button
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.onSurface.copy(alpha = 0.08f))
                    .clickable { navController.navigateUp() },
        ) {
            Icon(painterResource(R.drawable.arrow_back), contentDescription = null, tint = colors.onSurface)
        }
    }
}
