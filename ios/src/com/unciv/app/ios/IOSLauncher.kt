package com.unciv.app.ios

import com.badlogic.gdx.backends.iosrobovm.IOSApplication
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration
import com.badlogic.gdx.backends.iosrobovm.IOSGraphics
import com.badlogic.gdx.backends.iosrobovm.IOSScreenBounds
import com.badlogic.gdx.backends.iosrobovm.IOSUIViewController
import com.unciv.logic.files.UncivFiles
import com.unciv.ui.components.fonts.Fonts
import com.unciv.utils.Display
import com.unciv.utils.Log
import org.robovm.apple.foundation.NSAutoreleasePool
import org.robovm.apple.foundation.NSFileManager
import org.robovm.apple.foundation.NSSearchPathDirectory
import org.robovm.apple.foundation.NSSearchPathDomainMask
import org.robovm.apple.uikit.UIApplication
import org.robovm.apple.uikit.UIApplicationDelegateAdapter
import org.robovm.apple.uikit.UIApplicationLaunchOptions
import org.robovm.apple.uikit.UIScreen
import kotlin.math.roundToInt

/**
 * iOS launcher for Unciv.
 *
 * Main entry point that initializes the LibGDX iOS application
 * and sets up all platform-specific implementations.
 */
class IOSLauncher : IOSApplication.Delegate() {

    override fun createApplication(): IOSApplication {
        // Setup iOS logging
        Log.backend = IOSLogBackend()

        // Setup iOS display
        val displayImpl = IOSDisplay()
        Display.platform = displayImpl

        // Setup iOS fonts
        Fonts.fontImplementation = IOSFont()

        // Setup iOS file saver/loader
        UncivFiles.saverLoader = IOSSaverLoader()

        // Get documents directory path - use simplified approach
        val documentsPath = System.getProperty("user.home") + "/Documents"
        val documentsDir = java.io.File(documentsPath).absolutePath

        // Load settings
        val settings = UncivFiles.getSettingsForPlatformLaunchers(documentsDir)
        displayImpl.setCutout(settings.androidCutout)

        // DEBUG: Test UniqueType initialization
        println("HELLO!!!")
        try {
            println("Testing UniqueType initialization: ${com.unciv.models.ruleset.unique.UniqueType.entries.size} types")
            println("uniqueTypeMap size: ${com.unciv.models.ruleset.unique.UniqueType.uniqueTypeMap.size}")

            // Test creating a simple Unique object
            val testUnique = com.unciv.models.ruleset.unique.Unique("[+1 Gold]", null, "Test")
            println("Test Unique created successfully, params: ${testUnique.params}")
            println("Test Unique params is null: ${testUnique.params == null}")
            println("Test Unique params size: ${testUnique.params.size}")
        } catch (e: Exception) {
            println("ERROR during UniqueType initialization: ${e.message}")
            e.printStackTrace()
        }

        val testRuleset = com.unciv.models.ruleset.RulesetCache["Civ V - Vanilla"]
        if (testRuleset != null && testRuleset.buildings.isNotEmpty()) {
            val firstBuilding = testRuleset.buildings.values.first()
            println("Test building: ${firstBuilding.name}")
            println("  uniques strings: ${firstBuilding.uniques.size}")
            println("  uniqueObjects: ${firstBuilding.uniqueObjects.size}")
            if (firstBuilding.uniqueObjects.isNotEmpty()) {
                val firstUnique = firstBuilding.uniqueObjects.first()
                println("  first unique text: ${firstUnique.text}")
                println("  first unique params: ${firstUnique.params}")
            }
        } else {
            println("Ruleset not loaded!")
        }

        // Create iOS application configuration
        val config = IOSApplicationConfiguration()
        config.preferredFramesPerSecond = 60

        // Create and initialize game
        val game = IOSGame()
        return SafeAreaIOSApplication(game, config, displayImpl)
    }

    override fun didBecomeActive(application: UIApplication) {
        super.didBecomeActive(application)
        // Clear any pending notifications when app becomes active
        // This will be used for multiplayer turn notifications
    }

    override fun willResignActive(application: UIApplication) {
        super.willResignActive(application)
        // Schedule background turn checking when app goes to background
        // This will be implemented in Phase 4
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val pool = NSAutoreleasePool()
            UIApplication.main<UIApplication, IOSLauncher>(args, null, IOSLauncher::class.java)
            pool.close()
        }
    }
}

private class SafeAreaIOSApplication(
    listener: IOSGame,
    config: IOSApplicationConfiguration,
    private val display: IOSDisplay
) : IOSApplication(listener, config) {
    override fun computeBounds(): IOSScreenBounds {
        val window = getUIWindow() ?: return super.computeBounds()
        val contentFrame = display.getContentFrame(window)
        val width = contentFrame.width.roundToInt()
        val height = contentFrame.height.roundToInt()
        if (width <= 0 || height <= 0) return super.computeBounds()

        val scale = UIScreen.getMainScreen().nativeScale
        return IOSScreenBounds(
            0,
            0,
            width,
            height,
            (width * scale).roundToInt(),
            (height * scale).roundToInt()
        )
    }

    override fun createUIViewController(graphics: IOSGraphics): IOSUIViewController {
        return SafeAreaIOSUIViewController(this, graphics, display)
    }
}

private class SafeAreaIOSUIViewController(
    application: IOSApplication,
    graphics: IOSGraphics,
    private val display: IOSDisplay
) : IOSUIViewController(application, graphics) {
    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        display.applySafeAreaFrame()
    }

    override fun viewDidLayoutSubviews() {
        display.applySafeAreaFrame()
        super.viewDidLayoutSubviews()
        display.applySafeAreaFrame()
    }

    override fun viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        display.applySafeAreaFrame()
    }
}
