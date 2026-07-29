package com.kuikly.kuiklycalendar.calendar

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.module.CalendarModule
import com.tencent.kuikly.core.module.ICalendar
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

typealias CalendarDayContent = ViewContainer<*, *>.(CalendarDayState) -> Unit

/**
 * 月历组合组件。
 *
 * 默认提供月份切换、42 格稳定网格、单日期选择、禁用日期与事件标记；通过 [CalendarDayContent]
 * 可按日期替换单元格内容，而不会绕开组件的选择和禁用逻辑。
 */
class CalendarView : ComposeView<CalendarAttr, CalendarEvent>() {
    private var displayedMonth: CalendarMonth by observable(CalendarMonth(1970, 1))
    private var selectedDate: CalendarDate? by observable(null)
    private var today: CalendarDate by observable(CalendarDate(1970, 1, 1))
    private var renderVersion: Int by observable(0)

    override fun createAttr(): CalendarAttr = CalendarAttr()

    override fun createEvent(): CalendarEvent = CalendarEvent()

    override fun created() {
        super.created()
        require(attr.minDate == null || attr.maxDate == null || attr.minDate!! <= attr.maxDate!!) {
            "minDate 不能晚于 maxDate"
        }
        today = currentDate()
        displayedMonth = attr.initialMonth ?: CalendarMonth(today.year, today.month)
        selectedDate = attr.selectedDate
    }

    /** 以编程方式切换月份；超出 minDate/maxDate 覆盖范围的月份会被忽略。 */
    fun showMonth(month: CalendarMonth) {
        if (month != displayedMonth && canDisplayMonth(month)) {
            displayedMonth = month
            renderVersion++
            event.dispatchMonthChanged(month)
        }
    }

    /** 回到今日所在月份，并保留当前选中态。 */
    fun showToday() = showMonth(CalendarMonth(today.year, today.month))

    /** 以编程方式更新选中日期；不会触发用户点击回调。 */
    fun select(date: CalendarDate?) {
        if (date == null || isSelectable(date)) {
            selectedDate = date
            if (date != null) displayedMonth = CalendarMonth(date.year, date.month)
            renderVersion++
        }
    }

    fun currentMonth(): CalendarMonth = displayedMonth

    fun currentSelection(): CalendarDate? = selectedDate

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            vbind({ ctx.renderVersion }) {
                View {
                    attr {
                        width(ctx.calendarWidth())
                        backgroundColor(Color(ctx.attr.theme.backgroundColor))
                        borderRadius(ctx.attr.cornerRadius)
                        padding(ctx.attr.horizontalPadding)
                    }
                    ctx.renderHeader(this)
                    ctx.renderWeekdays(this)
                    ctx.renderGrid(this)
                }
            }
        }
    }

    private fun renderHeader(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr {
                height(ctx.attr.headerHeight)
                flexDirectionRow()
                alignItemsCenter()
                justifyContentSpaceBetween()
            }
            ctx.renderHeaderAction(this, "‹", ctx.canDisplayMonth(ctx.displayedMonth.previous())) {
                ctx.showMonth(ctx.displayedMonth.previous())
            }
            Text {
                attr {
                    text(ctx.displayedMonth.title())
                    fontSize(ctx.attr.titleFontSize)
                    fontWeightBold()
                    color(Color(ctx.attr.theme.titleColor))
                }
            }
            ctx.renderHeaderAction(this, "›", ctx.canDisplayMonth(ctx.displayedMonth.next())) {
                ctx.showMonth(ctx.displayedMonth.next())
            }
        }
    }

    private fun renderHeaderAction(container: ViewContainer<*, *>, text: String, enabled: Boolean, action: () -> Unit) {
        container.View {
            attr {
                size(this@CalendarView.attr.navigationButtonSize, this@CalendarView.attr.navigationButtonSize)
                allCenter()
                borderRadius(this@CalendarView.attr.navigationButtonSize / 2f)
            }
            Text {
                attr {
                    text(text)
                    fontSize(28f)
                    color(Color(if (enabled) this@CalendarView.attr.theme.titleColor else this@CalendarView.attr.theme.disabledTextColor))
                }
            }
            if (enabled) event { click { action() } }
        }
    }

    private fun renderWeekdays(container: ViewContainer<*, *>) {
        val ctx = this
        val labels = weekdayLabels()
        container.View {
            attr { height(ctx.attr.weekdayHeight); flexDirectionRow() }
            labels.forEachIndexed { index, label ->
                View {
                    attr { width(ctx.cellWidth()); height(ctx.attr.weekdayHeight); allCenter() }
                    Text {
                        attr {
                            text(label)
                            fontSize(ctx.attr.weekdayFontSize)
                            color(Color(if (index == 0 || index == 6) ctx.attr.weekendColor else ctx.attr.theme.weekdayColor))
                        }
                    }
                }
            }
        }
    }

    private fun renderGrid(container: ViewContainer<*, *>) {
        val first = CalendarDate(displayedMonth.year, displayedMonth.month, 1)
        val firstOffset = (CalendarMath.dayOfWeek(first.year, first.month, first.day) - attr.firstDayOfWeek.index + 7) % 7
        val gridStart = CalendarMath.addDays(first, -firstOffset)
        val ctx = this
        container.View {
            attr { flexDirectionRow(); flexWrapWrap() }
            repeat(GRID_SIZE) { index ->
                val date = CalendarMath.addDays(gridStart, index)
                val isCurrentMonth = date.year == ctx.displayedMonth.year && date.month == ctx.displayedMonth.month
                ctx.renderDayCell(this, date, isCurrentMonth)
            }
        }
    }

    private fun renderDayCell(container: ViewContainer<*, *>, date: CalendarDate, isCurrentMonth: Boolean) {
        val hidden = !isCurrentMonth && !attr.showAdjacentMonths
        val disabled = !isSelectable(date)
        val state = CalendarDayState(
            date = date,
            isCurrentMonth = isCurrentMonth,
            isSelected = date == selectedDate,
            isToday = date == today,
            isDisabled = disabled,
            marker = attr.markers[date]
        )
        container.View {
            attr {
                width(this@CalendarView.cellWidth())
                height(this@CalendarView.attr.dayHeight)
                allCenter()
            }
            if (!hidden) {
                if (this@CalendarView.attr.dayContent != null) {
                    this@CalendarView.attr.dayContent?.invoke(this, state)
                } else {
                    this@CalendarView.renderDefaultDay(this, state)
                }
                if (!disabled) {
                    event {
                        click { this@CalendarView.onDayTapped(state) }
                    }
                }
            }
        }
    }

    private fun renderDefaultDay(container: ViewContainer<*, *>, state: CalendarDayState) {
        container.View {
            attr {
                size(this@CalendarView.attr.dayIndicatorSize, this@CalendarView.attr.dayIndicatorSize)
                borderRadius(this@CalendarView.attr.dayIndicatorSize / 2f)
                backgroundColor(Color(if (state.isSelected) this@CalendarView.attr.theme.selectedBackgroundColor else 0x00000000))
                allCenter()
            }
            Text {
                attr {
                    text(state.date.day.toString())
                    fontSize(this@CalendarView.attr.dayFontSize)
                    color(Color(this@CalendarView.dayTextColor(state)))
                }
            }
            if (state.marker != null && !state.isSelected) {
                View {
                    attr {
                        width(this@CalendarView.attr.dayIndicatorSize)
                        height(5f)
                        absolutePosition(bottom = 0f, left = 0f)
                        flexDirectionRow()
                        allCenter()
                    }
                    state.marker.colors.take(MAX_MARKERS).forEach { markerColor ->
                        View {
                            attr {
                                size(4f, 4f)
                                borderRadius(2f)
                                marginLeft(1f)
                                marginRight(1f)
                                backgroundColor(Color(markerColor))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onDayTapped(state: CalendarDayState) {
        selectedDate = state.date
        if (!state.isCurrentMonth) displayedMonth = CalendarMonth(state.date.year, state.date.month)
        renderVersion++
        event.dispatchDateSelected(state.date)
        if (!state.isCurrentMonth) event.dispatchMonthChanged(displayedMonth)
    }

    private fun dayTextColor(state: CalendarDayState): Long = when {
        state.isSelected -> attr.theme.selectedTextColor
        state.isDisabled -> attr.theme.disabledTextColor
        state.isToday -> attr.theme.todayTextColor
        !state.isCurrentMonth -> attr.theme.adjacentMonthTextColor
        else -> attr.theme.dayTextColor
    }

    private fun isSelectable(date: CalendarDate): Boolean {
        if (attr.minDate != null && date < attr.minDate!!) return false
        if (attr.maxDate != null && date > attr.maxDate!!) return false
        if (attr.disabledDates.contains(date)) return false
        return attr.isDateSelectable?.invoke(date) ?: true
    }

    private fun canDisplayMonth(month: CalendarMonth): Boolean {
        val first = CalendarDate(month.year, month.month, 1)
        val last = CalendarDate(month.year, month.month, CalendarMath.daysInMonth(month.year, month.month))
        return (attr.minDate == null || last >= attr.minDate!!) && (attr.maxDate == null || first <= attr.maxDate!!)
    }

    private fun weekdayLabels(): List<String> {
        val source = attr.weekdayLabels
        return (0 until DAYS_PER_WEEK).map {
            val weekdayIndex = (attr.firstDayOfWeek.index + it) % DAYS_PER_WEEK
            source.getOrNull(weekdayIndex) ?: DEFAULT_WEEKDAY_LABELS[weekdayIndex]
        }
    }

    private fun cellWidth(): Float = (calendarWidth() - attr.horizontalPadding * 2f) / DAYS_PER_WEEK

    private fun calendarWidth(): Float = if (attr.width > 0f) attr.width else pagerData.pageViewWidth

    private fun currentDate(): CalendarDate {
        val calendar = getPager().acquireModule<CalendarModule>(CalendarModule.MODULE_NAME).newCalendarInstance()
        return CalendarDate(
            calendar.get(ICalendar.Field.YEAR),
            calendar.get(ICalendar.Field.MONTH) + 1,
            calendar.get(ICalendar.Field.DAY_OF_MONTH)
        )
    }

    private companion object {
        const val DAYS_PER_WEEK = 7
        const val GRID_SIZE = 42
        const val MAX_MARKERS = 3
        val DEFAULT_WEEKDAY_LABELS = listOf("日", "一", "二", "三", "四", "五", "六")
    }
}

class CalendarAttr : ComposeAttr() {
    var width: Float = 0f
    var initialMonth: CalendarMonth? = null
    var selectedDate: CalendarDate? = null
    var minDate: CalendarDate? = null
    var maxDate: CalendarDate? = null
    var disabledDates: Set<CalendarDate> = emptySet()
    var isDateSelectable: ((CalendarDate) -> Boolean)? = null
    var markers: Map<CalendarDate, CalendarMarker> = emptyMap()
    var showAdjacentMonths: Boolean = false
    var firstDayOfWeek: CalendarWeekday = CalendarWeekday.MONDAY
    var weekdayLabels: List<String> = listOf("日", "一", "二", "三", "四", "五", "六")
    var theme: CalendarTheme = CalendarTheme()
    var dayContent: CalendarDayContent? = null
    var cornerRadius: Float = 16f
    var horizontalPadding: Float = 12f
    var headerHeight: Float = 52f
    var weekdayHeight: Float = 32f
    var dayHeight: Float = 48f
    var navigationButtonSize: Float = 36f
    var dayIndicatorSize: Float = 36f
    var titleFontSize: Float = 17f
    var weekdayFontSize: Float = 12f
    var dayFontSize: Float = 15f
    var weekendColor: Long = 0xFFF05A5A
}

class CalendarEvent : ComposeEvent() {
    private var dateSelectedListener: ((CalendarDate) -> Unit)? = null
    private var monthChangedListener: ((CalendarMonth) -> Unit)? = null

    fun dateSelected(listener: (CalendarDate) -> Unit) {
        dateSelectedListener = listener
    }

    fun monthChanged(listener: (CalendarMonth) -> Unit) {
        monthChangedListener = listener
    }

    internal fun dispatchDateSelected(date: CalendarDate) = dateSelectedListener?.invoke(date)

    internal fun dispatchMonthChanged(month: CalendarMonth) = monthChangedListener?.invoke(month)
}

fun ViewContainer<*, *>.Calendar(init: CalendarView.() -> Unit) {
    addChild(CalendarView(), init)
}
