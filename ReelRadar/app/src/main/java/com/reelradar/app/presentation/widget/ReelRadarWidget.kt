package com.reelradar.app.presentation.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.reelradar.app.MainActivity
import com.reelradar.app.data.local.db.ReelRadarDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent)
interface WidgetEntryPoint {
    fun database(): ReelRadarDatabase
}

class ReelRadarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val today = LocalDate.now().toString()
        val record = try {
            entryPoint.database().dailyRecordDao.getRecordByDateSync(today)
        } catch (e: Exception) {
            null
        }

        val count = record?.count ?: 0
        val limit = record?.dailyLimit ?: 50
        val progress = (count.toFloat() / limit.coerceAtLeast(1)).coerceIn(0f, 1f)
        val percent = (progress * 100).toInt()

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF0F111A)))
                    .cornerRadius(18.dp)
                    .padding(14.dp)
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circular Progress Indicator
                    Box(
                        modifier = GlanceModifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ColorProvider(if (count >= limit) Color(0xFFFF7675) else Color(0xFF6C5CE7)),
                            modifier = GlanceModifier.fillMaxSize()
                        )
                        Text(
                            text = "$percent%",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(14.dp))

                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ReelRadar",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF00CEC9)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "$count / $limit",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (count <= limit) "On track today" else "Limit reached!",
                            style = TextStyle(
                                color = ColorProvider(Color.LightGray),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

