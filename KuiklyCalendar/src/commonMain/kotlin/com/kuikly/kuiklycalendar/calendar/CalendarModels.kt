package com.kuikly.kuiklycalendar.calendar

/**
 * 与平台无关的公历日期。month 为 1 至 12，day 从 1 开始。
 *
 * 组件刻意不向业务方暴露时间戳，避免不同时区把同一个自然日解析成相邻日期。
 */
data class CalendarDate(val year: Int, val month: Int, val day: Int) : Comparable<CalendarDate> {
    init {
        require(month in 1..12) { "month 必须在 1 到 12 之间" }
        require(day in 1..CalendarMath.daysInMonth(year, month)) { "day 不在当月有效范围内" }
    }

    override fun compareTo(other: CalendarDate): Int {
        return when {
            year != other.year -> year.compareTo(other.year)
            month != other.month -> month.compareTo(other.month)
            else -> day.compareTo(other.day)
        }
    }

    fun format(): String = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

/** 当前显示的月份。 */
data class CalendarMonth(val year: Int, val month: Int) {
    init {
        require(month in 1..12) { "month 必须在 1 到 12 之间" }
    }

    fun previous(): CalendarMonth = if (month == 1) CalendarMonth(year - 1, 12) else CalendarMonth(year, month - 1)

    fun next(): CalendarMonth = if (month == 12) CalendarMonth(year + 1, 1) else CalendarMonth(year, month + 1)

    fun title(): String = "${year}年${month}月"
}

/** 星期顺序以周日为 0，便于与 Kuikly CalendarModule 的 DAY_OF_WEEK 对齐。 */
enum class CalendarWeekday(val index: Int) {
    SUNDAY(0), MONDAY(1), TUESDAY(2), WEDNESDAY(3), THURSDAY(4), FRIDAY(5), SATURDAY(6)
}

/** 单个日期的事件标记；默认渲染为最多三个圆点。 */
data class CalendarMarker(
    val colors: List<Long> = listOf(0xFF4F8FFF),
    val accessibilityLabel: String = "有日程"
)

/** 传给 dayContent 插槽的完整状态，便于业务保留默认交互而只替换内容。 */
data class CalendarDayState(
    val date: CalendarDate,
    val isCurrentMonth: Boolean,
    val isSelected: Boolean,
    val isToday: Boolean,
    val isDisabled: Boolean,
    val marker: CalendarMarker?
)

/** 日历主题。所有颜色均为 ARGB Long，与 Kuikly Color 构造方式保持一致。 */
data class CalendarTheme(
    val backgroundColor: Long = 0xFFFFFFFF,
    val titleColor: Long = 0xFF1F2937,
    val weekdayColor: Long = 0xFF6B7280,
    val dayTextColor: Long = 0xFF1F2937,
    val adjacentMonthTextColor: Long = 0xFFB8C0CC,
    val disabledTextColor: Long = 0xFFD1D5DB,
    val todayTextColor: Long = 0xFF4F8FFF,
    val selectedBackgroundColor: Long = 0xFF4F8FFF,
    val selectedTextColor: Long = 0xFFFFFFFF,
    val markerColor: Long = 0xFF4F8FFF,
    val dividerColor: Long = 0xFFF1F5F9
)

/**
 * 纯 Kotlin 公历算法：不依赖 java.time、kotlinx-datetime 或原生日期控件。
 * 因而 Android、iOS、鸿蒙和 H5 的月份网格完全一致。
 */
internal object CalendarMath {
    private val monthOffsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)

    fun isLeapYear(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        2 -> if (isLeapYear(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    /** 返回 0（周日）至 6（周六）。 */
    fun dayOfWeek(year: Int, month: Int, day: Int): Int {
        var adjustedYear = year
        if (month < 3) adjustedYear--
        return (adjustedYear + adjustedYear / 4 - adjustedYear / 100 + adjustedYear / 400 + monthOffsets[month - 1] + day) % 7
    }

    fun addDays(date: CalendarDate, delta: Int): CalendarDate {
        var year = date.year
        var month = date.month
        var day = date.day
        var remaining = delta
        while (remaining > 0) {
            if (day < daysInMonth(year, month)) day++ else {
                day = 1
                if (month == 12) {
                    month = 1
                    year++
                } else month++
            }
            remaining--
        }
        while (remaining < 0) {
            if (day > 1) day-- else {
                if (month == 1) {
                    month = 12
                    year--
                } else month--
                day = daysInMonth(year, month)
            }
            remaining++
        }
        return CalendarDate(year, month, day)
    }
}
