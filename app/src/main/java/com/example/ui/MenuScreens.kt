package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngine
import com.example.engine.GameEngine
import com.example.ui.theme.*

@Composable
fun MainMenuScreen(
    highScore: Int,
    shards: Int,
    onStartGame: () -> Unit,
    onOpenLevelSelect: () -> Unit,
    onOpenUpgrades: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "titlePulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CyberDarker, Color(0xFF100B22), CyberDarker)
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 540.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stylized Title
            Text(
                text = "CYBERDASH",
                color = NeonCyan,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp
            )
            Text(
                text = "RETRO 2.5D PARKOUR FPS",
                color = NeonPink.copy(alpha = glowAlpha),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // High Score & Shards stats
            Row(
                modifier = Modifier
                    .background(CyberSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SpeedGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("HIGH SCORE: $highScore", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Diamond, contentDescription = null, tint = NeonMagenta, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SHARDS: $shards", color = NeonMagenta, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Start Button
            Button(
                onClick = onStartGame,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_game_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ENTER RUN", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenLevelSelect,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonCyan, Color(0xFF0088CC)))),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("mission_select_button")
                ) {
                    Text("DISTRICTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                OutlinedButton(
                    onClick = onOpenUpgrades,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonMagenta),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonMagenta, Color(0xFF8800CC)))),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("upgrade_shop_button")
                ) {
                    Text("UPGRADES", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                OutlinedButton(
                    onClick = onOpenTutorial,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonYellow),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonYellow, Color(0xFFCCAA00)))),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("tutorial_button")
                ) {
                    Text("PARKOUR GUIDE", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Settings link
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("settings_button")
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = CyberTextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SETTINGS & AUDIO", color = CyberTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun LevelSelectScreen(
    currentLevel: Int,
    onSelectLevel: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levels = listOf(
        Triple(1, "District 01: Neon Alley", "Wall-running tutorial, drone patrols, rooftop slide vents"),
        Triple(2, "District 02: Reactor Core", "High-voltage catwalks, acid hazard chasms, heavy mechs"),
        Triple(3, "District 03: Overdrive Gauntlet", "Endless jump towers, multi-tier turret crossfire, max speed")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarker)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
            }
            Text(
                text = "SELECT DISTRICT",
                color = NeonCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            for ((num, title, desc) in levels) {
                val isSelected = currentLevel == num
                Card(
                    onClick = { onSelectLevel(num) },
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) CyberSurfaceVariant else CyberSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(if (isSelected) listOf(NeonCyan, NeonPink) else listOf(CyberBorder, CyberBorder))
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) NeonCyan else CyberSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "0$num",
                                color = if (isSelected) CyberDarker else Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                color = CyberTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isSelected) NeonPink else CyberTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradesScreen(
    shards: Int,
    armorLevel: Int,
    dashLevel: Int,
    damageLevel: Int,
    onUpgradeArmor: () -> Unit,
    onUpgradeDash: () -> Unit,
    onUpgradeDamage: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarker)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                }
                Text(
                    text = "CYBER AUGMENTS",
                    color = NeonMagenta,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(CyberSurface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Diamond, contentDescription = null, tint = NeonMagenta, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$shards SHARDS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            UpgradeCard(
                title = "NANITE REINFORCEMENT",
                desc = "+25 Max Health & Faster Shield Regeneration",
                level = armorLevel,
                cost = (armorLevel + 1) * 3,
                canAfford = shards >= (armorLevel + 1) * 3,
                onBuy = onUpgradeArmor
            )

            UpgradeCard(
                title = "KINETIC THRUSTER",
                desc = "Air Dash Cooldown reduced by 20% & extra distance",
                level = dashLevel,
                cost = (dashLevel + 1) * 3,
                canAfford = shards >= (dashLevel + 1) * 3,
                onBuy = onUpgradeDash
            )

            UpgradeCard(
                title = "PLASMA OVERCLOCK",
                desc = "+20% Damage on all Plasma & Railgun weapon projectiles",
                level = damageLevel,
                cost = (damageLevel + 1) * 4,
                canAfford = shards >= (damageLevel + 1) * 4,
                onBuy = onUpgradeDamage
            )
        }
    }
}

@Composable
private fun UpgradeCard(
    title: String,
    desc: String,
    level: Int,
    cost: Int,
    canAfford: Boolean,
    onBuy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyberBorder, CyberBorder))),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, color = CyberTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("RANK: $level / 5", color = SpeedGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = onBuy,
                enabled = canAfford && level < 5,
                colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (level >= 5) "MAX" else "BUY ($cost)", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "PARKOUR & COMBAT MANUAL",
                color = NeonCyan,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TutorialItem("⚡ WALL RUNNING", "Jump towards any solid wall while sprinting forward to latch on and wall-run! Press JUMP while wall-running for a high-velocity Wall-Jump burst.")
                TutorialItem("🛹 SLIDING", "Tap SLIDE while sprinting to slide underneath low ceiling vents and laser barriers at high speed.")
                TutorialItem("💨 AIR DASH", "Tap DASH to burst forward with brief invulnerability frames, dodging enemy missile fire.")
                TutorialItem("🎯 SMART AUTO-AIM", "The retro 2.5D engine has no vertical looking up/down! Center enemies in your reticle cone: the smart targeting bracket locks on and automatically compensates elevation angle to shoot drones above or turrets below.")
                TutorialItem("🦘 BUNNYHOPPING", "Press JUMP right as you touch down from a leap to preserve and multiply your momentum up to 2.5x!")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("UNDERSTOOD", color = CyberDarker, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
private fun TutorialItem(title: String, desc: String) {
    Column {
        Text(title, color = NeonPink, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(desc, color = CyberTextPrimary, fontSize = 11.sp)
    }
}

@Composable
fun SettingsDialog(
    audioEngine: AudioEngine,
    onDismiss: () -> Unit
) {
    var soundOn by remember { mutableStateOf(audioEngine.soundEnabled) }
    var musicOn by remember { mutableStateOf(audioEngine.musicEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text("AUDIO & SETTINGS", color = NeonCyan, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sound Effects (SFX)", color = Color.White, fontFamily = FontFamily.Monospace)
                    Switch(
                        checked = soundOn,
                        onCheckedChange = {
                            soundOn = it
                            audioEngine.soundEnabled = it
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dynamic Synthwave BGM", color = Color.White, fontFamily = FontFamily.Monospace)
                    Switch(
                        checked = musicOn,
                        onCheckedChange = {
                            musicOn = it
                            audioEngine.musicEnabled = it
                            if (it) audioEngine.startDynamicBgm() else audioEngine.stopDynamicBgm()
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                Text("SAVE", color = CyberDarker, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
fun PauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onResume,
        containerColor = CyberSurface,
        title = {
            Text("SIMULATION PAUSED", color = NeonCyan, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("RESUME RUN", color = CyberDarker, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                OutlinedButton(
                    onClick = onRestart,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("RESTART DISTRICT", fontFamily = FontFamily.Monospace)
                }
                OutlinedButton(
                    onClick = onExitToMenu,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HealthRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ABORT TO HUB", fontFamily = FontFamily.Monospace)
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun GameOverDialog(
    score: Int,
    shards: Int,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onRestart,
        containerColor = CyberSurface,
        title = {
            Text("CORE COMPROMISED", color = HealthRed, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("RUN TERMINATED", color = CyberTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text("FINAL SCORE: $score", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                Text("DATA SHARDS RECOVERED: $shards", color = NeonMagenta, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = {
            Button(onClick = onRestart, colors = ButtonDefaults.buttonColors(containerColor = NeonPink)) {
                Text("RETRY", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onExitToMenu) {
                Text("HUB", color = CyberTextSecondary, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
fun VictoryDialog(
    score: Int,
    shards: Int,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onExitToMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onExitToMenu,
        containerColor = CyberSurface,
        title = {
            Text("DISTRICT CLEARED! 🏆", color = NeonGreen, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("EXTRACTION PORTAL REACHED", color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text("SCORE: $score", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                Text("DATA SHARDS: +$shards", color = NeonMagenta, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = {
            if (hasNextLevel) {
                Button(onClick = onNextLevel, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                    Text("NEXT DISTRICT", color = CyberDarker, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            } else {
                Button(onClick = onExitToMenu, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)) {
                    Text("COMPLETE", color = CyberDarker, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onExitToMenu) {
                Text("HUB", color = CyberTextSecondary, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
