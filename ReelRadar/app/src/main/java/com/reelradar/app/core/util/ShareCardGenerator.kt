package com.reelradar.app.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import com.reelradar.app.domain.model.DailyRecord
import com.reelradar.app.domain.model.StreakInfo
import java.io.File
import java.io.FileOutputStream

object ShareCardGenerator {

    fun generateAndGetShareIntent(
        context: Context,
        record: DailyRecord,
        streakInfo: StreakInfo
    ): Intent {
        val width = 1080
        val height = 1350 // 4:5 Instagram feed / story portrait ratio
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#0F111A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Decorative background gradient or rounded card
        val cardPaint = Paint().apply {
            color = Color.parseColor("#161926")
            style = Paint.Style.FILL
        }
        val cardRect = RectF(60f, 100f, (width - 60).toFloat(), (height - 100).toFloat())
        canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

        // Card Border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#6C5CE7")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, borderPaint)

        // Title: ReelRadar
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 64f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ReelRadar", width / 2f, 220f, titlePaint)

        val subPaint = Paint().apply {
            color = Color.parseColor("#00CEC9")
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Mindful Screen Time Summary", width / 2f, 280f, subPaint)

        // Date text
        val datePaint = Paint().apply {
            color = Color.parseColor("#AAAAAA")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Date: ${record.date}", width / 2f, 340f, datePaint)

        // Big Progress Ring
        val ringCenterX = width / 2f
        val ringCenterY = 580f
        val ringRadius = 180f

        val ringBgPaint = Paint().apply {
            color = Color.parseColor("#212538")
            style = Paint.Style.STROKE
            strokeWidth = 36f
        }
        canvas.drawCircle(ringCenterX, ringCenterY, ringRadius, ringBgPaint)

        val progress = (record.count.toFloat() / record.dailyLimit.coerceAtLeast(1)).coerceIn(0f, 1f)
        val ringActivePaint = Paint().apply {
            color = if (record.count <= record.dailyLimit) Color.parseColor("#6C5CE7") else Color.parseColor("#FF7675")
            style = Paint.Style.STROKE
            strokeWidth = 36f
            strokeCap = Paint.Cap.ROUND
        }
        val ringOval = RectF(
            ringCenterX - ringRadius,
            ringCenterY - ringRadius,
            ringCenterX + ringRadius,
            ringCenterY + ringRadius
        )
        canvas.drawArc(ringOval, -90f, progress * 360f, false, ringActivePaint)

        // Count inside ring
        val countPaint = Paint().apply {
            color = Color.WHITE
            textSize = 96f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${record.count}", ringCenterX, ringCenterY + 15f, countPaint)

        val limitPaint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("of ${record.dailyLimit} Reels", ringCenterX, ringCenterY + 75f, limitPaint)

        // Stats Box: Time Spent & Streak
        val mins = record.estimatedSeconds / 60
        val statBoxY = 860f

        val statTitlePaint = Paint().apply {
            color = Color.parseColor("#AAAAAA")
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        val statValPaint = Paint().apply {
            color = Color.WHITE
            textSize = 52f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        // Left: Time Spent
        canvas.drawText("Time Spent", width * 0.32f, statBoxY, statTitlePaint)
        canvas.drawText("${mins}m", width * 0.32f, statBoxY + 65f, statValPaint)

        // Right: Streak
        canvas.drawText("Active Streak", width * 0.68f, statBoxY, statTitlePaint)
        canvas.drawText("${streakInfo.currentStreak} Days", width * 0.68f, statBoxY + 65f, statValPaint)

        // Footer badge
        val footerPaint = Paint().apply {
            color = if (record.count <= record.dailyLimit) Color.parseColor("#00B894") else Color.parseColor("#FF7675")
            textSize = 36f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val statusText = if (record.count <= record.dailyLimit) {
            "🏆 Daily Goal Mastered!"
        } else {
            "⚠️ Daily Limit Exceeded"
        }
        canvas.drawText(statusText, width / 2f, 1080f, footerPaint)

        // Save Bitmap to Cache
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val imageFile = File(cachePath, "reelradar_share_card.png")
        val stream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, "Staying mindful with ReelRadar! Scrolled ${record.count} reels today. 🎯")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
