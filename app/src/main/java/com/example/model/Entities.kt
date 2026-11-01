package com.example.model

enum class WeaponType(
    val title: String,
    val fireRateSeconds: Float,
    val damage: Float,
    val maxAmmo: Int,
    val isMelee: Boolean = false,
    val recoilAmount: Float = 14f
) {
    BLASTER("Dual Pulse", 0.16f, 22f, 999, false, 8f),
    SHOTGUN("Scatter Flak", 0.65f, 75f, 32, false, 24f),
    RAILGUN("Vapor Rail", 0.90f, 150f, 16, false, 28f),
    KATANA("Cyber Katana", 0.35f, 110f, 999, true, 4f)
}

enum class EnemyType(
    val title: String,
    val maxHealth: Float,
    val speed: Float,
    val scoreValue: Int,
    val radius: Float = 0.45f,
    val height: Float = 0.9f,
    val isFlying: Boolean = false
) {
    CYBER_DRONE("Hunter Drone", 50f, 3.2f, 150, 0.4f, 0.6f, true),
    HEAVY_MECH("Apex Enforcer", 220f, 2.0f, 500, 0.65f, 1.2f, false),
    NEON_NINJA("Vapor Stalker", 75f, 5.0f, 250, 0.4f, 0.9f, false),
    SNIPER_TURRET("Pulse Sentry", 90f, 0.0f, 200, 0.5f, 0.8f, false)
}

data class Enemy(
    val id: Int,
    val type: EnemyType,
    var x: Float,
    var y: Float,
    var z: Float, // Elevation in 3D space
    var health: Float,
    var maxHealth: Float = type.maxHealth,
    var state: EnemyState = EnemyState.CHASE,
    var attackCooldown: Float = 1.0f,
    var hitFlashTimer: Float = 0f,
    var animTimer: Float = 0f,
    var animFrame: Int = 0,
    var isDead: Boolean = false,
    var deathTimer: Float = 0f
)

enum class EnemyState {
    IDLE, CHASE, ATTACK, HURT, DEAD
}

enum class ProjectileType {
    PLASMA_BOLT,
    SHOTGUN_PELLET,
    RAILGUN_BEAM,
    ENEMY_LASER,
    ENEMY_MISSILE
}

data class Projectile(
    val id: Int,
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    val isPlayerOwned: Boolean,
    val damage: Float,
    val type: ProjectileType,
    var lifeTime: Float = 2.5f,
    val radius: Float = 0.15f
)

enum class PickupType(val title: String, val colorHex: Int) {
    HEALTH_PACK("Nanite Medkit", 0xFF00FF66.toInt()),
    SHIELD_CELL("Cyber Shield", 0xFF00F5FF.toInt()),
    AMMO_BOX("Power Cell", 0xFFFFE600.toInt()),
    OVERDRIVE("Overdrive Core", 0xFFFF007F.toInt()),
    CYBER_ORB("Data Shard", 0xFFD600FF.toInt())
}

data class Pickup(
    val id: Int,
    val type: PickupType,
    var x: Float,
    var y: Float,
    var z: Float,
    var isCollected: Boolean = false,
    var bobOffset: Float = 0f
)

data class Particle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    val color: Int,
    var life: Float,
    val maxLife: Float,
    val size: Float
)

data class DamageText(
    var x: Float,
    var y: Float,
    var z: Float,
    val text: String,
    val color: Int,
    var life: Float = 0.8f,
    val maxLife: Float = 0.8f,
    var vy: Float = 0.8f
)

data class AutoAimTarget(
    val enemy: Enemy,
    val screenX: Float,
    val screenY: Float,
    val distance: Float,
    val verticalPitchOffset: Float // Pitch compensation for auto-aim
)
