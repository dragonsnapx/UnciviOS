package com.unciv.app.ios

import com.unciv.models.metadata.GameSettings
import com.unciv.utils.PlatformDisplay
import com.unciv.utils.ScreenMode
import com.unciv.utils.ScreenOrientation
import org.robovm.apple.uikit.*
import org.robovm.apple.foundation.NSSet
import org.robovm.apple.uikit.UIWindowScene
import org.robovm.apple.uikit.UIWindow

/**
 * iOS implementation of PlatformDisplay.
 *
 * Notes:
 * - For reliable notch / safe-area detection call `hasCutout()` from the main thread
 *   after layout (e.g. in your UIViewController's viewDidLayoutSubviews or viewDidAppear).
 * - This implementation prefers a scene's key window and falls back to keyWindow / windows list.
 */
class IOSDisplay : PlatformDisplay {

    private var requestedOrientation = ScreenOrientation.Auto

    override fun hasOrientation(): Boolean = true

    override fun setOrientation(orientation: ScreenOrientation) {
        requestedOrientation = orientation
        // Actual orientation chosen by the ViewController (preferredInterfaceOrientationForPresentation)
    }

    fun getPreferredOrientation(): ScreenOrientation = requestedOrientation

    override fun hasSystemUiVisibility(): Boolean = true

    override fun setSystemUiVisibility(hide: Boolean) {
        val app = UIApplication.getSharedApplication()
        val window = findKeyWindow(app)
        val vc = window?.rootViewController
        // Ask the VC to refresh status bar / home indicator appearance
        vc?.setNeedsStatusBarAppearanceUpdate()
        try {
            vc?.setNeedsUpdateOfHomeIndicatorAutoHidden()
        } catch (e: Exception) {
            // iOS < 11 or RoboVM binding not available — ignore
        }
    }

    override fun hasCutout(): Boolean {
        val app = UIApplication.getSharedApplication()
        val window = findKeyWindow(app) ?: return false

        // Prefer the rootViewController's view safe area insets (most reliable)
        val rootVCViewInsets = window.rootViewController?.view?.safeAreaInsets
        if (rootVCViewInsets != null) {
            if (rootVCViewInsets.top > 0.0) return true
        }

        // Fallback to window safe area insets
        val insets = window.safeAreaInsets
        return (insets?.top ?: 0.0) > 0.0
    }

    override fun setCutout(enabled: Boolean) {
        // iOS manages cutouts via safe area insets — nothing to do here
    }

    override fun getScreenModes(): Map<Int, ScreenMode> {
        // iOS typically has a single screen mode
        return mapOf(0 to DefaultIOSScreenMode())
    }

    override fun setScreenMode(id: Int, settings: GameSettings) {
        // iOS doesn't support changing refresh rates or screen modes programmatically here
    }

    override fun hasUserSelectableSize(id: Int): Boolean = false

    private class DefaultIOSScreenMode : ScreenMode {
        override fun getId(): Int = 0
        override fun hasUserSelectableSize(): Boolean = false
    }

    /**
     * Scene-aware key window lookup.
     *
     * Prefers:
     *  1) window from connected UIWindowScene that isKeyWindow
     *  2) first window from that scene
     *  3) UIApplication.keyWindow
     *  4) UIApplication.windows?.firstOrNull()
     */
    private fun findKeyWindow(app: UIApplication): UIWindow? {
        try {
            // connectedScenes is an NSSet of UIScene (since iOS 13)
            val scenes = app.connectedScenes as? NSSet<*>
            if (scenes != null) {
                val iter = scenes.iterator()
                while (iter.hasNext()) {
                    val scene = iter.next()
                    if (scene is UIWindowScene) {
                        val sceneWindows = scene.windows
                        if (sceneWindows != null) {
                            // prefer the key window in the scene
                            val key = sceneWindows.firstOrNull { w -> w.isKeyWindow }
                            if (key != null) return key
                            // otherwise return the first window in the scene
                            if (sceneWindows.isNotEmpty()) return sceneWindows[0]
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
            // If anything fails, fall through to older APIs
        }

        // Fallbacks for pre-iOS13 or when scenes aren't available
        try {
            val keyWindow = app.keyWindow
            if (keyWindow != null) return keyWindow
        } catch (ignored: Exception) {}

        try {
            val windows = app.windows
            if (!windows.isNullOrEmpty()) return windows[0]
        } catch (ignored: Exception) {}

        return null
    }
}
