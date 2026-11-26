package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.SecureShredderEngine
import com.example.engine.StorageInspector
import com.example.engine.UsdtBlockchainVerifier
import com.example.engine.UsdtVerificationResult
import com.example.model.ErasureCatalog
import com.example.model.SelectiveCategory
import com.example.model.ShredTargetItem
import com.example.model.UsdtIapCatalog
import com.example.model.UsdtNetwork
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches iShredder Excellent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("iShredder Excellent", appName)
    }

    @Test
    fun `all 27 erasure methods have matching pass counts`() {
        assertEquals(27, ErasureCatalog.allMethods.size)
        ErasureCatalog.allMethods.forEach { method ->
            assertEquals(
                "Method ${method.name} cycles must match passes.size",
                method.cycles,
                method.passes.size
            )
        }
    }

    @Test
    fun `usdt catalog spans 1_49 to 39_99 with four supported usdt networks`() {
        val prices = UsdtIapCatalog.items.map { it.priceUsd }
        assertEquals(6, UsdtIapCatalog.items.size)
        assertEquals(1.49, prices.minOrNull() ?: 0.0, 0.001)
        assertEquals(39.99, prices.maxOrNull() ?: 0.0, 0.001)
        assertEquals(4, UsdtNetwork.entries.size)
    }

    @Test
    fun `usdt verifier validates tx hash and merchant address formats`() = runTest {
        val verifier = UsdtBlockchainVerifier()
        assertTrue(verifier.isValidTxHash("a".repeat(64), UsdtNetwork.TRC20))
        assertFalse(verifier.isValidTxHash("0x" + "a".repeat(64), UsdtNetwork.TRC20))
        assertTrue(verifier.isValidTxHash("0x" + "b".repeat(64), UsdtNetwork.ERC20))
        assertFalse(verifier.isValidTxHash("b".repeat(64), UsdtNetwork.ERC20))
        assertTrue(verifier.isValidMerchantAddress("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t", UsdtNetwork.TRC20))
        assertTrue(verifier.isValidMerchantAddress("0xdAC17F958D2ee523a2206206994597C13D831ec7", UsdtNetwork.ERC20))

        val invalidResult = verifier.verifyTransaction("invalid_hash", UsdtNetwork.ERC20)
        assertTrue(invalidResult is UsdtVerificationResult.Failed)
    }

    @Test
    fun `secure shredder overwrites and deletes local file`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inspector = StorageInspector(context)
        val engine = SecureShredderEngine(context, inspector)

        val tempFile = File(context.cacheDir, "secret_test_doc.txt")
        tempFile.writeText("TOP_SECRET_PAYLOAD_1234567890")
        assertTrue(tempFile.exists())

        val method = ErasureCatalog.findById("bsi_vsitr")
        val target = ShredTargetItem(
            id = "file:${tempFile.absolutePath}",
            title = tempFile.name,
            subtitle = "Test file",
            sizeBytes = tempFile.length(),
            category = SelectiveCategory.RESIDUAL_FILES,
            localFile = tempFile
        )

        val result = engine.executeShred(
            targets = listOf(target),
            method = method,
            freeSpaceOverwriteMb = 64L,
            onProgress = {}
        )

        assertFalse(result.wasCancelled)
        assertEquals(1, result.itemsShredded)
        assertEquals(0, result.itemsFailed)
        assertFalse(tempFile.exists())
        assertTrue(result.verificationSha256.length == 64)
    }

    @Test
    fun `all five theme ui styles resolve with distinct specs`() {
        val styles = com.example.ui.theme.UiThemeStyle.entries
        assertEquals(5, styles.size)
        val expectedIds = setOf("ios_27", "android_17", "hyperos_4", "coloros_17", "m3_expressive")
        assertEquals(expectedIds, styles.map { it.id }.toSet())

        styles.forEach { style ->
            assertEquals(style, com.example.ui.theme.UiThemeStyle.fromId(style.id))
            val spec = com.example.ui.theme.AppThemePresets.specFor(style)
            assertEquals(style, spec.style)
            assertTrue(spec.cardBorderWidth.value > 0f)
        }
        assertEquals(
            com.example.ui.theme.UiThemeStyle.M3_EXPRESSIVE,
            com.example.ui.theme.UiThemeStyle.fromId("unknown_fallback")
        )
    }
}
