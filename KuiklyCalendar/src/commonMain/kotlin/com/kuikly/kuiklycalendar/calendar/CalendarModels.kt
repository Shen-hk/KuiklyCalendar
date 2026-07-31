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

/** 单个日期的事件标记；默认渲染为一个低干扰圆点。 */
data class CalendarMarker(
    val colors: List<Long> = listOf(0xFF4F8FFF),
    val accessibilityLabel: String = "有日程"
)

/** 为日期格提供农历、节日、调休等补充信息；基础日历不绑定具体地区数据。 */
interface CalendarSupplementaryProvider {
    fun supplementaryLabel(date: CalendarDate): String?

    fun isHoliday(date: CalendarDate): Boolean = false

    fun isWorkdayOverride(date: CalendarDate): Boolean = false
}

/**
 * 月历单元格可显示的轻量日程摘要。
 *
 * 组件只渲染当前 42 格可见范围内的摘要，不负责网络、鉴权、缓存或详情页跳转。
 */
data class CalendarEventSummary(
    val id: String,
    val startDate: CalendarDate,
    val endDate: CalendarDate = startDate,
    val title: String = "",
    val color: Long = 0xFF4F8FFF,
    val type: String = "default",
    val isAllDay: Boolean = true,
    val extra: Map<String, String> = emptyMap(),
    /** 节日、纪念日等可在 Agenda 中保留，但不计入月历日期格的日程点。 */
    val showsCalendarIndicator: Boolean = true
) {
    init {
        require(id.isNotBlank()) { "日程 id 不能为空" }
        require(endDate >= startDate) { "日程结束日期不能早于开始日期" }
    }

    fun occursOn(date: CalendarDate): Boolean = date >= startDate && date <= endDate
}

/** 限制 Calendar 日程声明 DSL 内的接收者解析范围。 */
@DslMarker
annotation class CalendarDslMarker

/**
 * 创建一份独立的日程摘要列表快照。
 *
 * 适用于 Calendar 与 CalendarAgenda 共用固定日程，或在创建 View 前预先组装数据的场景。
 */
fun calendarEvents(init: CalendarEventsDsl.() -> Unit): List<CalendarEventSummary> =
    CalendarEventsDsl().apply(init).build()

/** Calendar 与 CalendarAgenda 共用的声明式日程 DSL。 */
@CalendarDslMarker
class CalendarEventsDsl {
    private val items = mutableListOf<CalendarEventSummary>()

    /** 在配置块中声明完整的日程点。 */
    fun event(init: CalendarEventDsl.() -> Unit) {
        items += CalendarEventDsl().apply(init).build()
    }

    /** 预先传入必填字段的日程点。 */
    fun event(
        id: String,
        startDate: CalendarDate,
        endDate: CalendarDate = startDate,
        init: CalendarEventDsl.() -> Unit = {}
    ) {
        items += CalendarEventDsl(id, startDate, endDate).apply(init).build()
    }

    /** 单日事件简写，无需重复传入结束日期。 */
    fun event(
        id: String,
        date: CalendarDate,
        init: CalendarEventDsl.() -> Unit = {}
    ) {
        event(id = id, startDate = date, endDate = date, init = init)
    }

    /** 保留已有事件模型或转换后的业务响应。 */
    fun add(event: CalendarEventSummary) {
        items += event
    }

    fun build(): List<CalendarEventSummary> = items.toList()
}

/** [CalendarEventsDsl.event] 使用的单个日程配置。 */
@CalendarDslMarker
class CalendarEventDsl(
    var id: String = "",
    var startDate: CalendarDate? = null,
    var endDate: CalendarDate? = null
) {
    var title: String = ""
    var color: Long = 0xFF4F8FFF
    var type: String = "default"
    var isAllDay: Boolean = true
    var showsCalendarIndicator: Boolean = true
    var extra: Map<String, String> = emptyMap()

    /** 写入或替换随事件透传的业务字段。 */
    fun extra(key: String, value: String) {
        extra = extra + (key to value)
    }

    /** 在仅配置块的 event 形式中设置单日日期。 */
    fun on(date: CalendarDate) {
        startDate = date
        endDate = date
    }

    /** 在仅配置块的 event 形式中设置包含起止日的范围。 */
    fun during(start: CalendarDate, end: CalendarDate) {
        startDate = start
        endDate = end
    }

    internal fun build(): CalendarEventSummary {
        val date = requireNotNull(startDate) { "Schedule startDate cannot be null" }
        return CalendarEventSummary(
            id = id,
            startDate = date,
            endDate = endDate ?: date,
            title = title,
            color = color,
            type = type,
            isAllDay = isAllDay,
            extra = extra,
            showsCalendarIndicator = showsCalendarIndicator
        )
    }
}

/** 当前月格（含相邻月补位）的可见日期范围，起止日期均包含。 */
data class CalendarVisibleRange(
    val month: CalendarMonth,
    val start: CalendarDate,
    val end: CalendarDate
)

/** 日程摘要在日期格中的默认展示方式。 */
enum class CalendarEventDisplayMode {
    DOTS,
    COUNT_BADGE,
    CUSTOM
}

/** 传给 dayContent 插槽的完整状态，便于业务保留默认交互而只替换内容。 */
data class CalendarDayState(
    val date: CalendarDate,
    val isCurrentMonth: Boolean,
    val isSelected: Boolean,
    val selectionPosition: CalendarSelectionPosition,
    val isInRange: Boolean,
    val isToday: Boolean,
    val isDisabled: Boolean,
    val marker: CalendarMarker?,
    /** 日期数字下方的补充文案，例如“初一”“春节”“休”。 */
    val supplementaryLabel: String? = null,
    /** 当前自然日内（含跨天事件）的事件摘要。 */
    val events: List<CalendarEventSummary> = emptyList()
)

/** 日历头部插槽的只读状态。 */
data class CalendarHeaderState(
    val displayedMonth: CalendarMonth,
    val canGoPrevious: Boolean,
    val canGoNext: Boolean,
    val isMonthPickerVisible: Boolean,
    val isTodayActionVisible: Boolean
)

/** 星期标题插槽的只读状态。columnIndex 按当前 firstDayOfWeek 排列。 */
data class CalendarWeekdayState(
    val weekday: CalendarWeekday,
    val label: String,
    val columnIndex: Int,
    val isWeekend: Boolean
)

/** 底部插槽的只读状态。 */
data class CalendarFooterState(
    val displayedMonth: CalendarMonth,
    val selection: CalendarSelection,
    val today: CalendarDate
)

/**
 * Slot 可调用的受控动作集合。Slot 只能经由此对象改变组件状态，避免直接依赖内部渲染状态。
 */
class CalendarActions internal constructor(private val view: CalendarView) {
    fun showMonth(month: CalendarMonth) = view.showMonth(month)

    fun showToday() = view.showToday()

    fun select(date: CalendarDate?) = view.select(date)

    fun setSelection(selection: CalendarSelection, emitEvent: Boolean = false) = view.setSelection(selection, emitEvent)

    fun setSelectionMode(
        mode: CalendarSelectionMode,
        selection: CalendarSelection = CalendarSelection.None,
        emitEvent: Boolean = false
    ) = view.setSelectionMode(mode, selection, emitEvent)

    fun showMonthPicker() = view.showMonthPicker()

    fun hideMonthPicker() = view.hideMonthPicker()
}

/** 日期在当前选择中的视觉位置。 */
enum class CalendarSelectionPosition {
    NONE,
    SINGLE,
    RANGE_START,
    RANGE_MIDDLE,
    RANGE_END,
    MULTIPLE
}

/** 日历支持的选择模式；默认值 SINGLE 保持现有组件行为不变。 */
enum class CalendarSelectionMode {
    NONE,
    SINGLE,
    RANGE,
    MULTIPLE
}

/** 范围选择第二次点击早于起点时的处理策略。 */
enum class RangeOverflowPolicy {
    RESTART,
    SWAP
}

/** 选择未生效时返回给业务的原因。 */
enum class CalendarSelectionRejectedReason {
    DISABLED_DATE,
    RANGE_CONTAINS_DISABLED_DATE,
    MAX_RANGE_DAYS_EXCEEDED,
    MAX_SELECTION_COUNT_REACHED,
    SELECTION_DISABLED
}

/**
 * 可受控的选择状态。日期始终是无时区自然日，业务不需要自行维护范围起止状态机。
 */
sealed class CalendarSelection {
    object None : CalendarSelection()

    data class Single(val date: CalendarDate) : CalendarSelection()

    data class Range(val start: CalendarDate, val end: CalendarDate? = null) : CalendarSelection() {
        init {
            require(end == null || end >= start) { "范围结束日期不能早于开始日期" }
        }
    }

    data class Multiple(val dates: Set<CalendarDate>) : CalendarSelection()
}

/** CalendarAgenda 的只读渲染状态。范围和多选默认以第一个自然日作为日程筛选日期。 */
data class CalendarAgendaState(
    val date: CalendarDate?,
    val selection: CalendarSelection,
    val events: List<CalendarEventSummary>
)

/** 纯状态机的计算结果，供 View 层统一派发回调。 */
internal data class CalendarSelectionOutcome(
    val selection: CalendarSelection,
    val rejectionReason: CalendarSelectionRejectedReason? = null
)

/** 日历主题。所有颜色均为 ARGB Long，与 Kuikly Color 构造方式保持一致。 */
data class CalendarTheme(
    val backgroundColor: Long = 0xFFFFFFFF,
    val titleColor: Long = 0xFF1F2937,
    val weekdayColor: Long = 0xFF6B7280,
    val dayTextColor: Long = 0xFF1F2937,
    val adjacentMonthTextColor: Long = 0xFFB8C0CC,
    val disabledTextColor: Long = 0xFFD1D5DB,
    val todayTextColor: Long = 0xFF3D7EFF,
    val supplementaryTextColor: Long = 0xFF94A3B8,
    val selectedBackgroundColor: Long = 0xFF3D7EFF,
    val selectedTextColor: Long = 0xFFFFFFFF,
    val rangeBackgroundColor: Long = 0x333D7EFF,
    val markerColor: Long = 0xFF3D7EFF,
    val dividerColor: Long = 0xFFF1F5F9
)

/**
 * 选择规则的纯 Kotlin 实现。禁用日期和跨越禁用日期由 View 层在调用前校验，
 * 因为它们依赖业务传入的可选日期谓词。
 */
internal object CalendarSelectionMachine {
    fun select(
        current: CalendarSelection,
        mode: CalendarSelectionMode,
        date: CalendarDate,
        maxSelectionCount: Int,
        maxRangeDays: Int,
        rangeOverflowPolicy: RangeOverflowPolicy
    ): CalendarSelectionOutcome {
        return when (mode) {
            CalendarSelectionMode.NONE -> reject(current, CalendarSelectionRejectedReason.SELECTION_DISABLED)
            CalendarSelectionMode.SINGLE -> CalendarSelectionOutcome(CalendarSelection.Single(date))
            CalendarSelectionMode.RANGE -> selectRange(current, date, maxRangeDays, rangeOverflowPolicy)
            CalendarSelectionMode.MULTIPLE -> selectMultiple(current, date, maxSelectionCount)
        }
    }

    fun isSelected(selection: CalendarSelection, date: CalendarDate): Boolean = when (selection) {
        CalendarSelection.None -> false
        is CalendarSelection.Single -> selection.date == date
        is CalendarSelection.Range -> date == selection.start || date == selection.end
        is CalendarSelection.Multiple -> selection.dates.contains(date)
    }

    fun positionOf(selection: CalendarSelection, date: CalendarDate): CalendarSelectionPosition = when (selection) {
        CalendarSelection.None -> CalendarSelectionPosition.NONE
        is CalendarSelection.Single -> if (selection.date == date) CalendarSelectionPosition.SINGLE else CalendarSelectionPosition.NONE
        is CalendarSelection.Multiple -> if (selection.dates.contains(date)) CalendarSelectionPosition.MULTIPLE else CalendarSelectionPosition.NONE
        is CalendarSelection.Range -> when {
            selection.end == null && date == selection.start -> CalendarSelectionPosition.RANGE_START
            selection.end != null && selection.start == selection.end && date == selection.start -> CalendarSelectionPosition.SINGLE
            date == selection.start -> CalendarSelectionPosition.RANGE_START
            date == selection.end -> CalendarSelectionPosition.RANGE_END
            selection.end != null && date > selection.start && date < selection.end -> CalendarSelectionPosition.RANGE_MIDDLE
            else -> CalendarSelectionPosition.NONE
        }
    }

    fun isInRange(selection: CalendarSelection, date: CalendarDate): Boolean {
        return selection is CalendarSelection.Range && selection.end != null && date >= selection.start && date <= selection.end
    }

    private fun selectRange(
        current: CalendarSelection,
        date: CalendarDate,
        maxRangeDays: Int,
        rangeOverflowPolicy: RangeOverflowPolicy
    ): CalendarSelectionOutcome {
        val pendingRange = current as? CalendarSelection.Range
        if (pendingRange == null || pendingRange.end != null) {
            return CalendarSelectionOutcome(CalendarSelection.Range(date))
        }
        if (date >= pendingRange.start) {
            if (maxRangeDays > 0 && CalendarMath.daysBetween(pendingRange.start, date) + 1 > maxRangeDays) {
                return reject(current, CalendarSelectionRejectedReason.MAX_RANGE_DAYS_EXCEEDED)
            }
            return CalendarSelectionOutcome(CalendarSelection.Range(pendingRange.start, date))
        }
        return when (rangeOverflowPolicy) {
            RangeOverflowPolicy.RESTART -> CalendarSelectionOutcome(CalendarSelection.Range(date))
            RangeOverflowPolicy.SWAP -> {
                if (maxRangeDays > 0 && CalendarMath.daysBetween(date, pendingRange.start) + 1 > maxRangeDays) {
                    reject(current, CalendarSelectionRejectedReason.MAX_RANGE_DAYS_EXCEEDED)
                } else {
                    CalendarSelectionOutcome(CalendarSelection.Range(date, pendingRange.start))
                }
            }
        }
    }

    private fun selectMultiple(
        current: CalendarSelection,
        date: CalendarDate,
        maxSelectionCount: Int
    ): CalendarSelectionOutcome {
        val dates = (current as? CalendarSelection.Multiple)?.dates.orEmpty()
        if (dates.contains(date)) {
            val remaining = dates - date
            return CalendarSelectionOutcome(if (remaining.isEmpty()) CalendarSelection.None else CalendarSelection.Multiple(remaining))
        }
        if (maxSelectionCount > 0 && dates.size >= maxSelectionCount) {
            return reject(current, CalendarSelectionRejectedReason.MAX_SELECTION_COUNT_REACHED)
        }
        return CalendarSelectionOutcome(CalendarSelection.Multiple(dates + date))
    }

    private fun reject(
        current: CalendarSelection,
        reason: CalendarSelectionRejectedReason
    ): CalendarSelectionOutcome = CalendarSelectionOutcome(current, reason)
}

/** 仅为当前可见 42 格建立索引，避免每个日期格重复遍历全量事件。 */
internal object CalendarEventIndex {
    fun forVisibleRange(
        events: List<CalendarEventSummary>,
        range: CalendarVisibleRange
    ): Map<CalendarDate, List<CalendarEventSummary>> {
        if (events.isEmpty()) return emptyMap()
        val result = mutableMapOf<CalendarDate, MutableList<CalendarEventSummary>>()
        events.forEach { summary ->
            if (summary.endDate < range.start || summary.startDate > range.end) return@forEach
            var date = if (summary.startDate < range.start) range.start else summary.startDate
            val last = if (summary.endDate > range.end) range.end else summary.endDate
            while (date <= last) {
                result.getOrPut(date) { mutableListOf() }.add(summary)
                date = CalendarMath.addDays(date, 1)
            }
        }
        return result
    }
}

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

    fun daysBetween(start: CalendarDate, end: CalendarDate): Int {
        return (dayIndex(end) - dayIndex(start)).toInt()
    }

    fun visibleRange(month: CalendarMonth, firstDayOfWeek: CalendarWeekday): CalendarVisibleRange {
        val first = CalendarDate(month.year, month.month, 1)
        val firstOffset = (dayOfWeek(first.year, first.month, first.day) - firstDayOfWeek.index + 7) % 7
        val start = addDays(first, -firstOffset)
        return CalendarVisibleRange(month, start, addDays(start, 41))
    }

    private fun dayIndex(date: CalendarDate): Long {
        val completedYears = date.year - 1L
        val daysBeforeYear = completedYears * 365L + completedYears / 4L - completedYears / 100L + completedYears / 400L
        var daysBeforeMonth = 0L
        for (month in 1 until date.month) {
            daysBeforeMonth += daysInMonth(date.year, month)
        }
        return daysBeforeYear + daysBeforeMonth + date.day - 1L
    }
}
