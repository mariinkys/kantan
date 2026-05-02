package dev.mariinkys.kantan.data.svg

import android.content.Context
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Parses KanjiVG SVG files from assets/kanji/ and returns an ordered list of
 * Android [Path] objects — one per stroke. The caller animates these with
 * [PathMeasure] + Compose Canvas.
 *
 * KanjiVG file naming: Unicode code point in 5-hex-digit lowercase + ".svg"
 * Example: 食 (U+98DF) → "098df.svg"
 *
 * Each stroke is a `<path>` element inside a `<g id="kvg:StrokePaths_...">`.
 * The `d` attribute contains standard SVG path data (M, C, S, L, Z commands).
 */
object KanjiVGParser {

    private const val TAG = "KanjiVGParser"
    private const val ASSET_DIR = "kanji"

    /**
     * Returns stroke paths for [character], or empty list if the file doesn't
     * exist / can't be parsed. Results are intentionally NOT cached here —
     * the ViewModel / use case layer owns caching.
     */
    fun parseStrokes(context: Context, character: String): List<Path> {
        val codePoint = character.codePointAt(0)
        val fileName = "%05x.svg".format(codePoint)
        return try {
            context.assets.open("$ASSET_DIR/$fileName").use { stream ->
                parseStrokesFromStream(XmlPullParserFactory.newInstance().newPullParser().also {
                    it.setInput(stream, "UTF-8")
                })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load strokes for '$character' ($fileName): ${e.message}")
            emptyList()
        }
    }

    private fun parseStrokesFromStream(parser: XmlPullParser): List<Path> {
        val paths = mutableListOf<Path>()
        var insideStrokePaths = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    val id = parser.getAttributeValue(null, "id") ?: ""
                    if (parser.name == "g" && id.startsWith("kvg:StrokePaths")) {
                        insideStrokePaths = true
                    }
                    if (insideStrokePaths && parser.name == "path") {
                        val d = parser.getAttributeValue(null, "d")
                        if (!d.isNullOrBlank()) {
                            svgPathDataToAndroidPath(d)?.let { paths.add(it) }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "g" && insideStrokePaths) {
                        insideStrokePaths = false
                    }
                }
            }
            parser.next()
        }
        return paths
    }

    /**
     * Converts an SVG path `d` string to an Android [Path].
     *
     * KanjiVG uses: M (move-to), C (cubic bezier), S (smooth cubic),
     * L (line-to), Z (close). We handle all of these.
     */
    private fun svgPathDataToAndroidPath(d: String): Path? = runCatching {
        val path = Path()
        // Tokenize: split on command letters, keeping the letter
        val tokenRegex = Regex("""[MLCSZmlcsz]|[-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?""")
        val tokens = tokenRegex.findAll(d).map { it.value }.toList()
        var i = 0
        var cmd = 'M'
        // Last cubic control point for S continuations
        var lastCx2 = 0f
        var lastCy2 = 0f
        var lastX = 0f
        var lastY = 0f

        fun nextFloat() = tokens[i++].toFloat()

        while (i < tokens.size) {
            val t = tokens[i]
            if (t.first().isLetter()) {
                cmd = t.first(); i++
            }

            when (cmd.uppercaseChar()) {
                'M' -> {
                    val x = nextFloat()
                    val y = nextFloat()
                    if (cmd.isUpperCase()) {
                        path.moveTo(x, y); lastX = x; lastY = y
                    } else {
                        path.rMoveTo(x, y); lastX += x; lastY += y
                    }
                    lastCx2 = lastX; lastCy2 = lastY
                    // Subsequent coord pairs after M are implicit L
                    cmd = if (cmd.isUpperCase()) 'L' else 'l'
                }

                'L' -> {
                    val x = nextFloat()
                    val y = nextFloat()
                    if (cmd.isUpperCase()) {
                        path.lineTo(x, y); lastX = x; lastY = y
                    } else {
                        path.rLineTo(x, y); lastX += x; lastY += y
                    }
                    lastCx2 = lastX; lastCy2 = lastY
                }

                'C' -> {
                    val x1 = nextFloat()
                    val y1 = nextFloat()
                    val x2 = nextFloat()
                    val y2 = nextFloat()
                    val x = nextFloat()
                    val y = nextFloat()
                    if (cmd.isUpperCase()) {
                        path.cubicTo(x1, y1, x2, y2, x, y)
                        lastCx2 = x2; lastCy2 = y2; lastX = x; lastY = y
                    } else {
                        path.rCubicTo(x1, y1, x2, y2, x, y)
                        lastCx2 = lastX + x2; lastCy2 = lastY + y2
                        lastX += x; lastY += y
                    }
                }

                'S' -> {
                    // Smooth cubic: implicit first control point is reflection of previous c2
                    val x2 = nextFloat()
                    val y2 = nextFloat()
                    val x = nextFloat()
                    val y = nextFloat()
                    val cx1 = 2 * lastX - lastCx2
                    val cy1 = 2 * lastY - lastCy2
                    if (cmd.isUpperCase()) {
                        path.cubicTo(cx1, cy1, x2, y2, x, y)
                        lastCx2 = x2; lastCy2 = y2; lastX = x; lastY = y
                    } else {
                        path.cubicTo(cx1, cy1, lastX + x2, lastY + y2, lastX + x, lastY + y)
                        lastCx2 = lastX + x2; lastCy2 = lastY + y2
                        lastX += x; lastY += y
                    }
                }

                'Z' -> {
                    path.close()
                }
            }
        }
        path
    }.getOrNull()
}