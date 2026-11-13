package com.reelradar.app.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelradar.app.domain.model.DailyRecord
import com.reelradar.app.domain.model.ReelSession
import com.reelradar.app.domain.repository.ReelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class InsightsUiState(
    val records: List<DailyRecord> = emptyList(),
    val totalReelsTracked: Int = 0,
    val totalTimeSeconds: Long = 0,
    val hourlyTotals: List<Int> = List(24) { 0 },
    val weekdayAverages: Map<DayOfWeek, Float> = emptyMap(),
    val peakDoomscrollWindow: String = "10:00 PM - 12:00 AM",
    val longestSession: ReelSession? = null,
    val weeklyTotalReels: Int = 0,
    val weeklyDailyAverage: Int = 0,
    val weeklyPercentChange: Int = 0
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val reelRepository: ReelRepository
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = combine(
        reelRepository.getAllRecords(),
        reelRepository.getLongestSession()
    ) { records, longestSession ->
        if (records.isEmpty()) {
            return@combine InsightsUiState()
        }

        val totalReels = records.sumOf { it.count }
        val totalSecs = records.sumOf { it.estimatedSeconds }

        // Hourly aggregation
        val hourlyAggregate = MutableList(24) { 0 }
        for (r in records) {
            for (h in 0 until 24) {
                hourlyAggregate[h] += r.hourlyCounts.getOrElse(h) { 0 }
            }
        }

        // Peak doomscroll window calculation (sliding 2-hour window)
        var maxWindowSum = -1
        var peakStartHour = 22
        for (h in 0 until 24) {
            val sum = hourlyAggregate[h] + hourlyAggregate[(h + 1) % 24]
            if (sum > maxWindowSum) {
                maxWindowSum = sum
                peakStartHour = h
            }
        }
        val peakWindowFormatted = formatHourWindow(peakStartHour, (peakStartHour + 2) % 24)

        // Weekday aggregation
        val weekdayMap = mutableMapOf<DayOfWeek, MutableList<Int>>()
        DayOfWeek.values().forEach { weekdayMap[it] = mutableListOf() }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        for (r in records) {
            try {
                val date = LocalDate.parse(r.date, formatter)
                weekdayMap[date.dayOfWeek]?.add(r.count)
            } catch (e: Exception) {}
        }

        val weekdayAverages = weekdayMap.mapValues { (_, counts) ->
            if (counts.isEmpty()) 0f else counts.average().toFloat()
        }

        // Weekly recap
        val last7Days = records.take(7)
        val weeklyTotal = last7Days.sumOf { it.count }
        val weeklyAvg = if (last7Days.isNotEmpty()) weeklyTotal / last7Days.size else 0

        val prev7Days = records.drop(7).take(7)
        val prevWeeklyTotal = prev7Days.sumOf { it.count }
        val percentChange = if (prevWeeklyTotal > 0) {
            ((weeklyTotal - prevWeeklyTotal).toFloat() / prevWeeklyTotal * 100).toInt()
        } else 0

        InsightsUiState(
            records = records,
            totalReelsTracked = totalReels,
            totalTimeSeconds = totalSecs,
            hourlyTotals = hourlyAggregate,
            weekdayAverages = weekdayAverages,
            peakDoomscrollWindow = peakWindowFormatted,
            longestSession = longestSession,
            weeklyTotalReels = weeklyTotal,
            weeklyDailyAverage = weeklyAvg,
            weeklyPercentChange = percentChange
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState()
    )

    private fun formatHourWindow(startHour: Int, endHour: Int): String {
        fun formatH(h: Int): String {
            val hour12 = if (h % 12 == 0) 12 else h % 12
            val amPm = if (h < 12) "AM" else "PM"
            return "$hour12:00 $amPm"
        }
        return "${formatH(startHour)} - ${formatH(endHour)}"
    }
}
