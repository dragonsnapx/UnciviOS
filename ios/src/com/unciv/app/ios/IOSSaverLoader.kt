package com.unciv.app.ios

import com.unciv.logic.files.PlatformSaverLoader

/**
 * iOS implementation of PlatformSaverLoader.
 *
 * NOTE: This is a simplified stub implementation to get iOS building.
 * Full file picker functionality using UIDocumentPickerViewController
 * will be added in a future enhancement.
 *
 * For now, users should use the standard save/load functionality
 * through the game's internal file system.
 */
class IOSSaverLoader : PlatformSaverLoader {

    override fun saveGame(
        data: String,
        suggestedLocation: String,
        onSaved: (location: String) -> Unit,
        onError: (ex: Exception) -> Unit
    ) {
        // Stub implementation - file picker not yet implemented
        // Users will use the game's internal save system
        onError(Exception("External save not yet implemented on iOS. Please use the game's internal save system."))
    }

    override fun loadGame(
        onLoaded: (data: String, location: String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Stub implementation - file picker not yet implemented
        // Users will use the game's internal load system
        onError(Exception("External load not yet implemented on iOS. Please use the game's internal load system."))
    }
}
