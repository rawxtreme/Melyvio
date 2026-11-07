/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.utils

import androidx.core.content.ContextCompat
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.aaditya.melyvio.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/** Draws the branded "My Music DNA" picture (1080 x 1920, Instagram-story size) and shares it. */
object MusicDnaImage {
    private const val W = 1080
    private const val H = 1920

    private fun text(
        size: Float,
        color: Int,
        bold: Boolean = false,
        black: Boolean = false,
        spacing: Float = 0f,
    ) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface =
            when {
                black -> Typeface.create("sans-serif-black", Typeface.NORMAL)
                bold -> Typeface.create("sans-serif-medium", Typeface.BOLD)
                else -> Typeface.create("sans-serif", Typeface.NORMAL)
            }
        letterSpacing = spacing
    }

    private fun Canvas.line(
        value: String,
        x: Float,
        y: Float,
        paint: TextPaint,
        maxWidth: Float,
        alignRight: Boolean = false,
    ) {
        val cut = TextUtils.ellipsize(value, paint, maxWidth, TextUtils.TruncateAt.END).toString()
        val drawX = if (alignRight) x - paint.measureText(cut) else x
        drawText(cut, drawX, y, paint)
    }

    private fun Canvas.glow(
        cx: Float,
        cy: Float,
        radius: Float,
        color: Int,
        alpha: Int,
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader =
            RadialGradient(
                cx,
                cy,
                radius,
                Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
        drawCircle(cx, cy, radius, p)
    }

    private fun Canvas.helix(
        cx: Float,
        top: Float,
        bottom: Float,
        amp: Float,
        color: Int,
    ) {
        val steps = 90
        val strand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 7f
            strokeCap = Paint.Cap.ROUND
        }
        val rung = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 3f
            this.color = Color.argb(46, 255, 255, 255)
        }
        var px1 = 0f
        var px2 = 0f
        var py = 0f
        for (i in 0..steps) {
            val y = top + (bottom - top) * i / steps
            val a = i * 0.28f
            val x1 = cx + amp * sin(a)
            val x2 = cx - amp * sin(a)
            if (i % 3 == 0) drawLine(x1, y, x2, y, rung)
            if (i > 0) {
                strand.color = Color.argb((60 + 70 * (0.5f + 0.5f * kotlin.math.cos(a))).toInt(), Color.red(color), Color.green(color), Color.blue(color))
                drawLine(px1, py, x1, y, strand)
                strand.color = Color.argb((60 + 70 * (0.5f - 0.5f * kotlin.math.cos(a))).toInt(), 255, 255, 255)
                drawLine(px2, py, x2, y, strand)
            }
            px1 = x1
            px2 = x2
            py = y
        }
    }

    fun render(
        context: Context,
        data: MusicDnaData,
        periodLabel: String,
        accent: Int,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint()
        bg.shader =
            LinearGradient(
                0f, 0f, 0f, H.toFloat(),
                intArrayOf(0xFF140A33.toInt(), 0xFF2B1259.toInt(), 0xFF0B1230.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)
        c.glow(W * 0.88f, H * 0.16f, 720f, accent, 110)
        c.glow(W * 0.05f, H * 0.88f, 680f, 0xFFB5179E.toInt(), 80)
        c.helix(W - 120f, 330f, H - 330f, 52f, accent)

        // brand: the single-colour Melyvio mark on a gradient tile
        val logoSize = 132f
        val tile = RectF(72f, 92f, 72f + logoSize, 92f + logoSize)
        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        tilePaint.shader = LinearGradient(tile.left, tile.top, tile.right, tile.bottom, accent, 0xFFB5179E.toInt(), Shader.TileMode.CLAMP)
        c.drawRoundRect(tile, 34f, 34f, tilePaint)
        BitmapFactory.decodeResource(context.resources, R.drawable.about_logo)?.let { mark ->
            val pad = logoSize * 0.14f
            c.drawBitmap(mark, null, RectF(tile.left + pad, tile.top + pad, tile.right - pad, tile.bottom - pad), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        c.drawText("MELYVIO", 232f, 178f, text(58f, Color.WHITE, bold = true, spacing = 0.22f))
        c.drawText("Music DNA", 234f, 222f, text(34f, Color.argb(170, 255, 255, 255)))

        // heading
        c.drawText("MY MUSIC DNA", 72f, 430f, text(112f, Color.WHITE, black = true))
        c.drawText(periodLabel.uppercase(), 76f, 492f, text(36f, Color.argb(170, 255, 255, 255), spacing = 0.18f))
        val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        c.drawRoundRect(RectF(76f, 520f, 236f, 530f), 5f, 5f, bar)

        // personality card
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(30, 255, 255, 255) }
        val cardEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(60, 255, 255, 255)
        }
        val box = RectF(72f, 580f, 1008f, 930f)
        c.drawRoundRect(box, 48f, 48f, card)
        c.drawRoundRect(box, 48f, 48f, cardEdge)
        ContextCompat.getDrawable(context, data.persona.icon)?.mutate()?.let { icon ->
            icon.setTint(accent)
            icon.setBounds(112, 668, 112 + 150, 668 + 150)
            icon.draw(c)
        }
        c.line(data.persona.title.uppercase(), 300f, 690f, text(54f, accent, black = true), 680f)
        val descPaint = text(36f, Color.argb(220, 255, 255, 255))
        val layout =
            StaticLayout.Builder
                .obtain(data.persona.description, 0, data.persona.description.length, descPaint, 660)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(8f, 1f)
                .setMaxLines(4)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
        c.save()
        c.translate(300f, 722f)
        layout.draw(c)
        c.restore()

        // stat cards
        val stats =
            listOf(
                "LISTENING TIME" to formatDnaDuration(data.totalMinutes),
                "SONGS PLAYED" to data.totalPlays.toString(),
                "UNIQUE SONGS" to data.uniqueSongs.toString(),
            )
        val cw = 288f
        stats.forEachIndexed { i, (label, value) ->
            val x = 72f + i * (cw + 36f)
            val r = RectF(x, 970f, x + cw, 1130f)
            c.drawRoundRect(r, 36f, 36f, card)
            c.drawRoundRect(r, 36f, 36f, cardEdge)
            c.line(value, x + 28f, 1050f, text(60f, Color.WHITE, black = true), cw - 56f)
            c.line(label, x + 28f, 1100f, text(24f, Color.argb(170, 255, 255, 255), spacing = 0.12f), cw - 56f)
        }

        // top artists
        c.drawText("TOP ARTISTS", 72f, 1210f, text(36f, accent, bold = true, spacing = 0.2f))
        val nameFirst = text(48f, Color.WHITE, black = true)
        val nameRest = text(42f, Color.WHITE, bold = true)
        val plays = text(32f, Color.argb(170, 255, 255, 255))
        val rank = text(42f, accent, black = true)
        data.topArtists.take(5).forEachIndexed { i, artist ->
            val y = 1282f + i * 82f
            c.drawText("${i + 1}", 72f, y, rank)
            c.line(artist.name, 140f, y, if (i == 0) nameFirst else nameRest, 600f)
            c.line("${artist.plays} plays", 1008f, y, plays, 240f, alignRight = true)
        }

        // favourite genre + most played song
        val label = text(26f, Color.argb(170, 255, 255, 255), spacing = 0.16f)
        val value = text(46f, Color.WHITE, black = true)
        c.drawText("FAVOURITE GENRE", 72f, 1722f, label)
        c.line(data.favouriteGenre, 72f, 1776f, value, 440f)
        c.drawText("MOST PLAYED SONG", 556f, 1722f, label)
        c.line(data.topSongs.firstOrNull()?.title ?: "-", 556f, 1776f, value, 452f)

        // footer
        val footer = text(34f, Color.argb(200, 255, 255, 255), bold = true)
        val msg = "Discover yours on Melyvio"
        c.drawText(msg, (W - footer.measureText(msg)) / 2f, 1868f, footer)
        return bmp
    }

    suspend fun share(
        context: Context,
        data: MusicDnaData,
        periodLabel: String,
        accent: Int,
        shareText: String,
    ) {
        val file =
            withContext(Dispatchers.Default) {
                val bitmap = render(context, data, periodLabel, accent)
                val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
                val out = File(dir, "melyvio_music_dna.png")
                out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                out
            }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".FileProvider", file)
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
