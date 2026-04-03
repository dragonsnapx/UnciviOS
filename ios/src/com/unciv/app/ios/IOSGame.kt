package com.unciv.app.ios

import com.badlogic.gdx.Gdx
import com.unciv.UncivGame
import com.unciv.utils.Display
import org.robovm.apple.uikit.UIInterfaceOrientationMask

/**
 * iOS-specific game extension.
 *
 * Extends UncivGame with iOS-specific functionality including
 * orientation management and deep linking support.
 */
class IOSGame : UncivGame() {

    /**
     * Get preferred interface orientation based on display settings.
     * This is called by iOS to determine which orientations are allowed.
     */
    fun getPreferredInterfaceOrientationMask(): UIInterfaceOrientationMask {
        val iosDisplay = Display.platform as? IOSDisplay
        val orientation = iosDisplay?.getPreferredOrientation()

        return when (orientation) {
            com.unciv.utils.ScreenOrientation.Landscape ->
                UIInterfaceOrientationMask.Landscape

            com.unciv.utils.ScreenOrientation.Portrait ->
                UIInterfaceOrientationMask.Portrait

            else -> // Auto
                UIInterfaceOrientationMask.All
        }
    }

    /**
     * Handle deep linking for multiplayer games.
     * URL scheme: unciv://multiplayer?id=GAME_ID
     */
    fun handleDeepLink(url: String) {
        try {
            // Parse URL to extract game ID
            val uri = java.net.URI(url)
            if (uri.scheme == "unciv" && uri.host == "multiplayer") {
                val query = uri.query ?: return
                val params = query.split("&").associate {
                    val parts = it.split("=")
                    parts[0] to (parts.getOrNull(1) ?: "")
                }
                deepLinkedMultiplayerGame = params["id"]
            }
        } catch (e: Exception) {
            // Invalid URL, ignore
            Gdx.app.error("IOSGame", "Failed to parse deep link: $url", e)
        }
    }
}