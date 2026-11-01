package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.audio.AudioEngine
import com.example.model.*
import kotlin.math.*
import kotlin.random.Random

/**
 * 2.5D Raycasting and BSP Sector Engine with Sprite Billboarding,
 * Dynamic Lighting, Auto-Aiming, and Fluid Parkour Mechanics.
 */
class GameEngine(
    val audioEngine: AudioEngine
) {
    // Render buffer resolution
    val renderWidth = 360
    val renderHeight = 200

    private val pixels = IntArray(renderWidth * renderHeight)
    val renderBitmap: Bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
    private val zBuffer = FloatArray(renderWidth)

    // Current active map
    var map: GameMap = MapCatalog.createLevel1()

    // Player State
    var posX = 2.5f
    var posY = 2.5f
    var posZ = 0.0f // Elevation off sector floor
    var velX = 0.0f
    var velY = 0.0f
    var velZ = 0.0f

    // Camera: strictly horizontal (No Looking Up or Down as per engine constraint)
    var angle = 0.0f // Yaw in radians
    val fov = (PI * 0.42).toFloat() // ~75 degrees FOV

    // Parkour States
    var isGrounded = true
    var isWallRunning = false
    var wallSide = 0 // -1 = left wall, 1 = right wall
    var wallNormalX = 0f
    var wallNormalY = 0f
    var wallRunTimer = 0f
    var isSliding = false
    var slideTimer = 0f
    var isDashing = false
    var dashTimer = 0f
    var dashCooldown = 0f
    var canDoubleJump = true
    var bunnyHopTimer = 0f
    var momentumMultiplier = 1.0f
    var speedKmh = 0f

    // Combat States
    var health = 100f
    var maxHealth = 100f
    var shield = 100f
    var maxShield = 100f
    var currentWeapon = WeaponType.BLASTER
    val ammoMap = mutableMapOf(
        WeaponType.BLASTER to 999,
        WeaponType.SHOTGUN to 24,
        WeaponType.RAILGUN to 12,
        WeaponType.KATANA to 999
    )
    var fireCooldown = 0f
    var muzzleFlashTimer = 0f
    var weaponRecoil = 0f
    var weaponBobPhase = 0f
    var comboCounter = 0
    var comboTimer = 0f
    var score = 0
    var cyberOrbs = 0
    var hurtVignette = 0f

    // Auto-Aim Target (No looking up/down; automatic vertical pitch compensation)
    var autoAimTarget: AutoAimTarget? = null

    // World Entities
    val enemies = mutableListOf<Enemy>()
    val pickups = mutableListOf<Pickup>()
    val projectiles = mutableListOf<Projectile>()
    val particles = mutableListOf<Particle>()
    val damageTexts = mutableListOf<DamageText>()

    // Dynamic Point Lights (Muzzle flashes, plasma orbs, explosions)
    data class PointLight(val x: Float, val y: Float, val radius: Float, val intensity: Float, val color: Int)
    val dynamicLights = mutableListOf<PointLight>()

    // Level progression
    var currentLevelIndex = 1
    var isLevelComplete = false
    var isGameOver = false

    init {
        loadLevel(1)
    }

    fun loadLevel(levelNum: Int) {
        currentLevelIndex = levelNum
        map = when (levelNum) {
            1 -> MapCatalog.createLevel1()
            2 -> MapCatalog.createLevel2()
            else -> MapCatalog.createLevel3()
        }
        resetPlayer()
        enemies.clear()
        enemies.addAll(map.initialEnemies.map { it.copy() })
        pickups.clear()
        pickups.addAll(map.initialPickups.map { it.copy() })
        projectiles.clear()
        particles.clear()
        damageTexts.clear()
        isLevelComplete = false
        isGameOver = false
    }

    private fun resetPlayer() {
        posX = map.playerStartX
        posY = map.playerStartY
        posZ = 0.0f
        velX = 0f
        velY = 0f
        velZ = 0f
        angle = map.playerStartAngle
        health = maxHealth
        shield = maxShield
        momentumMultiplier = 1.0f
        isGrounded = true
        isWallRunning = false
        isSliding = false
        isDashing = false
        autoAimTarget = null
    }

    // ==========================================
    // UPDATE LOOP (60 FPS FIXED TICKS)
    // ==========================================

    fun update(
        dt: Float,
        moveForward: Float, // -1..1
        moveStrafe: Float,  // -1..1
        lookDeltaX: Float   // Touch delta yaw
    ) {
        if (isGameOver || isLevelComplete) return

        // 1. Look rotation (Strictly Horizontal Yaw - No Up/Down Pitch)
        angle += lookDeltaX
        if (angle < 0) angle += (2 * PI).toFloat()
        if (angle >= 2 * PI) angle -= (2 * PI).toFloat()

        // 2. Timers and cooldowns
        if (fireCooldown > 0f) fireCooldown -= dt
        if (muzzleFlashTimer > 0f) muzzleFlashTimer -= dt
        if (dashCooldown > 0f) dashCooldown -= dt
        if (weaponRecoil > 0f) weaponRecoil = max(0f, weaponRecoil - dt * 60f)
        if (hurtVignette > 0f) hurtVignette = max(0f, hurtVignette - dt * 2f)

        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) comboCounter = 0
        }

        if (bunnyHopTimer > 0f) bunnyHopTimer -= dt

        // 3. Movement & Parkour Mechanics
        updateParkourMovement(dt, moveForward, moveStrafe)

        // 4. Weapons & Projectiles
        updateProjectiles(dt)

        // 5. Enemies AI & Billboarding Logic
        updateEnemies(dt)

        // 6. Pickups
        updatePickups(dt)

        // 7. Particles & Visual FX
        updateParticles(dt)

        // 8. Auto-Aim Calculation (No Looking Up or Down!)
        calculateAutoAim()

        // 9. Exit / Level Completion check
        val distToExit = hypot(posX - map.exitX, posY - map.exitY)
        if (distToExit < 1.2f) {
            isLevelComplete = true
            audioEngine.playPickup()
        }

        // 10. Update Dynamic Lights list
        updateDynamicLights()
    }

    private fun updateParkourMovement(dt: Float, moveForward: Float, moveStrafe: Float) {
        val currentSector = map.getSector(posX.toInt(), posY.toInt())
        val groundHeight = currentSector.floorHeight
        val ceilingHeight = currentSector.ceilingHeight

        // Check if in hazard acid pit
        if (currentSector.floorType == 2 && posZ <= groundHeight + 0.1f) {
            applyDamage(dt * 30f)
            hurtVignette = 0.8f
        }

        // Handle Slide
        if (isSliding) {
            slideTimer -= dt
            if (slideTimer <= 0f) {
                // Check if ceiling is too low to stand up!
                if (ceilingHeight > 1.2f) {
                    isSliding = false
                } else {
                    slideTimer = 0.2f // Force keep sliding if under low vent!
                }
            }
        }

        // Handle Dash
        if (isDashing) {
            dashTimer -= dt
            if (dashTimer <= 0f) {
                isDashing = false
            }
        }

        // Calculate Target Acceleration Vector
        val cosA = cos(angle)
        val sinA = sin(angle)
        var targetSpeed = if (isSliding) 11.5f else 7.0f
        targetSpeed *= momentumMultiplier

        var accelX = (cosA * moveForward - sinA * moveStrafe) * targetSpeed
        var accelY = (sinA * moveForward + cosA * moveStrafe) * targetSpeed

        if (!isDashing) {
            val friction = if (isGrounded) (if (isSliding) 2.5f else 8.5f) else 1.8f
            velX += (accelX - velX) * min(1f, dt * friction)
            velY += (accelY - velY) * min(1f, dt * friction)
        }

        // Wall Running Check (airborne, moving forward, touching wall)
        checkWallRun(dt, moveForward)

        // Vertical Physics (Gravity & Jump/Fall)
        if (isWallRunning) {
            velZ = -0.8f // Very slow glide down the wall
            posZ += velZ * dt
            if (posZ < groundHeight) {
                posZ = groundHeight
                isWallRunning = false
                isGrounded = true
            }
        } else {
            // Normal gravity
            velZ -= 22f * dt
            posZ += velZ * dt

            // Platform / Floor Collision
            if (posZ <= groundHeight) {
                posZ = groundHeight
                velZ = 0f
                if (!isGrounded) {
                    // Just landed!
                    isGrounded = true
                    canDoubleJump = true
                    // Bunnyhop detection: landed while moving
                    if (bunnyHopTimer > 0f && (abs(velX) > 4f || abs(velY) > 4f)) {
                        momentumMultiplier = min(2.4f, momentumMultiplier * 1.12f)
                        audioEngine.playJump()
                    } else {
                        // Decay momentum smoothly
                        momentumMultiplier = max(1.0f, momentumMultiplier - dt * 0.8f)
                    }
                }
            } else {
                isGrounded = false
            }
        }

        // Ceiling collision
        val playerEyeHeight = if (isSliding) 0.35f else 0.75f
        if (posZ + playerEyeHeight > ceilingHeight) {
            posZ = ceilingHeight - playerEyeHeight
            if (velZ > 0f) velZ = 0f
        }

        // Horizontal Collision with Sector Walls (Sliding collision)
        val nextX = posX + velX * dt
        val nextY = posY + velY * dt
        val radius = 0.28f

        // X movement
        if (!isCollidingAt(nextX, posY, radius, groundHeight)) {
            posX = nextX
        } else {
            velX = 0f
            // If hitting wall while airborne, maybe can mantle onto it!
            tryMantle(nextX, posY)
        }

        // Y movement
        if (!isCollidingAt(posX, nextY, radius, groundHeight)) {
            posY = nextY
        } else {
            velY = 0f
            tryMantle(posX, nextY)
        }

        // Speed calculation
        val currentHorizSpeed = hypot(velX, velY)
        speedKmh = currentHorizSpeed * 3.6f

        // Weapon bobbing animation
        if (isGrounded && currentHorizSpeed > 0.5f) {
            weaponBobPhase += dt * currentHorizSpeed * 1.6f
        }
    }

    private fun isCollidingAt(x: Float, y: Float, radius: Float, curGround: Float): Boolean {
        val checkPoints = arrayOf(
            Pair(x - radius, y - radius),
            Pair(x + radius, y - radius),
            Pair(x - radius, y + radius),
            Pair(x + radius, y + radius)
        )
        for (pt in checkPoints) {
            val sec = map.getSector(pt.first.toInt(), pt.second.toInt())
            if (sec.wallType > 0) return true
            // Also treat platforms too high to step up as barriers
            if (sec.floorHeight > curGround + 0.45f && posZ < sec.floorHeight - 0.2f) {
                return true
            }
        }
        return false
    }

    private fun tryMantle(targetX: Float, targetY: Float) {
        val sec = map.getSector(targetX.toInt(), targetY.toInt())
        if (sec.wallType == 0 && sec.floorHeight > posZ && sec.floorHeight <= posZ + 0.8f) {
            // Mantle up!
            posZ = sec.floorHeight
            velZ = 4.0f
            audioEngine.playJump()
        }
    }

    private fun checkWallRun(dt: Float, moveForward: Float) {
        if (isGrounded || moveForward <= 0.2f) {
            isWallRunning = false
            return
        }

        val checkDist = 0.55f
        val cosA = cos(angle)
        val sinA = sin(angle)

        // Check left side (-sinA, cosA)
        val leftX = posX - sinA * checkDist
        val leftY = posY + cosA * checkDist
        val hasLeftWall = map.isWall(leftX.toInt(), leftY.toInt())

        // Check right side (sinA, -cosA)
        val rightX = posX + sinA * checkDist
        val rightY = posY - cosA * checkDist
        val hasRightWall = map.isWall(rightX.toInt(), rightY.toInt())

        if (hasLeftWall || hasRightWall) {
            if (!isWallRunning) {
                audioEngine.playWallRun()
                momentumMultiplier = min(2.2f, momentumMultiplier + 0.2f)
            }
            isWallRunning = true
            wallSide = if (hasLeftWall) -1 else 1
            wallNormalX = if (hasLeftWall) sinA else -sinA
            wallNormalY = if (hasLeftWall) -cosA else cosA
            wallRunTimer += dt

            // Spawn neon sparks along wall
            if (Random.nextFloat() < 0.4f) {
                particles.add(
                    Particle(
                        posX + (if (hasLeftWall) -sinA else sinA) * 0.4f,
                        posY + (if (hasLeftWall) cosA else -cosA) * 0.4f,
                        posZ + 0.4f,
                        Random.nextFloat() * 0.4f - 0.2f,
                        Random.nextFloat() * 0.4f - 0.2f,
                        Random.nextFloat() * 0.5f,
                        0xFF00F5FF.toInt(),
                        0.3f, 0.3f, 6f
                    )
                )
            }
        } else {
            isWallRunning = false
            wallRunTimer = 0f
        }
    }

    // ==========================================
    // PARKOUR INPUT ACTIONS
    // ==========================================

    fun onJumpPressed() {
        if (isWallRunning) {
            // Wall Jump! Push off wall with massive forward & outward burst
            isWallRunning = false
            velZ = 7.5f
            val pushX = wallNormalX * 6.5f + cos(angle) * 8.0f
            val pushY = wallNormalY * 6.5f + sin(angle) * 8.0f
            velX = pushX
            velY = pushY
            momentumMultiplier = min(2.5f, momentumMultiplier + 0.35f)
            audioEngine.playDoubleJump()
            bunnyHopTimer = 0.35f
        } else if (isGrounded) {
            // Normal Ground Jump
            velZ = 6.8f
            isGrounded = false
            audioEngine.playJump()
            bunnyHopTimer = 0.25f
        } else if (canDoubleJump) {
            // Air Double Jump
            canDoubleJump = false
            velZ = 7.2f
            momentumMultiplier = min(2.2f, momentumMultiplier + 0.2f)
            audioEngine.playDoubleJump()
            // Ring of cyan particles below
            for (i in 0 until 8) {
                val a = (i * PI * 2 / 8).toFloat()
                particles.add(
                    Particle(
                        posX, posY, posZ,
                        cos(a) * 2f, sin(a) * 2f, -0.5f,
                        0xFF00F5FF.toInt(), 0.35f, 0.35f, 7f
                    )
                )
            }
        }
    }

    fun onSlidePressed() {
        if (!isSliding && hypot(velX, velY) > 2.0f) {
            isSliding = true
            slideTimer = 0.75f
            // Kinetic slide speed burst
            velX *= 1.35f
            velY *= 1.35f
            momentumMultiplier = min(2.3f, momentumMultiplier + 0.15f)
            audioEngine.playSlide()
            // Slide sparks
            for (i in 0 until 5) {
                particles.add(
                    Particle(
                        posX, posY, posZ,
                        Random.nextFloat() * 2f - 1f, Random.nextFloat() * 2f - 1f, 0.5f,
                        0xFFFF007F.toInt(), 0.25f, 0.25f, 5f
                    )
                )
            }
        }
    }

    fun onDashPressed() {
        if (dashCooldown <= 0f && !isDashing) {
            isDashing = true
            dashTimer = 0.18f
            dashCooldown = 0.9f
            val cosA = cos(angle)
            val sinA = sin(angle)
            velX = cosA * 16.0f
            velY = sinA * 16.0f
            audioEngine.playDash()
            // Glitch dash particles
            for (i in 0 until 10) {
                particles.add(
                    Particle(
                        posX - cosA * (i * 0.2f),
                        posY - sinA * (i * 0.2f),
                        posZ + 0.4f,
                        Random.nextFloat() * 0.6f - 0.3f,
                        Random.nextFloat() * 0.6f - 0.3f,
                        Random.nextFloat() * 0.6f - 0.3f,
                        if (i % 2 == 0) 0xFF00F5FF.toInt() else 0xFFFF007F.toInt(),
                        0.4f, 0.4f, 8f
                    )
                )
            }
        }
    }

    fun switchWeapon(weapon: WeaponType) {
        currentWeapon = weapon
        weaponRecoil = 5f
    }

    // ==========================================
    // COMBAT & AUTO-AIM SHOOTING
    // ==========================================

    /**
     * Engine constraint: No Looking Up or Down!
     * The auto-aiming system scans for targets in front of the player
     * and calculates the necessary vertical trajectory to hit elevated enemies.
     */
    private fun calculateAutoAim() {
        var closestTarget: AutoAimTarget? = null
        var closestDist = 18.0f // Max targeting range

        val cosA = cos(angle)
        val sinA = sin(angle)

        for (enemy in enemies) {
            if (enemy.isDead) continue
            val dx = enemy.x - posX
            val dy = enemy.y - posY
            val dist = hypot(dx, dy)
            if (dist > closestDist) continue

            // Angle to enemy
            val enemyAngle = atan2(dy, dx)
            var angleDiff = enemyAngle - angle
            while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
            while (angleDiff < -PI) angleDiff += (2 * PI).toFloat()

            // Cone check (~26 degrees)
            if (abs(angleDiff) < 0.45f) {
                // Raycast check: is there a wall blocking vision to enemy?
                if (hasLineOfSight(posX, posY, enemy.x, enemy.y)) {
                    // Projected screen position
                    val screenX = (renderWidth * 0.5f) + (tan(angleDiff) / tan(fov * 0.5f)) * (renderWidth * 0.5f)
                    val screenY = renderHeight * 0.5f - (enemy.z - posZ) * (renderHeight / dist)
                    val pitchOffset = (enemy.z - (posZ + 0.4f)) / dist

                    closestTarget = AutoAimTarget(
                        enemy = enemy,
                        screenX = screenX,
                        screenY = screenY,
                        distance = dist,
                        verticalPitchOffset = pitchOffset
                    )
                    closestDist = dist
                }
            }
        }
        autoAimTarget = closestTarget
    }

    private fun hasLineOfSight(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        val dx = x1 - x0
        val dy = y1 - y0
        val dist = hypot(dx, dy)
        val steps = (dist * 4).toInt()
        val stepX = dx / steps
        val stepY = dy / steps
        var cx = x0
        var cy = y0
        for (i in 0 until steps) {
            cx += stepX
            cy += stepY
            if (map.isWall(cx.toInt(), cy.toInt())) return false
        }
        return true
    }

    fun onFirePressed(): Boolean {
        if (fireCooldown > 0f) return false
        val currentAmmo = ammoMap[currentWeapon] ?: 0
        if (currentAmmo <= 0 && currentWeapon != WeaponType.BLASTER && currentWeapon != WeaponType.KATANA) {
            return false // Out of ammo
        }

        fireCooldown = currentWeapon.fireRateSeconds
        muzzleFlashTimer = 0.08f
        weaponRecoil = currentWeapon.recoilAmount

        if (currentWeapon != WeaponType.BLASTER && currentWeapon != WeaponType.KATANA) {
            ammoMap[currentWeapon] = currentAmmo - 1
        }

        // Fire sound
        when (currentWeapon) {
            WeaponType.BLASTER -> audioEngine.playBlaster()
            WeaponType.SHOTGUN -> audioEngine.playShotgun()
            WeaponType.RAILGUN -> audioEngine.playRailgun()
            WeaponType.KATANA -> audioEngine.playKatana()
        }

        // Target aiming vector (Auto-Aim applies vertical pitch compensation!)
        val targetPitch = autoAimTarget?.verticalPitchOffset ?: 0.0f
        val cosA = cos(angle)
        val sinA = sin(angle)

        when (currentWeapon) {
            WeaponType.BLASTER -> {
                // Dual Pulse Blasters: 2 rapid plasma bolts
                val speed = 24.0f
                projectiles.add(
                    Projectile(
                        id = Random.nextInt(),
                        x = posX + cosA * 0.3f,
                        y = posY + sinA * 0.3f,
                        z = posZ + 0.4f,
                        vx = cosA * speed,
                        vy = sinA * speed,
                        vz = targetPitch * speed,
                        isPlayerOwned = true,
                        damage = currentWeapon.damage,
                        type = ProjectileType.PLASMA_BOLT
                    )
                )
            }

            WeaponType.SHOTGUN -> {
                // Scatter Flak: 8 spread pellets with heavy pushback
                velX -= cosA * 2.5f
                velY -= sinA * 2.5f
                val speed = 22.0f
                for (i in 0 until 8) {
                    val spreadAngle = angle + Random.nextFloat() * 0.18f - 0.09f
                    val spreadPitch = targetPitch + Random.nextFloat() * 0.12f - 0.06f
                    projectiles.add(
                        Projectile(
                            id = Random.nextInt(),
                            x = posX + cos(spreadAngle) * 0.3f,
                            y = posY + sin(spreadAngle) * 0.3f,
                            z = posZ + 0.4f,
                            vx = cos(spreadAngle) * speed,
                            vy = sin(spreadAngle) * speed,
                            vz = spreadPitch * speed,
                            isPlayerOwned = true,
                            damage = currentWeapon.damage / 6f,
                            type = ProjectileType.SHOTGUN_PELLET,
                            lifeTime = 0.6f
                        )
                    )
                }
            }

            WeaponType.RAILGUN -> {
                // Vapor Railgun: Pierces all enemies in beam
                val speed = 40.0f
                projectiles.add(
                    Projectile(
                        id = Random.nextInt(),
                        x = posX + cosA * 0.4f,
                        y = posY + sinA * 0.4f,
                        z = posZ + 0.4f,
                        vx = cosA * speed,
                        vy = sinA * speed,
                        vz = targetPitch * speed,
                        isPlayerOwned = true,
                        damage = currentWeapon.damage,
                        type = ProjectileType.RAILGUN_BEAM,
                        lifeTime = 1.0f
                    )
                )
            }

            WeaponType.KATANA -> {
                // Cyber Katana: Melee slash with dash impulse
                velX += cosA * 5.0f
                velY += sinA * 5.0f
                // Hit check in front of player
                for (enemy in enemies) {
                    if (enemy.isDead) continue
                    val dist = hypot(enemy.x - posX, enemy.y - posY)
                    if (dist < 1.8f) {
                        damageEnemy(enemy, currentWeapon.damage, isCrit = true)
                    }
                }
            }
        }
        return true
    }

    private fun updateProjectiles(dt: Float) {
        val iter = projectiles.iterator()
        while (iter.hasNext()) {
            val proj = iter.next()
            proj.lifeTime -= dt
            if (proj.lifeTime <= 0f) {
                iter.remove()
                continue
            }

            proj.x += proj.vx * dt
            proj.y += proj.vy * dt
            proj.z += proj.vz * dt

            // Wall collision
            if (map.isWall(proj.x.toInt(), proj.y.toInt())) {
                createImpactSparks(proj.x, proj.y, proj.z, 0xFF00F5FF.toInt())
                iter.remove()
                continue
            }

            // Entity collision
            if (proj.isPlayerOwned) {
                var hit = false
                for (enemy in enemies) {
                    if (enemy.isDead) continue
                    val dx = enemy.x - proj.x
                    val dy = enemy.y - proj.y
                    val dz = (enemy.z + 0.4f) - proj.z
                    val dist = sqrt(dx * dx + dy * dy + dz * dz)
                    if (dist < enemy.type.radius + proj.radius) {
                        damageEnemy(enemy, proj.damage, isCrit = (dz > 0.2f))
                        createImpactSparks(proj.x, proj.y, proj.z, 0xFFFF007F.toInt())
                        hit = true
                        if (proj.type != ProjectileType.RAILGUN_BEAM) break
                    }
                }
                if (hit && proj.type != ProjectileType.RAILGUN_BEAM) {
                    iter.remove()
                }
            } else {
                // Enemy projectile hitting player
                val dx = posX - proj.x
                val dy = posY - proj.y
                val dz = (posZ + 0.4f) - proj.z
                val dist = sqrt(dx * dx + dy * dy + dz * dz)
                if (dist < 0.4f + proj.radius) {
                    applyDamage(proj.damage)
                    createImpactSparks(proj.x, proj.y, proj.z, 0xFFFF2200.toInt())
                    iter.remove()
                }
            }
        }
    }

    private fun damageEnemy(enemy: Enemy, amount: Float, isCrit: Boolean) {
        enemy.health -= amount
        enemy.hitFlashTimer = 0.12f
        audioEngine.playHitMarker()

        // Floating damage text
        damageTexts.add(
            DamageText(
                x = enemy.x,
                y = enemy.y,
                z = enemy.z + 0.8f,
                text = "${amount.toInt()}${if (isCrit) "!" else ""}",
                color = if (isCrit) 0xFFFFE600.toInt() else 0xFFFFFFFF.toInt()
            )
        )

        if (enemy.health <= 0f && !enemy.isDead) {
            enemy.isDead = true
            enemy.state = EnemyState.DEAD
            audioEngine.playExplosion()
            score += enemy.type.scoreValue * (comboCounter + 1)
            comboCounter++
            comboTimer = 3.5f

            // Cyber orb drop
            cyberOrbs += 1
            pickups.add(Pickup(Random.nextInt(), PickupType.CYBER_ORB, enemy.x, enemy.y, enemy.z))

            // Explosion particles
            createExplosionFX(enemy.x, enemy.y, enemy.z + 0.4f)
        }
    }

    fun applyDamage(amount: Float) {
        if (isDashing) return // Invulnerable during air dash!

        audioEngine.playPlayerHurt()
        hurtVignette = 0.9f

        var remaining = amount
        if (shield > 0f) {
            val absorbed = min(shield, remaining)
            shield -= absorbed
            remaining -= absorbed
        }
        if (remaining > 0f) {
            health -= remaining
            if (health <= 0f) {
                health = 0f
                isGameOver = true
            }
        }
    }

    private fun updateEnemies(dt: Float) {
        for (enemy in enemies) {
            if (enemy.isDead) {
                enemy.deathTimer += dt
                continue
            }

            enemy.animTimer += dt
            if (enemy.animTimer > 0.25f) {
                enemy.animTimer = 0f
                enemy.animFrame = (enemy.animFrame + 1) % 2
            }

            if (enemy.hitFlashTimer > 0f) enemy.hitFlashTimer -= dt
            if (enemy.attackCooldown > 0f) enemy.attackCooldown -= dt

            val dx = posX - enemy.x
            val dy = posY - enemy.y
            val dist = hypot(dx, dy)

            // AI State Machine
            if (dist < 14f && hasLineOfSight(enemy.x, enemy.y, posX, posY)) {
                enemy.state = EnemyState.CHASE

                // Move toward player if not turret
                if (enemy.type != EnemyType.SNIPER_TURRET && dist > 2.0f) {
                    val moveSpeed = enemy.type.speed * dt
                    val dirX = (dx / dist) * moveSpeed
                    val dirY = (dy / dist) * moveSpeed

                    if (!map.isWall((enemy.x + dirX).toInt(), (enemy.y + dirY).toInt())) {
                        enemy.x += dirX
                        enemy.y += dirY
                    }
                }

                // Attack logic
                if (enemy.attackCooldown <= 0f && dist < 12f) {
                    enemy.attackCooldown = when (enemy.type) {
                        EnemyType.CYBER_DRONE -> 1.8f
                        EnemyType.HEAVY_MECH -> 2.4f
                        EnemyType.NEON_NINJA -> 1.0f
                        EnemyType.SNIPER_TURRET -> 2.2f
                    }

                    // Fire enemy projectiles
                    if (enemy.type == EnemyType.CYBER_DRONE || enemy.type == EnemyType.SNIPER_TURRET) {
                        val projSpeed = 12f
                        projectiles.add(
                            Projectile(
                                id = Random.nextInt(),
                                x = enemy.x,
                                y = enemy.y,
                                z = enemy.z + 0.4f,
                                vx = (dx / dist) * projSpeed,
                                vy = (dy / dist) * projSpeed,
                                vz = ((posZ + 0.4f) - (enemy.z + 0.4f)) / dist * projSpeed,
                                isPlayerOwned = false,
                                damage = 14f,
                                type = ProjectileType.ENEMY_LASER
                            )
                        )
                    } else if (enemy.type == EnemyType.HEAVY_MECH) {
                        val projSpeed = 10f
                        projectiles.add(
                            Projectile(
                                id = Random.nextInt(),
                                x = enemy.x,
                                y = enemy.y,
                                z = enemy.z + 0.8f,
                                vx = (dx / dist) * projSpeed,
                                vy = (dy / dist) * projSpeed,
                                vz = 0f,
                                isPlayerOwned = false,
                                damage = 25f,
                                type = ProjectileType.ENEMY_MISSILE
                            )
                        )
                    } else if (enemy.type == EnemyType.NEON_NINJA && dist < 2.2f) {
                        applyDamage(20f)
                    }
                }
            }
        }
    }

    private fun updatePickups(dt: Float) {
        for (pickup in pickups) {
            if (pickup.isCollected) continue
            pickup.bobOffset = (sin(System.currentTimeMillis() * 0.005) * 0.1).toFloat()

            val dist = hypot(posX - pickup.x, posY - pickup.y)
            if (dist < 0.8f && abs(posZ - pickup.z) < 1.0f) {
                pickup.isCollected = true
                audioEngine.playPickup()
                when (pickup.type) {
                    PickupType.HEALTH_PACK -> health = min(maxHealth, health + 35f)
                    PickupType.SHIELD_CELL -> shield = min(maxShield, shield + 50f)
                    PickupType.AMMO_BOX -> {
                        ammoMap[WeaponType.SHOTGUN] = (ammoMap[WeaponType.SHOTGUN] ?: 0) + 12
                        ammoMap[WeaponType.RAILGUN] = (ammoMap[WeaponType.RAILGUN] ?: 0) + 6
                    }
                    PickupType.OVERDRIVE -> {
                        momentumMultiplier = 2.4f
                        comboCounter += 2
                        comboTimer = 6.0f
                    }
                    PickupType.CYBER_ORB -> cyberOrbs += 1
                }
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.life -= dt
            if (p.life <= 0f) {
                pIter.remove()
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
        }

        val dIter = damageTexts.iterator()
        while (dIter.hasNext()) {
            val d = dIter.next()
            d.life -= dt
            if (d.life <= 0f) {
                dIter.remove()
                continue
            }
            d.z += d.vy * dt
        }
    }

    private fun updateDynamicLights() {
        dynamicLights.clear()
        // Player muzzle flash light
        if (muzzleFlashTimer > 0f) {
            dynamicLights.add(PointLight(posX, posY, 4.5f, 1.0f, 0xFF00F5FF.toInt()))
        }
        // Projectile point lights
        for (proj in projectiles) {
            val color = if (proj.isPlayerOwned) 0xFF00F5FF.toInt() else 0xFFFF2200.toInt()
            dynamicLights.add(PointLight(proj.x, proj.y, 2.5f, 0.7f, color))
        }
    }

    private fun createImpactSparks(x: Float, y: Float, z: Float, color: Int) {
        for (i in 0 until 6) {
            particles.add(
                Particle(
                    x, y, z,
                    Random.nextFloat() * 2f - 1f,
                    Random.nextFloat() * 2f - 1f,
                    Random.nextFloat() * 2f - 0.5f,
                    color, 0.25f, 0.25f, 4f
                )
            )
        }
    }

    private fun createExplosionFX(x: Float, y: Float, z: Float) {
        for (i in 0 until 18) {
            val a = Random.nextFloat() * 2 * PI
            val speed = Random.nextFloat() * 4f + 1f
            particles.add(
                Particle(
                    x, y, z,
                    cos(a).toFloat() * speed,
                    sin(a).toFloat() * speed,
                    Random.nextFloat() * 3f - 1f,
                    if (i % 2 == 0) 0xFFFF9900.toInt() else 0xFFFF0055.toInt(),
                    0.5f, 0.5f, 8f
                )
            )
        }
    }

    // ==========================================
    // 2.5D RAYCASTING & SPRITE BILLBOARD RENDERER
    // ==========================================

    fun renderFrame() {
        val playerEyeZ = posZ + (if (isSliding) 0.35f else 0.75f)
        val cosYaw = cos(angle)
        val sinYaw = sin(angle)

        // 1. Clear background (Atmospheric gradient: cyber zenith to horizon)
        for (y in 0 until renderHeight) {
            val skyColor = if (y < renderHeight / 2) {
                0xFF080614.toInt() // Deep cyber indigo
            } else {
                0xFF0C0F1E.toInt() // Dark floor horizon
            }
            val rowOffset = y * renderWidth
            for (x in 0 until renderWidth) {
                pixels[rowOffset + x] = skyColor
            }
        }

        // 2. Raycast Walls & Floors (with Sector Heights)
        val halfFov = fov * 0.5f

        for (x in 0 until renderWidth) {
            // Camera ray angle
            val rayAngle = angle - halfFov + (x.toFloat() / renderWidth) * fov
            val rayDirX = cos(rayAngle)
            val rayDirY = sin(rayAngle)

            // DDA Raycasting algorithm
            var mapX = posX.toInt()
            var mapY = posY.toInt()

            val deltaDistX = if (rayDirX != 0f) abs(1f / rayDirX) else 1e30f
            val deltaDistY = if (rayDirY != 0f) abs(1f / rayDirY) else 1e30f

            var stepX: Int
            var sideDistX: Float
            if (rayDirX < 0) {
                stepX = -1
                sideDistX = (posX - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1.0f - posX) * deltaDistX
            }

            var stepY: Int
            var sideDistY: Float
            if (rayDirY < 0) {
                stepY = -1
                sideDistY = (posY - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1.0f - posY) * deltaDistY
            }

            var hit = false
            var side = 0 // 0 for X, 1 for Y
            var hitSector = map.getSector(mapX, mapY)

            while (!hit) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    side = 0
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    side = 1
                }

                if (mapX !in 0 until map.width || mapY !in 0 until map.height) {
                    hit = true
                    hitSector = MapSector(1)
                } else {
                    hitSector = map.getSector(mapX, mapY)
                    if (hitSector.wallType > 0) {
                        hit = true
                    }
                }
            }

            // Perpendicular wall distance (correct fish-eye distortion)
            val perpWallDist = if (side == 0) {
                (mapX - posX + (1 - stepX) / 2) / rayDirX
            } else {
                (mapY - posY + (1 - stepY) / 2) / rayDirY
            }
            val correctedDist = max(0.1f, perpWallDist * cos(rayAngle - angle))
            zBuffer[x] = correctedDist

            // Wall column height and sector offsets
            val lineHeight = (renderHeight / correctedDist).toInt()
            val wallFloorZ = hitSector.floorHeight
            val wallCeilingZ = hitSector.ceilingHeight

            // Projected top and bottom screen coordinates
            val verticalCenter = renderHeight / 2
            val drawStart = verticalCenter - (lineHeight * (wallCeilingZ - playerEyeZ)).toInt()
            val drawEnd = verticalCenter + (lineHeight * (playerEyeZ - wallFloorZ)).toInt()

            val clampedStart = drawStart.coerceIn(0, renderHeight - 1)
            val clampedEnd = drawEnd.coerceIn(0, renderHeight - 1)

            // Texture coordinate X
            var wallX = if (side == 0) posY + perpWallDist * rayDirY else posX + perpWallDist * rayDirX
            wallX -= floor(wallX)
            var texX = (wallX * TextureStore.TEX_SIZE).toInt()
            if (side == 0 && rayDirX > 0) texX = TextureStore.TEX_SIZE - texX - 1
            if (side == 1 && rayDirY < 0) texX = TextureStore.TEX_SIZE - texX - 1

            val wallTex = TextureStore.wallTextures[hitSector.wallType] ?: TextureStore.wallTextures[1]!!

            // Dynamic lighting calculation for this column
            var lightFactor = 1.0f / (1.0f + correctedDist * 0.18f)
            // Side shading for pseudo-3D depth
            if (side == 1) lightFactor *= 0.78f
            // Dynamic point lights contribution
            for (light in dynamicLights) {
                val hitX = posX + rayDirX * perpWallDist
                val hitY = posY + rayDirY * perpWallDist
                val lDist = hypot(hitX - light.x, hitY - light.y)
                if (lDist < light.radius) {
                    lightFactor += (1f - lDist / light.radius) * light.intensity * 0.6f
                }
            }
            lightFactor = lightFactor.coerceIn(0.15f, 1.25f)

            // Draw wall column
            for (y in clampedStart..clampedEnd) {
                val texY = (((y - drawStart) * TextureStore.TEX_SIZE) / max(1, drawEnd - drawStart)).coerceIn(0, TextureStore.TEX_SIZE - 1)
                val rawColor = wallTex[texY * TextureStore.TEX_SIZE + texX]
                pixels[y * renderWidth + x] = applyLighting(rawColor, lightFactor)
            }
        }

        // 3. Render 2D Sprites in 3D Space (Billboarding with Depth Sorting)
        renderBillboardSprites(playerEyeZ, cosYaw, sinYaw)

        // 4. Update the Android Bitmap with newly rendered pixels
        renderBitmap.setPixels(pixels, 0, renderWidth, 0, 0, renderWidth, renderHeight)
    }

    private data class DrawableSprite(
        val type: Int, // 0 = enemy, 1 = pickup, 2 = projectile, 3 = particle
        val x: Float,
        val y: Float,
        val z: Float,
        val distanceSq: Float,
        val entityRef: Any
    )

    private fun renderBillboardSprites(playerEyeZ: Float, cosYaw: Float, sinYaw: Float) {
        val spriteList = mutableListOf<DrawableSprite>()

        // Collect Enemies (2D Sprites in 3D Space)
        for (enemy in enemies) {
            val distSq = (posX - enemy.x) * (posX - enemy.x) + (posY - enemy.y) * (posY - enemy.y)
            spriteList.add(DrawableSprite(0, enemy.x, enemy.y, enemy.z, distSq, enemy))
        }

        // Collect Pickups
        for (pickup in pickups) {
            if (pickup.isCollected) continue
            val distSq = (posX - pickup.x) * (posX - pickup.x) + (posY - pickup.y) * (posY - pickup.y)
            spriteList.add(DrawableSprite(1, pickup.x, pickup.y, pickup.z + pickup.bobOffset, distSq, pickup))
        }

        // Sort sprites strictly back-to-front (Painters algorithm for transparent sprites)
        spriteList.sortByDescending { it.distanceSq }

        // Render each billboarding sprite
        for (sprite in spriteList) {
            // Translate sprite position to camera space
            val spriteX = sprite.x - posX
            val spriteY = sprite.y - posY

            // Transform with camera rotation matrix
            // Inverse camera transform (Billboarding automatically faces player's camera)
            val invDet = 1.0f / (cosYaw * cosYaw + sinYaw * sinYaw) // = 1.0f
            val transformX = invDet * (sinYaw * spriteX - cosYaw * spriteY)
            val transformY = invDet * (cosYaw * spriteX + sinYaw * spriteY) // Depth distance

            if (transformY <= 0.2f) continue // Behind or right on camera

            val spriteScreenX = ((renderWidth / 2) * (1 + transformX / (transformY * tan(fov * 0.5f)))).toInt()

            // Height and width on screen based on distance
            val spriteHeight = abs((renderHeight / transformY) * 1.0f).toInt()
            val spriteWidth = abs((renderHeight / transformY) * 1.0f).toInt()

            val verticalCenter = renderHeight / 2
            val spriteScreenY = verticalCenter + ((playerEyeZ - sprite.z) * (renderHeight / transformY)).toInt()

            val drawStartY = (spriteScreenY - spriteHeight / 2).coerceIn(0, renderHeight - 1)
            val drawEndY = (spriteScreenY + spriteHeight / 2).coerceIn(0, renderHeight - 1)

            val drawStartX = (spriteScreenX - spriteWidth / 2).coerceIn(0, renderWidth - 1)
            val drawEndX = (spriteScreenX + spriteWidth / 2).coerceIn(0, renderWidth - 1)

            // Dynamic distance lighting
            val lightFactor = (1.0f / (1.0f + transformY * 0.15f)).coerceIn(0.2f, 1.2f)

            // Draw Billboard Bitmap
            when (sprite.type) {
                0 -> {
                    // Enemy 2D Billboard
                    val enemy = sprite.entityRef as Enemy
                    val frames = TextureStore.enemySprites[enemy.type]
                    val texture = if (frames != null && frames.isNotEmpty()) {
                        frames[enemy.animFrame % frames.size]
                    } else null

                    if (texture != null) {
                        for (stripe in drawStartX..drawEndX) {
                            val texX = (((stripe - (spriteScreenX - spriteWidth / 2)) * TextureStore.TEX_SIZE) / spriteWidth).coerceIn(0, TextureStore.TEX_SIZE - 1)
                            // Depth test against Z-Buffer!
                            if (transformY < zBuffer[stripe]) {
                                for (y in drawStartY..drawEndY) {
                                    val texY = (((y - (spriteScreenY - spriteHeight / 2)) * TextureStore.TEX_SIZE) / spriteHeight).coerceIn(0, TextureStore.TEX_SIZE - 1)
                                    val color = texture[texY * TextureStore.TEX_SIZE + texX]
                                    if ((color and -0x1000000) != 0) { // Alpha > 0
                                        val finalColor = if (enemy.hitFlashTimer > 0f) 0xFFFFFFFF.toInt() else applyLighting(color, lightFactor)
                                        pixels[y * renderWidth + stripe] = finalColor
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Pickup 2D Billboard
                    val pickup = sprite.entityRef as Pickup
                    val texture = TextureStore.pickupSprites[pickup.type]
                    if (texture != null) {
                        for (stripe in drawStartX..drawEndX) {
                            val texX = (((stripe - (spriteScreenX - spriteWidth / 2)) * TextureStore.TEX_SIZE) / spriteWidth).coerceIn(0, TextureStore.TEX_SIZE - 1)
                            if (transformY < zBuffer[stripe]) {
                                for (y in drawStartY..drawEndY) {
                                    val texY = (((y - (spriteScreenY - spriteHeight / 2)) * TextureStore.TEX_SIZE) / spriteHeight).coerceIn(0, TextureStore.TEX_SIZE - 1)
                                    val color = texture[texY * TextureStore.TEX_SIZE + texX]
                                    if ((color and -0x1000000) != 0) {
                                        pixels[y * renderWidth + stripe] = applyLighting(color, lightFactor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyLighting(argb: Int, factor: Float): Int {
        val a = (argb shr 24) and 0xFF
        val r = (((argb shr 16) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = (((argb shr 8) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((argb and 0xFF) * factor).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
