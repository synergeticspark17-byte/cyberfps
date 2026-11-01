package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioEngine
import com.example.engine.GameEngine
import com.example.engine.MapCatalog
import com.example.model.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CyberDash", appName)
  }

  @Test
  fun `verify game levels and engine initialization`() {
    val level1 = MapCatalog.createLevel1()
    assertNotNull(level1)
    assertTrue(level1.initialEnemies.isNotEmpty())
    assertTrue(level1.initialPickups.isNotEmpty())
    assertNotNull(level1.bspRoot)

    val audioEngine = AudioEngine()
    audioEngine.soundEnabled = false
    audioEngine.musicEnabled = false
    val engine = GameEngine(audioEngine)

    assertEquals(WeaponType.BLASTER, engine.currentWeapon)
    assertTrue(engine.health > 0f)

    // Test parkour actions
    engine.onJumpPressed()
    assertTrue(engine.velZ > 0f)

    engine.onDashPressed()
    assertTrue(engine.isDashing)
  }
}
