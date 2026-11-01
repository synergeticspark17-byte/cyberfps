package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.EnemyType
import com.example.model.PickupType
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Procedural stylized 2D bitmap generator for retro 2.5D FPS engine.
 * Generates walls, sector surfaces, and flat 2D billboard sprites.
 */
object TextureStore {
    const val TEX_SIZE = 64

    // Wall textures: IntArray of size TEX_SIZE * TEX_SIZE (ARGB)
    val wallTextures = mutableMapOf<Int, IntArray>()

    // Floor and ceiling textures
    val floorTextures = mutableMapOf<Int, IntArray>()
    val ceilingTextures = mutableMapOf<Int, IntArray>()

    // Billboard sprite bitmaps for 2D enemies and pickups
    val enemySprites = mutableMapOf<EnemyType, Array<IntArray>>()
    val pickupSprites = mutableMapOf<PickupType, IntArray>()
    val explosionFrames = mutableListOf<IntArray>()

    init {
        generateWallTextures()
        generateFloorCeilingTextures()
        generateEnemySprites()
        generatePickupSprites()
        generateExplosionSprites()
    }

    private fun generateWallTextures() {
        // Wall 1: Cyber Panel (Dark Slate with glowing Neon Cyan Circuit lines)
        wallTextures[1] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val isBorder = x == 0 || x == TEX_SIZE - 1 || y == 0 || y == TEX_SIZE - 1
            val isBolt = (x == 3 || x == TEX_SIZE - 4) && (y == 3 || y == TEX_SIZE - 4)
            val isCircuitLine = (y == 20 && x in 8..56) || (x == 32 && y in 20..50) || (y == 50 && x in 32..54) || (x == 54 && y in 40..50)
            val isCircuitDot = (x == 54 && y == 40) || (x == 8 && y == 20)

            when {
                isCircuitDot -> 0xFFFFFFFF.toInt()
                isCircuitLine -> 0xFF00F5FF.toInt()
                isBorder -> 0xFF00B0FF.toInt()
                isBolt -> 0xFF708090.toInt()
                (x + y) % 16 == 0 -> 0xFF141A2E.toInt()
                else -> 0xFF0C101F.toInt()
            }
        }

        // Wall 2: Neon Holographic Hex Grid (Hot Pink / Magenta)
        wallTextures[2] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val hex = ((x * 2 + y) % 16 == 0) || ((x * 2 - y + 64) % 16 == 0) || (y % 16 == 0)
            val isEdge = x < 2 || x > TEX_SIZE - 3 || y < 2 || y > TEX_SIZE - 3

            when {
                isEdge -> 0xFFFF007F.toInt()
                hex -> 0xFFFF2A9D.toInt()
                (x % 8 == 0 && y % 8 == 0) -> 0xFF350028.toInt()
                else -> 0xFF12031B.toInt()
            }
        }

        // Wall 3: Industrial Reactor Conduit (Hazard Stripes & Amber Energy Tubes)
        wallTextures[3] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val isTube = x in 24..39
            val isTubeCenter = x in 28..35
            val isHazard = y in 0..7 || y in (TEX_SIZE - 8) until TEX_SIZE
            val hazardStripe = (x + y) % 12 < 6

            when {
                isHazard && hazardStripe -> 0xFFFFD700.toInt()
                isHazard -> 0xFF111111.toInt()
                isTubeCenter -> 0xFFFF9900.toInt()
                isTube -> 0xFFFF5500.toInt()
                x == 23 || x == 40 -> 0xFF888888.toInt()
                else -> 0xFF1E2128.toInt()
            }
        }

        // Wall 4: Glass & Digital Shield Wall (Cyan Shimmer)
        wallTextures[4] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val diag = (x + y) % 32 in 14..18
            val grid = x % 16 == 0 || y % 16 == 0

            when {
                grid -> 0xFF00FFFF.toInt()
                diag -> 0x8800D5FF.toInt()
                else -> 0x33003050.toInt()
            }
        }

        // Wall 5: Reactor Core (Pulsing High Voltage)
        wallTextures[5] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val dist = sqrt(((x - 32) * (x - 32) + (y - 32) * (y - 32)).toDouble()).toInt()
            when {
                dist < 10 -> 0xFFFFFFFF.toInt()
                dist < 22 -> 0xFFFF0055.toInt()
                dist < 28 -> 0xFF770022.toInt()
                x % 8 == 0 || y % 8 == 0 -> 0xFF330011.toInt()
                else -> 0xFF150009.toInt()
            }
        }

        // Wall 6: Finish Portal
        wallTextures[6] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val dist = sqrt(((x - 32) * (x - 32) + (y - 32) * (y - 32)).toDouble()).toInt()
            val ring = (dist + (x / 4)) % 8 < 4
            when {
                dist < 26 && ring -> 0xFF00FFCC.toInt()
                dist < 26 -> 0xFF005544.toInt()
                else -> 0xFF00221A.toInt()
            }
        }
    }

    private fun generateFloorCeilingTextures() {
        // Floor 0: Cyber Grid (Dark floor with faint glowing grid lines)
        floorTextures[0] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val grid = x % 16 == 0 || y % 16 == 0
            val dot = (x % 16 == 0) && (y % 16 == 0)
            when {
                dot -> 0xFF00F5FF.toInt()
                grid -> 0xFF1B2A4A.toInt()
                else -> 0xFF080B14.toInt()
            }
        }

        // Floor 1: Elevated Catwalk Grating
        floorTextures[1] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val grate = (x % 4 == 0) || (y % 4 == 0)
            val border = x < 2 || x > TEX_SIZE - 3 || y < 2 || y > TEX_SIZE - 3
            when {
                border -> 0xFFFFD700.toInt()
                grate -> 0xFF3A4454.toInt()
                else -> 0xFF10141D.toInt()
            }
        }

        // Floor 2: Toxic Acid Hazard Pit (Vibrant bubbling green)
        floorTextures[2] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val bubble1 = (x - 20) * (x - 20) + (y - 20) * (y - 20) < 36
            val bubble2 = (x - 45) * (x - 45) + (y - 40) * (y - 40) < 49
            when {
                bubble1 || bubble2 -> 0xFFB3FF00.toInt()
                (x + y) % 8 == 0 -> 0xFF22AA00.toInt()
                else -> 0xFF0D5500.toInt()
            }
        }

        // Floor 3: Speed Boost Pad (Vibrant Neon Forward Arrows)
        floorTextures[3] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val arrow = abs(x - 32) < (y % 20) && (y % 20 < 14)
            when {
                arrow -> 0xFF00FFFF.toInt()
                x in 4..6 || x in 57..59 -> 0xFFFF007F.toInt()
                else -> 0xFF0A0F20.toInt()
            }
        }

        // Ceiling 0: Digital Sky / Ceiling
        ceilingTextures[0] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val star = (x * 37 + y * 19) % 197 == 0
            when {
                star -> 0xFF88CCFF.toInt()
                y % 32 == 0 -> 0xFF140D2B.toInt()
                else -> 0xFF05030E.toInt()
            }
        }
    }

    private fun generateEnemySprites() {
        // Cyber Drone (Floating sphere, glowing eye, rotor thrusters)
        val droneFrames = Array(2) { frame ->
            IntArray(TEX_SIZE * TEX_SIZE) { i ->
                val x = i % TEX_SIZE
                val y = i / TEX_SIZE
                val dx = x - 32
                val dy = y - 32
                val distSq = dx * dx + dy * dy

                // Thruster flicker at bottom
                val isThruster = dy in 20..28 && abs(dx) in 8..18
                val thrusterGlow = isThruster && ((frame == 0 && y % 2 == 0) || (frame == 1 && y % 2 == 1))

                // Core eye
                val isEye = dx * dx + (dy + 2) * (dy + 2) < 25

                when {
                    isEye -> 0xFFFF0033.toInt()
                    distSq < 256 -> { // Main metallic sphere
                        val shade = 0xFF2B3A4C.toInt()
                        if (dx * dx + dy * dy < 144) 0xFF47607D.toInt() else shade
                    }
                    // Outer armor ring
                    distSq in 256..380 -> 0xFF1E2835.toInt()
                    thrusterGlow -> 0xFF00F5FF.toInt()
                    else -> 0x00000000 // Transparent
                }
            }
        }
        enemySprites[EnemyType.CYBER_DRONE] = droneFrames

        // Heavy Mech (Armored walking enforcer, shoulder cannons, red eye slit)
        val mechFrames = Array(2) { frame ->
            IntArray(TEX_SIZE * TEX_SIZE) { i ->
                val x = i % TEX_SIZE
                val y = i / TEX_SIZE
                val legOffset = if (frame == 0) 0 else 3

                val isTorso = x in 18..45 && y in 14..36
                val isCannonL = x in 10..17 && y in 8..24
                val isCannonR = x in 46..53 && y in 8..24
                val isVisor = x in 24..39 && y in 18..22
                val isLegL = x in 16..24 && y in 37..(56 + legOffset)
                val isLegR = x in 39..47 && y in 37..(56 - legOffset)

                when {
                    isVisor -> 0xFFFF0055.toInt()
                    isCannonL || isCannonR -> 0xFFFF9900.toInt()
                    isTorso -> {
                        if (x in 26..37 && y in 25..32) 0xFF2A2E3D.toInt() else 0xFF434A5F.toInt()
                    }
                    isLegL || isLegR -> 0xFF2D3240.toInt()
                    else -> 0x00000000
                }
            }
        }
        enemySprites[EnemyType.HEAVY_MECH] = mechFrames

        // Neon Ninja (Agile cyber operative with glowing visor and neon katana)
        val ninjaFrames = Array(2) { frame ->
            IntArray(TEX_SIZE * TEX_SIZE) { i ->
                val x = i % TEX_SIZE
                val y = i / TEX_SIZE
                val bladeX = if (frame == 0) 48 else 50
                val isHead = (x - 32) * (x - 32) + (y - 14) * (y - 14) < 40
                val isVisor = isHead && y in 13..15 && x in 30..36
                val isTorso = x in 26..38 && y in 20..38
                val isBlade = x in bladeX..(bladeX + 3) && y in 10..52
                val isLegs = (x in 24..30 || x in 34..40) && y in 39..58

                when {
                    isVisor -> 0xFF00FFCC.toInt()
                    isBlade -> 0xFFFF007F.toInt()
                    isHead || isTorso -> 0xFF141724.toInt()
                    isLegs -> 0xFF0C0E17.toInt()
                    else -> 0x00000000
                }
            }
        }
        enemySprites[EnemyType.NEON_NINJA] = ninjaFrames

        // Pulse Turret (Tripod base, heavy swivel laser barrel)
        val turretFrames = Array(1) {
            IntArray(TEX_SIZE * TEX_SIZE) { i ->
                val x = i % TEX_SIZE
                val y = i / TEX_SIZE
                val isBarrel = x in 28..35 && y in 12..28
                val isLaserGlow = isBarrel && y in 12..16
                val isHead = (x - 32) * (x - 32) + (y - 30) * (y - 30) < 100
                val isBase = y in 42..56 && (abs(x - 32) < (y - 40) * 2)

                when {
                    isLaserGlow -> 0xFFFF2200.toInt()
                    isBarrel -> 0xFF888888.toInt()
                    isHead -> 0xFF354050.toInt()
                    isBase -> 0xFF1F2630.toInt()
                    else -> 0x00000000
                }
            }
        }
        enemySprites[EnemyType.SNIPER_TURRET] = turretFrames
    }

    private fun generatePickupSprites() {
        // Health Pack (Green nanite medical cross in spinning diamond)
        pickupSprites[PickupType.HEALTH_PACK] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val isCross = (x in 28..35 && y in 20..43) || (y in 28..35 && x in 20..43)
            val isBox = abs(x - 32) + abs(y - 32) < 26

            when {
                isCross -> 0xFF00FF66.toInt()
                isBox && (abs(x - 32) + abs(y - 32) in 23..26) -> 0xFFFFFFFF.toInt()
                isBox -> 0xAA003318.toInt()
                else -> 0x00000000
            }
        }

        // Shield Cell (Cyan hexagonal energy core)
        pickupSprites[PickupType.SHIELD_CELL] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val dist = abs(x - 32) + abs(y - 32)
            val isCore = dist < 12
            val isRing = dist in 16..22

            when {
                isCore -> 0xFFFFFFFF.toInt()
                isRing -> 0xFF00F5FF.toInt()
                dist < 22 -> 0x88003A50.toInt()
                else -> 0x00000000
            }
        }

        // Ammo Box (Amber glowing battery pack)
        pickupSprites[PickupType.AMMO_BOX] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val isBody = x in 22..41 && y in 22..46
            val isBolt = (y in 26..42) && (abs(x - 32) < 4)

            when {
                isBolt -> 0xFFFFFFFF.toInt()
                isBody -> 0xFFFFD700.toInt()
                x in 26..37 && y in 18..21 -> 0xFFCC9900.toInt() // Terminals
                else -> 0x00000000
            }
        }

        // Overdrive Core (Hot Magenta Pulsing Energy Star)
        pickupSprites[PickupType.OVERDRIVE] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val dx = abs(x - 32)
            val dy = abs(y - 32)
            val star = (dx * dy) < 48 && (dx + dy < 28)

            when {
                dx + dy < 8 -> 0xFFFFFFFF.toInt()
                star -> 0xFFFF007F.toInt()
                dx + dy < 20 -> 0xAA660033.toInt()
                else -> 0x00000000
            }
        }

        // Cyber Orb (Violet Shard)
        pickupSprites[PickupType.CYBER_ORB] = IntArray(TEX_SIZE * TEX_SIZE) { i ->
            val x = i % TEX_SIZE
            val y = i / TEX_SIZE
            val shard = abs(x - 32) * 2 + abs(y - 32) < 32
            when {
                abs(x - 32) < 3 && abs(y - 32) < 10 -> 0xFFFFFFFF.toInt()
                shard -> 0xFFD600FF.toInt()
                else -> 0x00000000
            }
        }
    }

    private fun generateExplosionSprites() {
        for (f in 0 until 4) {
            val radius = 10 + f * 7
            val pixels = IntArray(TEX_SIZE * TEX_SIZE) { i ->
                val x = i % TEX_SIZE
                val y = i / TEX_SIZE
                val dist = sqrt(((x - 32) * (x - 32) + (y - 32) * (y - 32)).toDouble()).toInt()
                val noise = (x * 13 + y * 29) % 5
                when {
                    dist < radius / 2 -> 0xFFFFFFFF.toInt()
                    dist < radius -> if ((dist + noise) % 2 == 0) 0xFFFFCC00.toInt() else 0xFFFF3300.toInt()
                    dist < radius + 6 && noise < 2 -> 0xFFFF0055.toInt()
                    else -> 0x00000000
                }
            }
            explosionFrames.add(pixels)
        }
    }
}
