/**
 * Room 类型转换器。
 *
 * Room 原生不支持 `java.time.LocalDate`，需要通过 @TypeConverter
 * 将其与 Long（epochDay）互相转换后存到 SQLite 中。
 */
package com.liuhy.myaccount.core.database

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? {
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }
}
