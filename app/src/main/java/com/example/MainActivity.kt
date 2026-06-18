package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import androidx.compose.ui.platform.LocalView
import android.view.SoundEffectConstants
import android.media.ToneGenerator
import android.media.AudioManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GeometricRandomizerScreen()
                }
            }
        }
    }
}

// Custom DASHED border modifier to match the dashboard wireframe perfectly
fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp
) = this.drawWithContent {
    drawContent()
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), 0f)
    )
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
    )
}

@Composable
fun ParticleBurst(trigger: Int, color: Color) {
    if (trigger == 0) return

    val progressAnim = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )
    }

    val progress = progressAnim.value
    if (progress < 1f) {
        val particles = remember(trigger) {
            List(24) { i ->
                val angle = i * (2 * Math.PI / 24) + kotlin.random.Random.nextDouble(-0.15, 0.15)
                val distance = kotlin.random.Random.nextDouble(100.0, 220.0).toFloat()
                val size = kotlin.random.Random.nextDouble(6.0, 14.0).toFloat()
                val colorVariant = if (kotlin.random.Random.nextBoolean()) color else Color(0xFFFF5252)
                Triple(angle, distance, size to colorVariant)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw an expanding glowing ring shockwave
            drawCircle(
                color = color.copy(alpha = (1f - progress) * 0.4f),
                radius = 160.dp.toPx() * progress,
                style = Stroke(width = 3.dp.toPx() * (1f - progress)),
                center = center
            )

            // Draw individual radiating particles
            particles.forEach { (angle, maxDistance, sizeAndColor) ->
                val (size, pColor) = sizeAndColor
                val currentDistance = maxDistance * progress
                val x = (currentDistance * Math.cos(angle)).toFloat()
                val y = (currentDistance * Math.sin(angle)).toFloat()
                val alpha = 1f - progress

                drawCircle(
                    color = pColor.copy(alpha = alpha),
                    radius = size * (1f - progress * 0.5f),
                    center = androidx.compose.ui.geometry.Offset(center.x + x, center.y + y)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeometricRandomizerScreen(viewModel: MainViewModel = viewModel()) {
    val minRange by viewModel.minRange.collectAsState()
    val maxRange by viewModel.maxRange.collectAsState()
    val currentNumber by viewModel.currentNumber.collectAsState()
    val isRolling by viewModel.isRolling.collectAsState()
    val history by viewModel.history.collectAsState()
    val allowDuplicates by viewModel.allowDuplicates.collectAsState()

    var activeTab by remember { mutableStateOf("random") } // "random", "history", "saved"
    var showSettings by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGenerator?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    LaunchedEffect(currentNumber) {
        if (currentNumber != null) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 25)
            } catch (e: Exception) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        }
    }

    // Saved custom ranges state for "Saved" screen tab
    var savedRanges by remember {
        mutableStateOf(
            listOf(
                1 to 6,
                1 to 10,
                1 to 20,
                1 to 100
            )
        )
    }

    // Local state for settings inputs
    var minInput by remember(minRange) { mutableStateOf(minRange.toString()) }
    var maxInput by remember(maxRange) { mutableStateOf(maxRange.toString()) }

    val minVal = minInput.toIntOrNull() ?: 1
    val maxVal = maxInput.toIntOrNull() ?: 100
    val isRangeValid = minVal <= maxVal

    Scaffold(
        bottomBar = {
            // High-fidelity integrated Navigation Bar Mock matching Geometric Balance UI
            NavigationBar(
                containerColor = GeoSurfaceVariant,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(BorderStroke(1.dp, GeoBorder.copy(alpha = 0.5f)))
                    .height(84.dp)
            ) {
                NavigationBarItem(
                    selected = activeTab == "random",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        activeTab = "random"
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Random selector generator",
                            tint = if (activeTab == "random") GeoPurple else GeoTextMedium
                        )
                    },
                    label = {
                        Text(
                            text = "Random",
                            fontWeight = if (activeTab == "random") FontWeight.Bold else FontWeight.Normal,
                            color = if (activeTab == "random") GeoPurple else GeoTextMedium,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = GeoLightPurple
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "history",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        activeTab = "history"
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historical records log",
                            tint = if (activeTab == "history") GeoPurple else GeoTextMedium
                        )
                    },
                    label = {
                        Text(
                            text = "History",
                            fontWeight = if (activeTab == "history") FontWeight.Bold else FontWeight.Normal,
                            color = if (activeTab == "history") GeoPurple else GeoTextMedium,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = GeoLightPurple
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "saved",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        activeTab = "saved"
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Saved presets lists",
                            tint = if (activeTab == "saved") GeoPurple else GeoTextMedium
                        )
                    },
                    label = {
                        Text(
                            text = "Saved",
                            fontWeight = if (activeTab == "saved") FontWeight.Bold else FontWeight.Normal,
                            color = if (activeTab == "saved") GeoPurple else GeoTextMedium,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = GeoLightPurple
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GeoCreamBg)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                // Status Bar & Header Mock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GeoPurple.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Application Menu",
                                    tint = GeoTextMedium,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Randomizer",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = GeoTextDark
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick range readout pill visualizer
                        Box(
                            modifier = Modifier
                                .background(GeoPurple.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, GeoPurple.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "L: $minRange - $maxRange",
                                color = GeoPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        // Dedicated Refresh/Reset Button
                        Surface(
                            shape = CircleShape,
                            color = ErrorRed.copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.updateRange(1, 100)
                                    viewModel.clearHistory()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Clear history and reset range inputs to default",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = GeoPurple.copy(alpha = 0.1f),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showSettings = true
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Configuration Sheet toggle",
                                    tint = GeoPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Main content swap depending on Active Tab Selector
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTab) {
                        "random" -> {
                            // Home Page: Random Interactive Dice Area
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Tactile visual spring pop modifier
                                val scaleAnim by animateFloatAsState(
                                    targetValue = if (isRolling) 0.94f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "GeneratorScale"
                                )

                                // Track completion triggers for the interactive particle explosion
                                var triggerBurst by remember { mutableStateOf(0) }
                                LaunchedEffect(isRolling) {
                                    if (!isRolling && currentNumber != null) {
                                        triggerBurst += 1
                                    }
                                }

                                // CSS keyframe translation: Infinite/state-driven animations for 3D tumbling & pulsing
                                val infiniteTransition = rememberInfiniteTransition(label = "cssEffects")

                                // 3D rotation simulations mimicking CSS transform: rotate3d()
                                val rotationX by animateFloatAsState(
                                    targetValue = if (isRolling) 14f else 0f,
                                    animationSpec = if (isRolling) {
                                        infiniteRepeatable(
                                            animation = tween(400, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    } else {
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    },
                                    label = "rotationX"
                                )

                                val rotationY by animateFloatAsState(
                                    targetValue = if (isRolling) 22f else 0f,
                                    animationSpec = if (isRolling) {
                                        infiniteRepeatable(
                                            animation = tween(500, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    } else {
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    },
                                    label = "rotationY"
                                )

                                val rotationZ by animateFloatAsState(
                                    targetValue = if (isRolling) 5f else 0f,
                                    animationSpec = if (isRolling) {
                                        infiniteRepeatable(
                                            animation = tween(140, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    } else {
                                        spring(
                                            dampingRatio = Spring.DampingRatioHighBouncy,
                                            stiffness = Spring.StiffnessHigh
                                        )
                                    },
                                    label = "rotationZ"
                                )

                                // Custom CSS box-shadow neon pulsing glow equivalent via border colors
                                val glowColor by animateColorAsState(
                                    targetValue = if (isRolling) Color(0xFFFF00FF) else GeoBorder.copy(alpha = 0.5f),
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                    label = "glowColor"
                                )

                                val glowWidth by animateDpAsState(
                                    targetValue = if (isRolling) 4.dp else 2.dp,
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    label = "glowWidth"
                                )

                                // Scale oscillation for pulsing "alive" card state when rolling
                                val scaleOscillation by infiniteTransition.animateFloat(
                                    initialValue = 0.97f,
                                    targetValue = 1.03f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(350, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "scaleOscillation"
                                )

                                val cardScale = if (isRolling) scaleOscillation else scaleAnim

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // 1. Particle Overlay (Unclipped, so particles freely blast outside card boundary)
                                    ParticleBurst(trigger = triggerBurst, color = GeoPurple)

                                    // 2. The Interactive Glass/Slab Card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .scale(cardScale)
                                            .graphicsLayer {
                                                this.rotationX = rotationX
                                                this.rotationY = rotationY
                                                this.rotationZ = rotationZ
                                                this.cameraDistance = 16f * density
                                            }
                                            .background(GeoSurface, RoundedCornerShape(48.dp))
                                            .dashedBorder(glowWidth, glowColor, 48.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                if (!isRolling) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.roll()
                                                }
                                            }
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = "CURRENT RESULT",
                                                color = GeoPurple,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.SansSerif,
                                                letterSpacing = 2.sp,
                                                modifier = Modifier.padding(top = 12.dp)
                                            )

                                            // Large Light Font Result text
                                            AnimatedContent(
                                                targetState = currentNumber,
                                                transitionSpec = {
                                                    if (isRolling) {
                                                        // CSS slot-machine-style vertical scrolling transition
                                                        (slideInVertically(animationSpec = tween(90, easing = LinearEasing)) { h -> h / 2 } + fadeIn(animationSpec = tween(60))) togetherWith
                                                                (slideOutVertically(animationSpec = tween(90, easing = LinearEasing)) { h -> -h / 2 } + fadeOut(animationSpec = tween(60)))
                                                    } else {
                                                        // CSS elastic-overshoot spring-pop on layout entry
                                                        (scaleIn(
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            )
                                                        ) + fadeIn()).togetherWith(
                                                            scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
                                                        )
                                                    }
                                                },
                                                label = "NumberValueTransition"
                                            ) { rolledVal ->
                                                Text(
                                                    text = rolledVal?.toString() ?: "--",
                                                    color = GeoPurple,
                                                    fontSize = if (rolledVal?.toString()?.length ?: 0 > 4) 68.sp else 94.sp,
                                                    fontWeight = FontWeight.Light,
                                                    fontFamily = FontFamily.SansSerif,
                                                    textAlign = TextAlign.Center
                                                )
                                            }

                                            // Action indicator instructions
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            ) {
                                                // Cute bouncing tap icon or simple icon
                                                val dy by infiniteTransition.animateFloat(
                                                    initialValue = 0f,
                                                    targetValue = -6f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(600, easing = FastOutSlowInEasing),
                                                        repeatMode = RepeatMode.Reverse
                                                    ),
                                                    label = "dy"
                                                )

                                                Icon(
                                                    imageVector = Icons.Default.TouchApp,
                                                    contentDescription = null,
                                                    tint = GeoPurple,
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .offset(y = dy.dp)
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = if (isRolling) "Rolling..." else "Tap to generate",
                                                    color = GeoTextMedium,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                // Visual Indicator for sequence duplicates rule settings
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .background(GeoSurfaceVariant, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = null,
                                        tint = GeoPurple.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (allowDuplicates) "Sequence duplicates allowed" else "No consecutive repetition",
                                        fontSize = 11.sp,
                                        color = GeoTextMedium,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        "history" -> {
                            // History Page: High fidelity full past roll records list
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Generation Log (${history.size})",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GeoTextDark
                                    )

                                    if (history.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.clearHistory()
                                            }
                                        ) {
                                            Text("Clear History", color = ErrorRed)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (history.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .dashedBorder(1.dp, GeoBorder, 16.dp)
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = GeoBorder,
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "No recorded rolls in current session",
                                                color = GeoTextMedium,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp)
                                    ) {
                                        items(history.mapIndexed { index, i -> index to i }) { (idx, value) ->
                                            Card(
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(containerColor = GeoSurfaceVariant),
                                                modifier = Modifier.fillMaxWidth(),
                                                border = BorderStroke(1.dp, GeoBorder.copy(alpha = 0.2f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(18.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .background(GeoLightPurple, CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "#${history.size - idx}",
                                                                color = GeoDarkPurple,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(14.dp))
                                                        Text(
                                                            text = "Generated Roll",
                                                            fontSize = 14.sp,
                                                            color = GeoTextMedium
                                                        )
                                                    }

                                                    Text(
                                                        text = value.toString(),
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GeoPurple,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "saved" -> {
                            // Saved Presets page: Allows quickly launching preset bounds
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Range Presets",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GeoTextDark
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(savedRanges) { (sMin, sMax) ->
                                        val isCurrent = minRange == sMin && maxRange == sMax
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.updateRange(sMin, sMax)
                                                },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isCurrent) GeoLightPurple else GeoSurfaceVariant
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isCurrent) GeoPurple else GeoBorder.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(18.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "INTERVAL",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp,
                                                    color = if (isCurrent) GeoDarkPurple else GeoTextMedium
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "$sMin to $sMax",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrent) GeoPurple else GeoTextDark,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = if (isCurrent) "ACTIVE" else "TAP TO LOAD",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrent) GeoPurple else GeoTextMedium.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick Creator card to save active config easily
                                val alreadySaved = savedRanges.any { it.first == minRange && it.second == maxRange }

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!alreadySaved) {
                                            savedRanges = savedRanges + (minRange to maxRange)
                                        }
                                    },
                                    enabled = !alreadySaved,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GeoLightPurple,
                                        contentColor = GeoDarkPurple,
                                        disabledContainerColor = GeoSurfaceVariant
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (alreadySaved) "CURRENT LIMIT SAVED" else "SAVE CURRENT LIMIT PRESET",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Slide Overlay Backdrop
            AnimatedVisibility(
                visible = showSettings,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xD9100F14))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isRangeValid) {
                                viewModel.updateRange(minVal, maxVal)
                                showSettings = false
                            }
                        }
                )
            }

            // Settings Sheet Bottom sliding layer (Geometric Balance specifications)
            AnimatedVisibility(
                visible = showSettings,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(1.dp, GeoBorder.copy(alpha = 0.5f)),
                            RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        ),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = GeoSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Drag Handle matching high quality HTML design
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(4.dp)
                                .align(Alignment.CenterHorizontally)
                                .background(GeoLabelAccent.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        )

                        // Title Settings segment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Setting configuration icon",
                                    tint = GeoTextMedium,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Range Settings",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GeoTextDark
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isRangeValid) {
                                        viewModel.updateRange(minVal, maxVal)
                                        showSettings = false
                                    }
                                },
                                modifier = Modifier
                                    .background(GeoPurple.copy(alpha = 0.08f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss range settings overlay",
                                    tint = GeoPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Divider(color = GeoBorder.copy(alpha = 0.3f), thickness = 1.dp)

                        // Dual Value numeric settings boxes with solid bottom borders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Minimum input card box
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(GeoInputBg, RoundedCornerShape(12.dp))
                                    .border(
                                        BorderStroke(
                                            0.dp,
                                            Color.Transparent
                                        )
                                    )
                                    .drawWithContent {
                                        drawContent()
                                        // Colored bottom border matching geometric style
                                        val strokeW = 2.dp.toPx()
                                        drawLine(
                                            color = GeoPurple,
                                            start = androidx.compose.ui.geometry.Offset(0f, size.height - strokeW/2),
                                            end = androidx.compose.ui.geometry.Offset(size.width, size.height - strokeW/2),
                                            strokeWidth = strokeW
                                        )
                                    }
                                    .padding(vertical = 10.dp, horizontal = 14.dp)
                            ) {
                                Text(
                                    text = "MIN VALUE",
                                    color = GeoPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextField(
                                    value = minInput,
                                    onValueChange = { text ->
                                        if (text.isEmpty() || text.all { it.isDigit() }) {
                                            minInput = text
                                        }
                                    },
                                    placeholder = { Text("1") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = GeoTextDark,
                                        unfocusedTextColor = GeoTextDark
                                    ),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                )
                            }

                            // Maximum input card box
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(GeoInputBg, RoundedCornerShape(12.dp))
                                    .border(BorderStroke(0.dp, Color.Transparent))
                                    .drawWithContent {
                                        drawContent()
                                        // Colored bottom border matching geometric style
                                        val strokeW = 2.dp.toPx()
                                        drawLine(
                                            color = GeoPurple,
                                            start = androidx.compose.ui.geometry.Offset(0f, size.height - strokeW/2),
                                            end = androidx.compose.ui.geometry.Offset(size.width, size.height - strokeW/2),
                                            strokeWidth = strokeW
                                        )
                                    }
                                    .padding(vertical = 10.dp, horizontal = 14.dp)
                            ) {
                                Text(
                                    text = "MAX VALUE",
                                    color = GeoPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextField(
                                    value = maxInput,
                                    onValueChange = { text ->
                                        if (text.isEmpty() || text.all { it.isDigit() }) {
                                            maxInput = text
                                        }
                                    },
                                    placeholder = { Text("100") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = GeoTextDark,
                                        unfocusedTextColor = GeoTextDark
                                    ),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                )
                            }
                        }

                        // Allow duplicates preference toggle card matching
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GeoCreamBg, RoundedCornerShape(16.dp))
                                .border(BorderStroke(1.dp, GeoBorder.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Allow Duplicates",
                                    color = GeoTextDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Numbers can repeat sequentially",
                                    color = GeoTextMedium,
                                    fontSize = 12.sp
                                )
                            }

                            Switch(
                                checked = allowDuplicates,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setAllowDuplicates(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GeoPurple,
                                    uncheckedThumbColor = GeoTextMedium,
                                    uncheckedTrackColor = GeoInputBg
                                )
                            )
                        }

                        // Error state visual guide
                        if (!isRangeValid) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Minimum limit must be less than/equal to maximum.",
                                    color = ErrorRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Save Range and Reset defaults button visual layout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.updateRange(1, 100)
                                    viewModel.clearHistory()
                                },
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset limits and clear history",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reset All",
                                    color = ErrorRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    if (isRangeValid) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.updateRange(minVal, maxVal)
                                        showSettings = false
                                    }
                                },
                                enabled = isRangeValid && minInput.isNotEmpty() && maxInput.isNotEmpty(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GeoLightPurple,
                                    contentColor = GeoDarkPurple,
                                    disabledContainerColor = GeoInputBg,
                                    disabledContentColor = GeoTextMedium.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .height(52.dp)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isRangeValid) GeoPurple.copy(alpha = 0.3f) else Color.Transparent
                                        ),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentPadding = PaddingValues(horizontal = 24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Save Range",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
