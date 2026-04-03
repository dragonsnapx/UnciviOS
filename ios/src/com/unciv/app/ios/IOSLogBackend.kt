package com.unciv.app.ios

import com.unciv.utils.LogBackend
import com.unciv.utils.Tag
import org.robovm.apple.foundation.Foundation

/**
 * iOS implementation of LogBackend using NSLog for console output.
 *
 * Debug detection: iOS builds are considered debug if assertions are enabled.
 * This can be controlled in Xcode build settings (SWIFT_ACTIVE_COMPILATION_CONDITIONS).
 */
class IOSLogBackend : LogBackend {

    private val isRelease: Boolean = assertionsDisabled()

    private fun assertionsDisabled() =
        try {
            assert(false)
            true
        } catch (_: AssertionError) {
            false
        }

    override fun debug(tag: Tag, curThreadName: String, msg: String) {
        Foundation.log("[$curThreadName] [${tag.name}] $msg")
    }

    override fun error(tag: Tag, curThreadName: String, msg: String) {
        Foundation.log("ERROR [$curThreadName] [${tag.name}] $msg")
    }

    override fun isRelease(): Boolean {
        return isRelease
    }

    override fun getSystemInfo(): String {
        val device = org.robovm.apple.uikit.UIDevice.getCurrentDevice()
        val runtime = Runtime.getRuntime()

        return """
        Device: ${device.model} (${device.name})
        iOS Version: ${device.systemVersion}
        System Name: ${device.systemName}
        Java heap limit: ${runtime.maxMemory().formatMB()}
        Java heap free: ${runtime.freeMemory().formatMB()}
        """.trimIndent()
    }

    private fun Long.formatMB() = "${(this + 524288L) / 1048576L} MB"
}
