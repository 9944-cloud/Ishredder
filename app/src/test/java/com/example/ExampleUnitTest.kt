package com.example

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun all_six_theme_ui_styles_and_oled_mode_resolve_correctly() {
    val styles = com.example.ui.theme.UiThemeStyle.entries
    assertEquals(6, styles.size)
    assertEquals(
      setOf("ios_27", "android_17", "hyperos_4", "coloros_17", "m3_expressive", "oled"),
      styles.map { it.id }.toSet()
    )
    styles.forEach { style ->
      assertEquals(style, com.example.ui.theme.UiThemeStyle.fromId(style.id))
      val spec = com.example.ui.theme.AppThemePresets.specFor(style)
      assertEquals(style, spec.style)
      assertTrue(spec.cardBorderWidth.value > 0f)

      val oledSpec = com.example.ui.theme.AppThemePresets.resolveSpec(
        style = style,
        oledEnabled = true,
        accentId = "cyan"
      )
      assertTrue(oledSpec.isOledBlack)
      assertEquals(Color.Black, oledSpec.bgBase)
      assertEquals(Color.Black, oledSpec.bgGradientTop)
      assertEquals(Color.Black, oledSpec.bgGradientBottom)
      assertEquals(spec.cardShape, oledSpec.cardShape)
      assertEquals(
        com.example.ui.theme.ThemeAccentPalette.CYBER_CYAN.primary,
        oledSpec.accentPrimary
      )
    }
  }

  @Test
  fun factory_reset_and_2026_catalog_defaults_are_valid() {
    val defaults = com.example.data.AppSettings()
    assertFalse(defaults.passcodeEnabled)
    assertEquals("", defaults.passcodeHash)
    assertEquals("en", defaults.languageCode)
    assertEquals("bsi_vsitr", defaults.defaultMethodId)
    assertEquals(128L, defaults.freeSpaceOverwriteMb)
    assertEquals("m3_expressive", defaults.uiThemeStyleId)
    assertFalse(defaults.dynamicColorEnabled)
    assertFalse(defaults.oledModeEnabled)
    assertEquals("os_default", defaults.uiAccentId)
    assertFalse(defaults.autoShredEnabled)
    assertEquals("q_shred_2026", defaults.autoShredMethodId)

    assertEquals("bsi_vsitr", com.example.model.ErasureCatalog.findById(defaults.defaultMethodId).id)
    assertEquals(30, com.example.model.ErasureCatalog.allMethods.size)
    assertEquals("nist_800_88_r2_2026", com.example.model.ErasureCatalog.findById("nist_800_88_r2_2026").id)
    assertEquals("eu_cra_2026", com.example.model.ErasureCatalog.findById("eu_cra_2026").id)
    assertEquals("q_shred_2026", com.example.model.ErasureCatalog.findById("q_shred_2026").id)
  }
}
