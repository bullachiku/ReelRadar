package com.reelradar.app.data.local.db.converter

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromHourlyList(list: List<Int>?): String {
        if (list == null || list.isEmpty()) {
            return List(24) { 0 }.joinToString(",")
        }
        return list.joinToString(",")
    }

    @TypeConverter
    fun toHourlyList(data: String?): List<Int> {
        if (data.isNullOrEmpty()) {
            return List(24) { 0 }
        }
        val items = data.split(",").mapNotNull { it.toIntOrNull() }
        return if (items.size == 24) items else List(24) { index -> items.getOrElse(index) { 0 } }
    }
}
