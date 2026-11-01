package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.audio.AudioEngine
import com.example.engine.GameEngine
import com.example.ui.*
import com.example.ui.theme.CyberDashTheme
import com.example.ui.theme.CyberDarker

enum class AppScreen {
    MENU,
    GAME,
    LEVEL_SELECT,
    UPGRADES
}

class MainActivity : ComponentActivity() {

    private val audioEngine by lazy { AudioEngine() }
    private val gameEngine by lazy { GameEngine(audioEngine) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CyberDashTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberDarker
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreen.MENU) }
                    var showPauseDialog by remember { mutableStateOf(false) }
                    var showSettingsDialog by remember { mutableStateOf(false) }
                    var showTutorialDialog by remember { mutableStateOf(false) }
                    var showGameOverDialog by remember { mutableStateOf(false) }
                    var showVictoryDialog by remember { mutableStateOf(false) }

                    // Persistent game progress state
                    var highScore by remember { mutableIntStateOf(0) }
                    var totalShards by remember { mutableIntStateOf(10) } // Initial bonus shards
                    var armorLevel by remember { mutableIntStateOf(1) }
                    var dashLevel by remember { mutableIntStateOf(1) }
                    var damageLevel by remember { mutableIntStateOf(1) }
                    var selectedDistrict by remember { mutableIntStateOf(1) }

                    // Start dynamic BGM
                    LaunchedEffect(Unit) {
                        audioEngine.startDynamicBgm()
                    }

                    // Handle Back Navigation cleanly
                    BackHandler(enabled = currentScreen != AppScreen.MENU) {
                        if (currentScreen == AppScreen.GAME) {
                            showPauseDialog = true
                        } else {
                            currentScreen = AppScreen.MENU
                        }
                    }

                    when (currentScreen) {
                        AppScreen.MENU -> {
                            MainMenuScreen(
                                highScore = highScore,
                                shards = totalShards,
                                onStartGame = {
                                    gameEngine.loadLevel(selectedDistrict)
                                    // Apply upgrades
                                    gameEngine.maxHealth = 100f + (armorLevel - 1) * 25f
                                    gameEngine.health = gameEngine.maxHealth
                                    currentScreen = AppScreen.GAME
                                },
                                onOpenLevelSelect = { currentScreen = AppScreen.LEVEL_SELECT },
                                onOpenUpgrades = { currentScreen = AppScreen.UPGRADES },
                                onOpenTutorial = { showTutorialDialog = true },
                                onOpenSettings = { showSettingsDialog = true }
                            )
                        }

                        AppScreen.LEVEL_SELECT -> {
                            LevelSelectScreen(
                                currentLevel = selectedDistrict,
                                onSelectLevel = { level ->
                                    selectedDistrict = level
                                    gameEngine.loadLevel(level)
                                    gameEngine.maxHealth = 100f + (armorLevel - 1) * 25f
                                    gameEngine.health = gameEngine.maxHealth
                                    currentScreen = AppScreen.GAME
                                },
                                onBack = { currentScreen = AppScreen.MENU }
                            )
                        }

                        AppScreen.UPGRADES -> {
                            UpgradesScreen(
                                shards = totalShards,
                                armorLevel = armorLevel,
                                dashLevel = dashLevel,
                                damageLevel = damageLevel,
                                onUpgradeArmor = {
                                    val cost = (armorLevel + 1) * 3
                                    if (totalShards >= cost && armorLevel < 5) {
                                        totalShards -= cost
                                        armorLevel++
                                        audioEngine.playPickup()
                                    }
                                },
                                onUpgradeDash = {
                                    val cost = (dashLevel + 1) * 3
                                    if (totalShards >= cost && dashLevel < 5) {
                                        totalShards -= cost
                                        dashLevel++
                                        audioEngine.playPickup()
                                    }
                                },
                                onUpgradeDamage = {
                                    val cost = (damageLevel + 1) * 4
                                    if (totalShards >= cost && damageLevel < 5) {
                                        totalShards -= cost
                                        damageLevel++
                                        audioEngine.playPickup()
                                    }
                                },
                                onBack = { currentScreen = AppScreen.MENU }
                            )
                        }

                        AppScreen.GAME -> {
                            GameScreen(
                                engine = gameEngine,
                                onPauseClicked = { showPauseDialog = true },
                                onLevelCompleted = {
                                    if (gameEngine.score > highScore) highScore = gameEngine.score
                                    totalShards += gameEngine.cyberOrbs
                                    showVictoryDialog = true
                                },
                                onGameOver = {
                                    if (gameEngine.score > highScore) highScore = gameEngine.score
                                    totalShards += gameEngine.cyberOrbs
                                    showGameOverDialog = true
                                }
                            )
                        }
                    }

                    // Dialogs
                    if (showPauseDialog) {
                        PauseDialog(
                            onResume = { showPauseDialog = false },
                            onRestart = {
                                showPauseDialog = false
                                gameEngine.loadLevel(selectedDistrict)
                            },
                            onExitToMenu = {
                                showPauseDialog = false
                                currentScreen = AppScreen.MENU
                            }
                        )
                    }

                    if (showSettingsDialog) {
                        SettingsDialog(
                            audioEngine = audioEngine,
                            onDismiss = { showSettingsDialog = false }
                        )
                    }

                    if (showTutorialDialog) {
                        TutorialDialog(
                            onDismiss = { showTutorialDialog = false }
                        )
                    }

                    if (showGameOverDialog) {
                        GameOverDialog(
                            score = gameEngine.score,
                            shards = gameEngine.cyberOrbs,
                            onRestart = {
                                showGameOverDialog = false
                                gameEngine.loadLevel(selectedDistrict)
                            },
                            onExitToMenu = {
                                showGameOverDialog = false
                                currentScreen = AppScreen.MENU
                            }
                        )
                    }

                    if (showVictoryDialog) {
                        VictoryDialog(
                            score = gameEngine.score,
                            shards = gameEngine.cyberOrbs,
                            hasNextLevel = selectedDistrict < 3,
                            onNextLevel = {
                                showVictoryDialog = false
                                selectedDistrict++
                                gameEngine.loadLevel(selectedDistrict)
                            },
                            onExitToMenu = {
                                showVictoryDialog = false
                                currentScreen = AppScreen.MENU
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (audioEngine.musicEnabled) {
            audioEngine.startDynamicBgm()
        }
    }

    override fun onPause() {
        super.onPause()
        audioEngine.stopDynamicBgm()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioEngine.stopDynamicBgm()
    }
}
