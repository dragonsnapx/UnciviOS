package com.unciv.app.ios

import com.unciv.models.metadata.GameSettings
import com.unciv.utils.PlatformDisplay
import com.unciv.utils.ScreenMode
import com.unciv.utils.ScreenOrientation
import org.robovm.apple.coregraphics.CGAffineTransform
import org.robovm.apple.coregraphics.CGRect
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
    private var useCutoutArea = true

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
            if (rootVCViewInsets.hasInsets()) return true
        }

        // Fallback to window safe area insets
        val insets = window.safeAreaInsets
        return insets?.hasInsets() == true
    }

    override fun setCutout(enabled: Boolean) {
        useCutoutArea = enabled
        applySafeAreaFrame()
    }

    internal fun applySafeAreaFrame() {
        val app = UIApplication.getSharedApplication()
        val window = findKeyWindow(app) ?: return
        val rootView = window.rootViewController?.view ?: return
        val contentFrame = getContentFrame(window)

        window.backgroundColor = UIColor.black()
        rootView.transform = CGAffineTransform.Identity()
        rootView.frame = CGRect(0.0, 0.0, contentFrame.width, contentFrame.height)
        rootView.transform = CGAffineTransform.createTranslation(contentFrame.x, contentFrame.y)
    }

    internal fun getContentFrame(window: UIWindow): CGRect {
        val bounds = window.bounds
        if (useCutoutArea) return bounds

        val insets = window.safeAreaInsets ?: return bounds
        val orientation = window.windowScene?.interfaceOrientation
        val isLandscape = orientation == UIInterfaceOrientation.LandscapeLeft ||
            orientation == UIInterfaceOrientation.LandscapeRight

        if (isLandscape) {
            val sideInset = insets.left.coerceAtLeast(insets.right)
            return if (sideInset > 0.0 && bounds.width > sideInset * 2.0)
                CGRect(sideInset, 0.0, bounds.width - sideInset * 2.0, bounds.height)
            else bounds
        }

        return if (insets.top > 0.0 && bounds.height > insets.top)
            CGRect(0.0, insets.top, bounds.width, bounds.height - insets.top)
        else bounds
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

    private fun UIEdgeInsets.hasInsets() =
        top > 0.0 || left > 0.0 || bottom > 0.0 || right > 0.0
}
