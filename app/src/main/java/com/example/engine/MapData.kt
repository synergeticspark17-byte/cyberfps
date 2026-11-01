package com.example.engine

import com.example.model.Enemy
import com.example.model.EnemyType
import com.example.model.Pickup
import com.example.model.PickupType

data class MapSector(
    val wallType: Int, // 0 = empty space, >0 = solid wall
    val floorHeight: Float = 0.0f, // 0.0 standard, 0.6 elevated platform, -0.6 acid pit
    val ceilingHeight: Float = 1.6f, // 0.8 low vent (must slide!), 1.6 normal, 2.4 open sky
    val floorType: Int = 0, // 0 = cyber grid, 1 = platform, 2 = hazard acid, 3 = boost pad
    val ceilingType: Int = 0,
    val lightLevel: Float = 0.85f
)

/**
 * Wall segment for BSP Tree partitioning.
 */
data class WallSegment(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val wallType: Int,
    val floorHeight: Float,
    val ceilingHeight: Float
) {
    fun sideOfPoint(px: Float, py: Float): Float {
        return (x2 - x1) * (py - y1) - (y2 - y1) * (px - x1)
    }
}

/**
 * Binary Space Partitioning (BSP) Tree Node.
 * Allows strict front-to-back traversal of the 2D sector map.
 */
class BspNode(
    val partition: WallSegment,
    val segments: List<WallSegment>,
    var front: BspNode? = null,
    var back: BspNode? = null
)

class GameMap(
    val title: String,
    val width: Int,
    val height: Int,
    val sectors: Array<Array<MapSector>>,
    val playerStartX: Float,
    val playerStartY: Float,
    val playerStartAngle: Float,
    val initialEnemies: List<Enemy>,
    val initialPickups: List<Pickup>,
    val exitX: Float,
    val exitY: Float
) {
    val bspRoot: BspNode? = buildBspTree()

    fun getSector(x: Int, y: Int): MapSector {
        if (x !in 0 until width || y !in 0 until height) {
            return MapSector(wallType = 1, floorHeight = 0f, ceilingHeight = 1.6f)
        }
        return sectors[y][x]
    }

    fun isWall(x: Int, y: Int): Boolean {
        return getSector(x, y).wallType > 0
    }

    private fun buildBspTree(): BspNode? {
        val allSegments = mutableListOf<WallSegment>()
        // Extract 4 boundary segments per solid wall tile adjacent to empty tile
        for (y in 0 until height) {
            for (x in 0 until width) {
                val sec = sectors[y][x]
                if (sec.wallType > 0) {
                    val fx = x.toFloat()
                    val fy = y.toFloat()
                    // North face
                    if (y > 0 && sectors[y - 1][x].wallType == 0) {
                        allSegments.add(WallSegment(fx, fy, fx + 1f, fy, sec.wallType, sec.floorHeight, sec.ceilingHeight))
                    }
                    // South face
                    if (y < height - 1 && sectors[y + 1][x].wallType == 0) {
                        allSegments.add(WallSegment(fx + 1f, fy + 1f, fx, fy + 1f, sec.wallType, sec.floorHeight, sec.ceilingHeight))
                    }
                    // West face
                    if (x > 0 && sectors[y][x - 1].wallType == 0) {
                        allSegments.add(WallSegment(fx, fy + 1f, fx, fy, sec.wallType, sec.floorHeight, sec.ceilingHeight))
                    }
                    // East face
                    if (x < width - 1 && sectors[y][x + 1].wallType == 0) {
                        allSegments.add(WallSegment(fx + 1f, fy, fx + 1f, fy + 1f, sec.wallType, sec.floorHeight, sec.ceilingHeight))
                    }
                }
            }
        }
        return buildBspNodeRecursive(allSegments, depth = 0)
    }

    private fun buildBspNodeRecursive(segments: List<WallSegment>, depth: Int): BspNode? {
        if (segments.isEmpty() || depth > 12) return null
        val partition = segments[segments.size / 2]
        val frontList = mutableListOf<WallSegment>()
        val backList = mutableListOf<WallSegment>()
        val collinear = mutableListOf<WallSegment>()

        for (seg in segments) {
            val midX = (seg.x1 + seg.x2) * 0.5f
            val midY = (seg.y1 + seg.y2) * 0.5f
            val side = partition.sideOfPoint(midX, midY)
            when {
                side > 0.01f -> frontList.add(seg)
                side < -0.01f -> backList.add(seg)
                else -> collinear.add(seg)
            }
        }

        val node = BspNode(partition, collinear)
        node.front = buildBspNodeRecursive(frontList, depth + 1)
        node.back = buildBspNodeRecursive(backList, depth + 1)
        return node
    }

    /**
     * Front-to-back traversal from player camera position.
     */
    fun traverseBspFrontToBack(camX: Float, camY: Float, onVisit: (WallSegment) -> Boolean) {
        fun traverse(node: BspNode?): Boolean {
            if (node == null) return true
            val side = node.partition.sideOfPoint(camX, camY)
            if (side >= 0) {
                if (!traverse(node.front)) return false
                for (seg in node.segments) {
                    if (!onVisit(seg)) return false
                }
                return traverse(node.back)
            } else {
                if (!traverse(node.back)) return false
                for (seg in node.segments) {
                    if (!onVisit(seg)) return false
                }
                return traverse(node.front)
            }
        }
        traverse(bspRoot)
    }
}

object MapCatalog {

    fun createLevel1(): GameMap {
        val w = 24
        val h = 24
        // Level 1: Neon Canyon - High speed corridors, low vent to slide, platforms to mantle
        val grid = Array(h) { y ->
            Array(w) { x ->
                when {
                    // Outer border
                    x == 0 || y == 0 || x == w - 1 || y == h - 1 -> MapSector(wallType = 1, ceilingHeight = 1.6f)

                    // Central parkour corridor with low slide section at y = 8
                    y == 8 && x in 6..10 -> MapSector(wallType = 0, floorHeight = 0.0f, ceilingHeight = 0.75f, floorType = 3) // Low vent!
                    y == 7 && x in 5..11 -> MapSector(wallType = 2)
                    y == 9 && x in 5..11 -> MapSector(wallType = 2)

                    // Elevated platform at x in 14..18, y in 5..8
                    x in 14..18 && y in 5..8 -> MapSector(wallType = 0, floorHeight = 0.65f, floorType = 1, lightLevel = 1.0f)

                    // Wallrun alleys (corridors with alternating jump walls)
                    y in 12..20 && x == 6 -> MapSector(wallType = 2)
                    y in 14..21 && x == 10 -> MapSector(wallType = 3)
                    y in 12..19 && x == 14 -> MapSector(wallType = 2)

                    // Hazard acid pit in bottom right
                    x in 16..21 && y in 15..20 -> MapSector(wallType = 0, floorHeight = -0.7f, floorType = 2, lightLevel = 0.6f)

                    // Finish room at top right
                    x in 20..22 && y in 2..4 -> MapSector(wallType = 0, floorHeight = 0.2f, floorType = 0)
                    x == 19 && y in 1..4 -> MapSector(wallType = 1)
                    x == 23 && y in 1..4 -> MapSector(wallType = 1)

                    else -> MapSector(wallType = 0, floorHeight = 0.0f, ceilingHeight = 1.6f)
                }
            }
        }

        // Enemies
        val enemies = listOf(
            Enemy(1, EnemyType.CYBER_DRONE, 3.5f, 5.0f, 0.4f, EnemyType.CYBER_DRONE.maxHealth),
            Enemy(2, EnemyType.CYBER_DRONE, 8.0f, 4.0f, 0.5f, EnemyType.CYBER_DRONE.maxHealth),
            Enemy(3, EnemyType.NEON_NINJA, 16.0f, 6.5f, 0.65f, EnemyType.NEON_NINJA.maxHealth),
            Enemy(4, EnemyType.SNIPER_TURRET, 17.5f, 6.5f, 0.65f, EnemyType.SNIPER_TURRET.maxHealth),
            Enemy(5, EnemyType.CYBER_DRONE, 8.0f, 16.0f, 0.4f, EnemyType.CYBER_DRONE.maxHealth),
            Enemy(6, EnemyType.NEON_NINJA, 12.0f, 18.0f, 0.0f, EnemyType.NEON_NINJA.maxHealth),
            Enemy(7, EnemyType.HEAVY_MECH, 21.0f, 3.0f, 0.2f, EnemyType.HEAVY_MECH.maxHealth)
        )

        // Pickups
        val pickups = listOf(
            Pickup(1, PickupType.SHIELD_CELL, 3.5f, 7.5f, 0.0f),
            Pickup(2, PickupType.AMMO_BOX, 8.0f, 8.0f, 0.0f),
            Pickup(3, PickupType.HEALTH_PACK, 15.0f, 6.5f, 0.65f),
            Pickup(4, PickupType.OVERDRIVE, 12.0f, 15.0f, 0.0f),
            Pickup(5, PickupType.CYBER_ORB, 4.5f, 16.0f, 0.0f),
            Pickup(6, PickupType.CYBER_ORB, 8.0f, 19.0f, 0.0f),
            Pickup(7, PickupType.CYBER_ORB, 18.0f, 14.0f, 0.0f),
            Pickup(8, PickupType.HEALTH_PACK, 20.5f, 2.5f, 0.2f)
        )

        return GameMap(
            title = "District 01: Neon Alley",
            width = w,
            height = h,
            sectors = grid,
            playerStartX = 2.5f,
            playerStartY = 2.5f,
            playerStartAngle = 0.0f,
            initialEnemies = enemies,
            initialPickups = pickups,
            exitX = 21.5f,
            exitY = 2.5f
        )
    }

    fun createLevel2(): GameMap {
        val w = 26
        val h = 26
        // Level 2: Reactor Core - Intense vertical jumps, acid corridors, heavy mechs
        val grid = Array(h) { y ->
            Array(w) { x ->
                when {
                    x == 0 || y == 0 || x == w - 1 || y == h - 1 -> MapSector(wallType = 3, ceilingHeight = 2.0f)

                    // Central circular reactor core with catwalks
                    (x - 13) * (x - 13) + (y - 13) * (y - 13) <= 12 -> MapSector(wallType = 5, lightLevel = 1.0f) // Central column
                    (x - 13) * (x - 13) + (y - 13) * (y - 13) in 13..45 -> MapSector(wallType = 0, floorHeight = 0.7f, floorType = 1) // Ring catwalk

                    // Acid moat around central reactor
                    (x - 13) * (x - 13) + (y - 13) * (y - 13) in 46..80 -> MapSector(wallType = 0, floorHeight = -0.8f, floorType = 2)

                    // Outer perimeter walls and vents
                    x in 4..8 && y in 3..6 -> MapSector(wallType = 1)
                    x in 18..22 && y in 3..6 -> MapSector(wallType = 2)
                    x in 4..8 && y in 19..22 -> MapSector(wallType = 2)
                    x in 18..22 && y in 19..22 -> MapSector(wallType = 1)

                    // Low vent tunnel at x = 13, y in 2..5
                    x == 13 && y in 2..5 -> MapSector(wallType = 0, floorHeight = 0.0f, ceilingHeight = 0.72f, floorType = 3)
                    x in 12..14 && y in 2..5 && x != 13 -> MapSector(wallType = 3)

                    else -> MapSector(wallType = 0, floorHeight = 0.0f, ceilingHeight = 1.8f)
                }
            }
        }

        val enemies = listOf(
            Enemy(1, EnemyType.CYBER_DRONE, 5.0f, 8.0f, 0.4f, EnemyType.CYBER_DRONE.maxHealth),
            Enemy(2, EnemyType.SNIPER_TURRET, 13.0f, 9.0f, 0.7f, EnemyType.SNIPER_TURRET.maxHealth),
            Enemy(3, EnemyType.SNIPER_TURRET, 13.0f, 17.0f, 0.7f, EnemyType.SNIPER_TURRET.maxHealth),
            Enemy(4, EnemyType.HEAVY_MECH, 13.0f, 13.0f, 0.7f, EnemyType.HEAVY_MECH.maxHealth),
            Enemy(5, EnemyType.NEON_NINJA, 6.0f, 16.0f, 0.0f, EnemyType.NEON_NINJA.maxHealth),
            Enemy(6, EnemyType.NEON_NINJA, 20.0f, 16.0f, 0.0f, EnemyType.NEON_NINJA.maxHealth),
            Enemy(7, EnemyType.CYBER_DRONE, 18.0f, 8.0f, 0.5f, EnemyType.CYBER_DRONE.maxHealth),
            Enemy(8, EnemyType.HEAVY_MECH, 22.0f, 22.0f, 0.0f, EnemyType.HEAVY_MECH.maxHealth)
        )

        val pickups = listOf(
            Pickup(1, PickupType.AMMO_BOX, 13.0f, 6.0f, 0.0f),
            Pickup(2, PickupType.OVERDRIVE, 13.0f, 13.0f, 0.7f),
            Pickup(3, PickupType.HEALTH_PACK, 9.0f, 13.0f, 0.7f),
            Pickup(4, PickupType.SHIELD_CELL, 17.0f, 13.0f, 0.7f),
            Pickup(5, PickupType.CYBER_ORB, 4.0f, 13.0f, 0.0f),
            Pickup(6, PickupType.CYBER_ORB, 22.0f, 13.0f, 0.0f),
            Pickup(7, PickupType.CYBER_ORB, 13.0f, 21.0f, 0.0f)
        )

        return GameMap(
            title = "District 02: Reactor Core",
            width = w,
            height = h,
            sectors = grid,
            playerStartX = 2.5f,
            playerStartY = 2.5f,
            playerStartAngle = 0.78f,
            initialEnemies = enemies,
            initialPickups = pickups,
            exitX = 23.5f,
            exitY = 23.5f
        )
    }

    fun createLevel3(): GameMap {
        val w = 28
        val h = 28
        // Level 3: Cyber Overdrive (Endless Gauntlet with jumping arenas)
        val grid = Array(h) { y ->
            Array(w) { x ->
                when {
                    x == 0 || y == 0 || x == w - 1 || y == h - 1 -> MapSector(wallType = 2, ceilingHeight = 2.2f)

                    // Checkerboard speed arenas
                    (x % 6 == 0) && (y % 6 == 0) -> MapSector(wallType = 3)
                    (x in 10..18 && y in 10..18) && (x == 10 || x == 18 || y == 10 || y == 18) && !(x in 13..15 || y in 13..15) -> MapSector(wallType = 1)

                    // Elevated jump towers
                    (x in 5..7 && y in 5..7) -> MapSector(wallType = 0, floorHeight = 0.8f, floorType = 1)
                    (x in 21..23 && y in 5..7) -> MapSector(wallType = 0, floorHeight = 0.8f, floorType = 1)
                    (x in 5..7 && y in 21..23) -> MapSector(wallType = 0, floorHeight = 0.8f, floorType = 1)
                    (x in 21..23 && y in 21..23) -> MapSector(wallType = 0, floorHeight = 0.8f, floorType = 1)

                    // Toxic energy chasms
                    (x in 11..17 && y in 11..17) -> MapSector(wallType = 0, floorHeight = -0.9f, floorType = 2, lightLevel = 0.8f)

                    else -> MapSector(wallType = 0, floorHeight = 0.0f, ceilingHeight = 2.0f)
                }
            }
        }

        val enemies = mutableListOf<Enemy>()
        var idCounter = 1
        for (i in 0 until 12) {
            val ex = (4 + (i * 5) % 20).toFloat()
            val ey = (4 + (i * 7) % 20).toFloat()
            val type = when (i % 4) {
                0 -> EnemyType.CYBER_DRONE
                1 -> EnemyType.NEON_NINJA
                2 -> EnemyType.SNIPER_TURRET
                else -> EnemyType.HEAVY_MECH
            }
            enemies.add(Enemy(idCounter++, type, ex, ey, if (type.isFlying) 0.5f else 0.0f, type.maxHealth))
        }

        val pickups = listOf(
            Pickup(1, PickupType.OVERDRIVE, 14.0f, 14.0f, 0.0f),
            Pickup(2, PickupType.HEALTH_PACK, 6.0f, 6.0f, 0.8f),
            Pickup(3, PickupType.SHIELD_CELL, 22.0f, 6.0f, 0.8f),
            Pickup(4, PickupType.AMMO_BOX, 6.0f, 22.0f, 0.8f),
            Pickup(5, PickupType.AMMO_BOX, 22.0f, 22.0f, 0.8f),
            Pickup(6, PickupType.CYBER_ORB, 14.0f, 6.0f, 0.0f),
            Pickup(7, PickupType.CYBER_ORB, 14.0f, 22.0f, 0.0f)
        )

        return GameMap(
            title = "District 03: Overdrive Gauntlet",
            width = w,
            height = h,
            sectors = grid,
            playerStartX = 2.5f,
            playerStartY = 2.5f,
            playerStartAngle = 0.0f,
            initialEnemies = enemies,
            initialPickups = pickups,
            exitX = 25.5f,
            exitY = 25.5f
        )
    }
}
