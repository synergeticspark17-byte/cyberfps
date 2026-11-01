package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameEngine
import com.example.model.WeaponType
import com.example.ui.theme.*
import kotlinx.coroutines.isActive
import kotlin.math.*

@Composable
fun GameScreen(
    engine: GameEngine,
    onPauseClicked: () -> Unit,
    onLevelCompleted: () -> Unit,
    onGameOver: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Game loop ticker (~60 FPS)
    var frameTick by remember { mutableLongStateOf(0L) }
    var joyForward by remember { mutableFloatStateOf(0f) }
    var joyStrafe by remember { mutableFloatStateOf(0f) }
    var lookDeltaX by remember { mutableFloatStateOf(0f) }
    var isFiringHeld by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (isActive) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                // Auto-fire if held
                if (isFiringHeld) {
                    engine.onFirePressed()
                }

                engine.update(
                    dt = dt,
                    moveForward = joyForward,
                    moveStrafe = joyStrafe,
                    lookDeltaX = lookDeltaX
                )
                lookDeltaX = 0f // Reset delta after consuming

                engine.renderFrame()
                frameTick++

                if (engine.isLevelComplete) onLevelCompleted()
                if (engine.isGameOver) onGameOver()
            }
        }
    }

    // Dynamic camera tilt during wall running
    val cameraRoll by animateFloatAsState(
        targetValue = if (engine.isWallRunning) (engine.wallSide * 5f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cameraRoll"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarker)
    ) {
        // 1. Raycasted 3D World Bitmap Viewport (Scaled with screen roll)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(cameraRoll)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val img = engine.renderBitmap.asImageBitmap()
                drawImage(
                    image = img,
                    dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.None
                )
            }

            // Floating 3D Damage Texts
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (dmg in engine.damageTexts) {
                    val dx = dmg.x - engine.posX
                    val dy = dmg.y - engine.posY
                    val dist = hypot(dx, dy)
                    if (dist > 0.5f) {
                        val cosA = cos(engine.angle)
                        val sinA = sin(engine.angle)
                        val transX = sinA * dx - cosA * dy
                        val transY = cosA * dx + sinA * dy
                        if (transY > 0.2f) {
                            val sx = (size.width * 0.5f) * (1 + transX / (transY * tan(engine.fov * 0.5f)))
                            val sy = size.height * 0.5f - ((dmg.z - engine.posZ) * (size.height / transY))
                            drawCircle(Color(dmg.color), radius = 6f, center = Offset(sx, sy))
                        }
                    }
                }
            }

            // Speed Lines Effect when wall running or dashing
            if (engine.isWallRunning || engine.isDashing || engine.momentumMultiplier > 1.4f) {
                SpeedLinesOverlay(modifier = Modifier.fillMaxSize())
            }

            // 2. First-Person 2D Weapon Viewmodel
            WeaponViewmodel(
                weapon = engine.currentWeapon,
                bobPhase = engine.weaponBobPhase,
                recoil = engine.weaponRecoil,
                isFiring = engine.muzzleFlashTimer > 0f,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Hurt Vignette (Red Pulse on damage)
        if (engine.hurtVignette > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HealthRed.copy(alpha = engine.hurtVignette * 0.45f))
            )
        }

        // 3. Central Reticle & Smart Vertical Auto-Aim Indicator
        AutoAimCrosshair(
            autoAimTarget = engine.autoAimTarget,
            modifier = Modifier.fillMaxSize()
        )

        // 4. Look Touchpad (Entire right half of screen for touch-look rotation)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.55f)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Horizontal camera yaw rotation (No looking up or down)
                        lookDeltaX += dragAmount.x * 0.0055f
                    }
                }
        )

        // 5. HUD Top Bar: Vitals, Level Info, Mini-Map, Pause
        HudTopBar(
            engine = engine,
            onPauseClicked = onPauseClicked,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        // 6. HUD Center Left: Speedometer & Parkour Status
        SpeedometerBadge(
            engine = engine,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )

        // 7. On-Screen Touch Controls (Virtual Joystick & Action Buttons)
        TouchControlsLayer(
            engine = engine,
            onMove = { fwd, str ->
                joyForward = fwd
                joyStrafe = str
            },
            onFireHeld = { isFiringHeld = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

@Composable
private fun SpeedLinesOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Draw radial motion streaks from screen edges
        for (i in 0 until 12) {
            val yOffset = (i * h / 12f)
            drawLine(
                color = Color(0x6600F5FF),
                start = Offset(0f, yOffset),
                end = Offset(w * 0.15f, yOffset + (i - 6) * 15f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0x6600F5FF),
                start = Offset(w, yOffset),
                end = Offset(w * 0.85f, yOffset + (i - 6) * 15f),
                strokeWidth = 3f
            )
        }
    }
}

@Composable
private fun AutoAimCrosshair(
    autoAimTarget: com.example.model.AutoAimTarget?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Center reticle
        Box(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.Center)
                .border(1.5.dp, if (autoAimTarget != null) NeonPink else NeonCyan, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .align(Alignment.Center)
                    .background(if (autoAimTarget != null) NeonPink else Color.White, CircleShape)
            )
        }

        // When locked on target: show auto-aim bracket and enemy elevation/health
        if (autoAimTarget != null) {
            val enemy = autoAimTarget.enemy
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-45).dp)
                    .background(CyberSurface.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .border(1.dp, NeonPink, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LOCK: ${enemy.type.title.uppercase()}",
                        color = NeonPink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    // Health bar
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF330011))
                    ) {
                        val hpPercent = (enemy.health / enemy.maxHealth).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(hpPercent)
                                .background(NeonPink)
                        )
                    }
                    Text(
                        text = "DIST: ${autoAimTarget.distance.toInt()}m | ELEV: ${if (autoAimTarget.verticalPitchOffset > 0) "+" else ""}${(autoAimTarget.verticalPitchOffset * 10).toInt()}",
                        color = CyberTextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun HudTopBar(
    engine: GameEngine,
    onPauseClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Health & Shield Bars
        Column(modifier = Modifier.width(180.dp)) {
            // Shield Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SHD",
                    color = ShieldCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(28.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberSurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((engine.shield / engine.maxShield).coerceIn(0f, 1f))
                            .background(ShieldCyan)
                    )
                }
                Text(
                    text = "${engine.shield.toInt()}",
                    color = ShieldCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Health Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "HP",
                    color = HealthRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(28.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberSurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((engine.health / engine.maxHealth).coerceIn(0f, 1f))
                            .background(HealthRed)
                    )
                }
                Text(
                    text = "${engine.health.toInt()}",
                    color = HealthRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        // Center: Score & Combo Multiplier
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SCORE: ${engine.score}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            if (engine.comboCounter > 0) {
                Text(
                    text = "COMBO x${engine.comboCounter + 1} 🔥",
                    color = NeonYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Right: Mini-Map Radar & Pause Button
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Mini-Map Canvas (2D Sector Layout)
            MiniMapRadar(engine = engine)

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onPauseClicked,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, CircleShape)
                    .testTag("pause_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause Game",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniMapRadar(engine: GameEngine) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(CyberDark.copy(alpha = 0.85f))
            .border(1.dp, NeonCyan, CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val map = engine.map
            val mapW = map.width
            val mapH = map.height
            val scaleX = size.width / mapW
            val scaleY = size.height / mapH

            // Draw wall sectors
            for (y in 0 until mapH) {
                for (x in 0 until mapW) {
                    if (map.isWall(x, y)) {
                        drawRect(
                            color = Color(0xFF1E284A),
                            topLeft = Offset(x * scaleX, y * scaleY),
                            size = androidx.compose.ui.geometry.Size(scaleX, scaleY)
                        )
                    }
                }
            }

            // Draw Exit Portal
            drawCircle(
                color = NeonGreen,
                radius = 3.5f,
                center = Offset(map.exitX * scaleX, map.exitY * scaleY)
            )

            // Draw Enemies
            for (enemy in engine.enemies) {
                if (!enemy.isDead) {
                    drawCircle(
                        color = HealthRed,
                        radius = 2.5f,
                        center = Offset(enemy.x * scaleX, enemy.y * scaleY)
                    )
                }
            }

            // Draw Player Position & Direction Arrow
            val px = engine.posX * scaleX
            val py = engine.posY * scaleY
            drawCircle(color = NeonCyan, radius = 3.5f, center = Offset(px, py))
            drawLine(
                color = NeonCyan,
                start = Offset(px, py),
                end = Offset(px + cos(engine.angle) * 7f, py + sin(engine.angle) * 7f),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun SpeedometerBadge(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CyberSurface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = SpeedGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${engine.speedKmh.toInt()} KM/H",
                color = SpeedGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        // Parkour status label
        val statusText = when {
            engine.isWallRunning -> "⚡ WALL RUNNING"
            engine.isSliding -> "🛹 SLIDING"
            engine.isDashing -> "💨 AIR DASH"
            !engine.isGrounded -> "🦘 AIRBORNE"
            engine.momentumMultiplier > 1.3f -> "🔥 MOMENTUM x${"%.1f".format(engine.momentumMultiplier)}"
            else -> "🏃 SPRINT"
        }
        val statusColor = when {
            engine.isWallRunning -> NeonCyan
            engine.isSliding -> NeonPink
            engine.isDashing -> NeonYellow
            else -> CyberTextSecondary
        }

        Text(
            text = statusText,
            color = statusColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TouchControlsLayer(
    engine: GameEngine,
    onMove: (forward: Float, strafe: Float) -> Unit,
    onFireHeld: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Left Virtual Joystick for Movement & Strafing
        VirtualJoystick(
            onMove = onMove,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(140.dp)
                .testTag("virtual_joystick")
        )

        // Bottom Center: Weapon Selector Carousel
        WeaponCarousel(
            engine = engine,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )

        // Right Side: Action Buttons (Jump, Slide, Dash, Fire)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Slide / Crouch Button
            ActionButton(
                label = "SLIDE",
                icon = Icons.Default.VerticalAlignBottom,
                color = NeonPink,
                onClick = { engine.onSlidePressed() },
                testTag = "slide_button"
            )

            // Dash Button
            ActionButton(
                label = "DASH",
                icon = Icons.Default.FastForward,
                color = NeonYellow,
                onClick = { engine.onDashPressed() },
                testTag = "dash_button"
            )

            // Jump / Wall-Jump Button
            ActionButton(
                label = "JUMP",
                icon = Icons.Default.ArrowUpward,
                color = NeonCyan,
                size = 72.dp,
                onClick = { engine.onJumpPressed() },
                testTag = "jump_button"
            )

            // Fire Button
            FireButton(
                onFireHeld = onFireHeld,
                testTag = "fire_button"
            )
        }
    }
}

@Composable
private fun VirtualJoystick(
    onMove: (forward: Float, strafe: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 55f

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(CyberSurface.copy(alpha = 0.65f))
            .border(2.dp, CyberBorder, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val newOffset = thumbOffset + dragAmount
                    val dist = hypot(newOffset.x, newOffset.y)
                    val clamped = if (dist > maxRadius) {
                        Offset(newOffset.x / dist * maxRadius, newOffset.y / dist * maxRadius)
                    } else {
                        newOffset
                    }
                    thumbOffset = clamped
                    // Normalize: Y negative is forward, X positive is strafe right
                    val forward = -clamped.y / maxRadius
                    val strafe = clamped.x / maxRadius
                    onMove(forward, strafe)
                }
            }
    ) {
        // Center Thumb Knob
        Box(
            modifier = Modifier
                .size(46.dp)
                .offset { IntOffset(thumbOffset.x.toInt(), thumbOffset.y.toInt()) }
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonCyan, Color(0xFF007799))
                    )
                )
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun WeaponCarousel(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(CyberSurface.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val weapons = listOf(
            WeaponType.BLASTER to "PULSE",
            WeaponType.SHOTGUN to "FLAK",
            WeaponType.RAILGUN to "RAIL",
            WeaponType.KATANA to "BLADE"
        )
        for ((w, name) in weapons) {
            val isSelected = engine.currentWeapon == w
            val ammo = engine.ammoMap[w] ?: 0
            val ammoStr = if (w == WeaponType.BLASTER || w == WeaponType.KATANA) "∞" else "$ammo"

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) CyberSurfaceVariant else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) NeonCyan else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .pointerInput(w) {
                        detectDragGestures(onDragStart = { engine.switchWeapon(w) }) { _, _ -> }
                    }
                    .testTag("weapon_btn_${w.name.lowercase()}")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = name,
                        color = if (isSelected) NeonCyan else CyberTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = ammoStr,
                        color = if (isSelected) Color.White else CyberTextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 58.dp,
    testTag: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(CyberSurface.copy(alpha = 0.85f))
                .border(2.dp, color, CircleShape)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(size * 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun FireButton(
    onFireHeld: (Boolean) -> Unit,
    testTag: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonPink, Color(0xFF990033))
                    )
                )
                .border(2.5.dp, Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onFireHeld(true) },
                        onDragEnd = { onFireHeld(false) },
                        onDragCancel = { onFireHeld(false) }
                    ) { _, _ -> }
                }
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = "Fire Weapon",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "FIRE",
            color = NeonPink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
