package com.unciv.app.ios

import com.badlogic.gdx.graphics.Pixmap
import com.unciv.UncivGame
import com.unciv.ui.components.fonts.FontFamilyData
import com.unciv.ui.components.fonts.FontImplementation
import com.unciv.ui.components.fonts.FontMetricsCommon
import com.unciv.ui.components.fonts.Fonts
import com.unciv.utils.Log
import org.robovm.apple.coregraphics.*
import org.robovm.apple.coretext.CTLine
import org.robovm.apple.foundation.NSAttributedString
import org.robovm.apple.foundation.NSDictionary
import org.robovm.apple.foundation.NSObject
import org.robovm.apple.foundation.NSString
import org.robovm.apple.uikit.NSAttributedStringAttribute
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIFont
import org.robovm.apple.uikit.UIGraphics
import kotlin.math.ceil

class IOSFont : FontImplementation {

    private var uiFont: UIFont = UIFont.getSystemFont(18.0)
    private var currentSize: Int = 18
    private var currentFontFamily: String? = null

    override fun setFontFamily(fontFamilyData: FontFamilyData, size: Int) {
        currentSize = size

        // Don’t reload if unchanged
        if (currentFontFamily == fontFamilyData.invariantName) return
        currentFontFamily = fontFamilyData.invariantName

        uiFont =
            if (fontFamilyData.filePath != null) {
                // Option A-ish: load font from file if possible, else fallback.
                // NOTE: This only works if you register the font (see comment below).
                loadCustomUIFont(fontFamilyData.filePath!!, size)
                    ?: UIFont.getFont(Fonts.DEFAULT_FONT_FAMILY, size.toDouble())
                    ?: UIFont.getSystemFont(size.toDouble())
            } else {
                UIFont.getFont(fontFamilyData.invariantName, size.toDouble())
                    ?: UIFont.getFont(Fonts.DEFAULT_FONT_FAMILY, size.toDouble())
                    ?: UIFont.getSystemFont(size.toDouble())
            }
    }

    override fun getFontSize(): Int = currentSize

    override fun getCharPixmap(symbolString: String): Pixmap {
        val metrics = getMetrics()
        
        val ns = NSString(symbolString)
        val attrs: NSDictionary<NSString, NSObject> = NSDictionary(
            NSAttributedStringAttribute.Font.value(), uiFont,
            NSAttributedStringAttribute.ForegroundColor.value(), UIColor.white()
        )

        val measured = ns.getSize(attrs)
        var width = ceil(measured.width).toInt()
        var height = ceil(metrics.height.toDouble()).toInt()

        if (width <= 0) { // spaces/tabs
            height = getFontSize()
            width = height
        }

        // small padding to avoid clipping
        val pad = 2
        val w = width + pad * 2
        val h = height + pad * 2

        // Create RGBA Pixmap output
        val pixmap = Pixmap(w, h, Pixmap.Format.RGBA8888)

        // Backing store for CoreGraphics (BGRA premultiplied)
        val bytesPerRow = w * 4
        val buffer = ByteArray(h * bytesPerRow)

        val colorSpace = CGColorSpace.createDeviceRGB()

        val info = CGBitmapInfo(
            CGImageAlphaInfo.PremultipliedFirst.value() or CGBitmapInfo.ByteOrder32Little.value()
        )

        val ctx = CGBitmapContext.create(
            buffer,
            w.toLong(),
            h.toLong(),
            8,
            bytesPerRow.toLong(),
            colorSpace,
            info
        )

        // Clear transparent
        ctx.setBlendMode(CGBlendMode.Clear)
        ctx.fillRect(CGRect(0.0, 0.0, w.toDouble(), h.toDouble()))
        ctx.setBlendMode(CGBlendMode.Normal)

        // Flip to UIKit coordinate system
        ctx.translateCTM(0.0, h.toDouble())
        ctx.scaleCTM(1.0, -1.0)

        val attrStr = NSAttributedString(symbolString, attrs)            // wrap NSString + attributes
        val ctLine = CTLine.create(attrStr) // create CTLine

        // Draw at baseline: y = pad + leading + ascent (+1 matches Android's "+1f" tweak)
        val baselineY = metrics.ascent - 70f
        UIGraphics.pushContext(ctx)
        try {
            ctx.setTextPosition(pad.toDouble(), baselineY.toDouble())
            ctLine.draw(ctx)
        } finally {
            UIGraphics.popContext()
        }
        
        // Convert BGRA -> RGBA into Pixmap (matches Desktop/Android behavior)
        for (y in 0 until h) {
            val srcRowStart = (h - 1 - y) * bytesPerRow   // <-- flip vertically here
            var src = srcRowStart
            for (x in 0 until w) {
                val b = buffer[src].toInt() and 0xFF
                val g = buffer[src + 1].toInt() and 0xFF
                val r = buffer[src + 2].toInt() and 0xFF
                val a = buffer[src + 3].toInt() and 0xFF
                src += 4

                // pack into same int format you used before (Desktop used reverseBytes; this
                // matches the prior logic of (r<<24)|(g<<16)|(b<<8)|a)
                val rgba = (r shl 24) or (g shl 16) or (b shl 8) or a
                pixmap.drawPixel(x, y, rgba)
            }
        }



        return pixmap
    }

    override fun getSystemFonts(): Sequence<FontFamilyData> =
        UIFont.getFamilyNames().asSequence().map { FontFamilyData(it, it) }

    override fun getMetrics(): FontMetricsCommon {
        val ascent = uiFont.ascender.toFloat()
        val descent = (-uiFont.descender).toFloat() // descender is negative
        val height = uiFont.lineHeight.toFloat()
        val leading = (height - ascent - descent).coerceAtLeast(0f)
        return FontMetricsCommon(ascent, descent, height, leading)
    }

    /**
     * Best-effort loader. iOS won’t use a .ttf from disk unless you register it with CoreText.
     * If you don't want to do that now, return null and you’ll fall back to default/system.
     */
    private fun loadCustomUIFont(path: String, size: Int): UIFont? {
        return try {
            // If your mods are in local storage, you can at least read the file,
            // but UIFont cannot directly load arbitrary files without registration.
            // So for now, just attempt by "name" based on file stem (sometimes works if already registered).
            val file = UncivGame.Current.files.getLocalFile(path).file()
            val guessedName = file.nameWithoutExtension
            UIFont.getFont(guessedName, size.toDouble())
        } catch (t: Throwable) {
            Log.error("iOS: failed to resolve custom font, falling back", t)
            null
        }
    }
}
