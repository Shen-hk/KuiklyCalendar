package com.kuikly.kuiklycalendar.calendar

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.PanGestureParams
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.module.CalendarModule
import com.tencent.kuikly.core.module.ICalendar
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

typealias CalendarDayContent = ViewContainer<*, *>.(CalendarDayState) -> Unit
typealias CalendarHeaderSlot = ViewContainer<*, *>.(CalendarHeaderState, CalendarActions) -> Unit
typealias CalendarHeaderTitleSlot = ViewContainer<*, *>.(CalendarHeaderState, CalendarActions) -> Unit
typealias CalendarHeaderActionSlot = ViewContainer<*, *>.(CalendarHeaderState, CalendarActions) -> Unit
typealias CalendarWeekdaySlot = ViewContainer<*, *>.(CalendarWeekdayState) -> Unit
typealias CalendarMarkerSlot = ViewContainer<*, *>.(CalendarDayState) -> Unit
typealias CalendarFooterSlot = ViewContainer<*, *>.(CalendarFooterState, CalendarActions) -> Unit
typealias CalendarSimpleSlot = ViewContainer<*, *>.() -> Unit

/** 可组合的日历渲染插槽。完整 header 的优先级高于 header 子插槽。 */
class CalendarSlots {
    var header: CalendarHeaderSlot? = null
    var headerTitle: CalendarHeaderTitleSlot? = null
    var headerLeading: CalendarHeaderActionSlot? = null
    var headerTrailing: CalendarHeaderActionSlot? = null
    var weekday: CalendarWeekdaySlot? = null
    var dayContent: CalendarDayContent? = null
    var marker: CalendarMarkerSlot? = null
    var footer: CalendarFooterSlot? = null
    var emptyContent: CalendarSimpleSlot? = null
}

/**
 * 月历组合组件。
 *
 * 默认提供月份切换、42 格稳定网格、单日期选择、禁用日期与事件标记；通过 [CalendarDayContent]
 * 可按日期替换单元格内容，而不会绕开组件的选择和禁用逻辑。
 */
class CalendarView : ComposeView<CalendarAttr, CalendarEvent>() {
    private var displayedMonth: CalendarMonth by observable(CalendarMonth(1970, 1))
    private var selection: CalendarSelection by observable(CalendarSelection.None)
    private var activeSelectionMode: CalendarSelectionMode by observable(CalendarSelectionMode.SINGLE)
    private var today: CalendarDate by observable(CalendarDate(1970, 1, 1))
    private var renderVersion: Int by observable(0)
    private var isMonthPickerVisible: Boolean by observable(false)
    private var isPickingYear: Boolean by observable(true)
    private var monthPickerYear: Int by observable(1970)
    private var eventsByDate: Map<CalendarDate, List<CalendarEventSummary>> = emptyMap()
    private var lastDispatchedVisibleRange: CalendarVisibleRange? = null
    private var monthSwipeStartX: Float? = null
    private val actions by lazy { CalendarActions(this) }

    override fun createAttr(): CalendarAttr = CalendarAttr()

    override fun createEvent(): CalendarEvent = CalendarEvent()

    override fun created() {
        super.created()
        require(attr.minDate == null || attr.maxDate == null || attr.minDate!! <= attr.maxDate!!) {
            "minDate 不能晚于 maxDate"
        }
        today = currentDate()
        displayedMonth = attr.initialMonth ?: CalendarMonth(today.year, today.month)
        activeSelectionMode = attr.selectionMode
        require(!attr.yearRange.isEmpty()) { "yearRange 不能为空" }
        require(attr.yearPickerStep > 0) { "yearPickerStep 必须大于 0" }
        selection = attr.selection ?: attr.selectedDate?.let { CalendarSelection.Single(it) } ?: CalendarSelection.None
        require(isSelectionCompatibleWithMode(selection)) { "selection 与 selectionMode 不匹配" }
        refreshVisibleRange(notify = true)
    }

    /** 以编程方式切换月份；超出 minDate/maxDate 覆盖范围的月份会被忽略。 */
    fun showMonth(month: CalendarMonth) {
        if (month != displayedMonth && canDisplayMonth(month)) {
            displayedMonth = month
            refreshVisibleRange(notify = false)
            renderVersion++
            event.dispatchMonthChanged(month)
            dispatchVisibleRangeIfChanged()
        }
    }

    /** 回到今日所在月份，并保留当前选中态。 */
    fun showToday() = showMonth(CalendarMonth(today.year, today.month))

    /** 打开内置年月面板；未启用 monthPickerEnabled 时不产生状态变化。 */
    fun showMonthPicker() {
        if (!attr.monthPickerEnabled || isMonthPickerVisible) return
        monthPickerYear = displayedMonth.year.coerceIn(attr.yearRange.first, attr.yearRange.last)
        isPickingYear = true
        isMonthPickerVisible = true
        renderVersion++
        event.dispatchMonthPickerVisibilityChanged(true)
    }

    /** 关闭内置年月面板。 */
    fun hideMonthPicker() {
        if (!isMonthPickerVisible) return
        isMonthPickerVisible = false
        renderVersion++
        event.dispatchMonthPickerVisibilityChanged(false)
    }

    /** 以编程方式更新选中日期；不会触发用户点击回调。 */
    fun select(date: CalendarDate?) {
        if (date == null || isSelectable(date)) {
            val previousMonth = displayedMonth
            selection = date?.let { CalendarSelection.Single(it) } ?: CalendarSelection.None
            if (date != null) displayedMonth = CalendarMonth(date.year, date.month)
            if (displayedMonth != previousMonth) refreshVisibleRange(notify = true)
            renderVersion++
        }
    }

    fun currentMonth(): CalendarMonth = displayedMonth

    /** 兼容 v1.0 的单日期读取 API；范围/多选请使用 [currentCalendarSelection]。 */
    fun currentSelection(): CalendarDate? {
        val current = selection
        return when (current) {
        CalendarSelection.None -> null
        is CalendarSelection.Single -> current.date
        is CalendarSelection.Range -> current.start
        is CalendarSelection.Multiple -> current.dates.firstOrNull()
        }
    }

    fun currentCalendarSelection(): CalendarSelection = selection

    fun currentSelectionMode(): CalendarSelectionMode = activeSelectionMode

    /** 动态切换选择模式；适合在同一日历中提供“普通选日/范围选日”的体验开关。 */
    fun setSelectionMode(
        mode: CalendarSelectionMode,
        value: CalendarSelection = CalendarSelection.None,
        emitEvent: Boolean = false
    ) {
        require(isSelectionCompatibleWithMode(value, mode)) { "selection 与新的 selectionMode 不匹配" }
        val previousMonth = displayedMonth
        activeSelectionMode = mode
        selection = value
        primaryDate(value)?.also { displayedMonth = CalendarMonth(it.year, it.month) }
        if (displayedMonth != previousMonth) refreshVisibleRange(notify = true)
        renderVersion++
        if (emitEvent) event.dispatchSelectionChanged(value)
    }

    /** 以编程方式同步任意选择状态；默认不触发业务回调。 */
    fun setSelection(value: CalendarSelection, emitEvent: Boolean = false) {
        require(isSelectionCompatibleWithMode(value)) { "selection 与 selectionMode 不匹配" }
        val previousMonth = displayedMonth
        selection = value
        primaryDate(value)?.also { displayedMonth = CalendarMonth(it.year, it.month) }
        if (displayedMonth != previousMonth) refreshVisibleRange(notify = true)
        renderVersion++
        if (emitEvent) event.dispatchSelectionChanged(value)
    }

    fun clearSelection(emitEvent: Boolean = false) = setSelection(CalendarSelection.None, emitEvent)

    /** 动态显示或隐藏所有日期格的补充标签（如农历）。 */
    fun setSupplementaryLabelVisible(visible: Boolean) {
        if (attr.showSupplementaryLabel == visible) return
        attr.showSupplementaryLabel = visible
        renderVersion++
    }

    /** 受控模式下更新事件摘要。调用后只重建当前 42 格可见范围的日期索引。 */
    fun setEvents(value: List<CalendarEventSummary>) {
        attr.events = value.toList()
        refreshVisibleRange(notify = false)
        renderVersion++
    }

    /** 当业务直接更新 [CalendarAttr.events] 的集合引用后，可调用此方法刷新显示。 */
    fun refreshEvents() {
        refreshVisibleRange(notify = false)
        renderVersion++
    }

    fun currentVisibleRange(): CalendarVisibleRange = visibleRange()

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
                    if (ctx.isMonthPickerVisible) {
                        ctx.renderMonthPicker(this)
                    } else {
                        ctx.renderWeekdays(this)
                        if (ctx.hasSelectableDateInDisplayedMonth() || ctx.attr.slots.emptyContent == null) {
                            ctx.renderGrid(this)
                        } else {
                            ctx.attr.slots.emptyContent?.invoke(this)
                        }
                        ctx.renderFooter(this)
                    }
                }
            }
        }
    }

    /** 左滑显示下个月、右滑显示上个月；纵向滑动仍交由外层滚动容器处理。 */
    private fun handleMonthSwipe(params: PanGestureParams) {
        if (isMonthPickerVisible) return
        when (params.state) {
            "start" -> monthSwipeStartX = params.x
            "end" -> {
                val distance = params.x - (monthSwipeStartX ?: params.x)
                monthSwipeStartX = null
                when {
                    distance >= MONTH_SWIPE_THRESHOLD -> showMonth(displayedMonth.previous())
                    distance <= -MONTH_SWIPE_THRESHOLD -> showMonth(displayedMonth.next())
                }
            }
            "cancel" -> monthSwipeStartX = null
        }
    }

    private fun renderHeader(container: ViewContainer<*, *>) {
        val ctx = this
        val state = headerState()
        attr.slots.header?.let { slot ->
            slot.invoke(container, state, actions)
            return
        }
        container.View {
            attr {
                height(ctx.attr.headerHeight)
                flexDirectionRow()
                alignItemsCenter()
                justifyContentSpaceBetween()
            }
            if (ctx.attr.slots.headerLeading != null) {
                ctx.attr.slots.headerLeading?.invoke(this, state, ctx.actions)
            } else {
                ctx.renderHeaderAction(this, "‹", state.canGoPrevious) {
                    ctx.showMonth(ctx.displayedMonth.previous())
                }
            }
            if (ctx.attr.slots.headerTitle != null) {
                ctx.attr.slots.headerTitle?.invoke(this, state, ctx.actions)
            } else {
                Text {
                    attr {
                        text(ctx.displayedMonth.title())
                        fontSize(ctx.attr.titleFontSize)
                        fontWeightBold()
                        color(Color(ctx.attr.theme.titleColor))
                    }
                    if (ctx.attr.monthPickerEnabled) {
                        event { click { ctx.showMonthPicker() } }
                    }
                }
            }
            if (ctx.attr.slots.headerTrailing != null) {
                ctx.attr.slots.headerTrailing?.invoke(this, state, ctx.actions)
            } else {
                View {
                    attr { flexDirectionRow(); alignItemsCenter() }
                    if (state.isTodayActionVisible) ctx.renderTodayAction(this)
                    ctx.renderHeaderAction(this, "›", state.canGoNext) {
                        ctx.showMonth(ctx.displayedMonth.next())
                    }
                }
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
                val weekdayIndex = (ctx.attr.firstDayOfWeek.index + index) % DAYS_PER_WEEK
                val weekday = CalendarWeekday.values().first { it.index == weekdayIndex }
                View {
                    attr { width(ctx.cellWidth()); height(ctx.attr.weekdayHeight); allCenter() }
                    if (ctx.attr.swipeMonthEnabled) {
                        event { pan { params -> ctx.handleMonthSwipe(params) } }
                    }
                    val state = CalendarWeekdayState(
                        weekday = weekday,
                        label = label,
                        columnIndex = index,
                        isWeekend = weekday == CalendarWeekday.SUNDAY || weekday == CalendarWeekday.SATURDAY
                    )
                    if (ctx.attr.slots.weekday != null) {
                        ctx.attr.slots.weekday?.invoke(this, state)
                    } else {
                        Text {
                            attr {
                                text(label)
                                fontSize(ctx.attr.weekdayFontSize)
                                color(Color(if (state.isWeekend) ctx.attr.weekendColor else ctx.attr.theme.weekdayColor))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderGrid(container: ViewContainer<*, *>) {
        val gridStart = visibleRange().start
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
            isSelected = CalendarSelectionMachine.isSelected(selection, date),
            selectionPosition = CalendarSelectionMachine.positionOf(selection, date),
            isInRange = CalendarSelectionMachine.isInRange(selection, date),
            isToday = date == today,
            isDisabled = disabled,
            marker = attr.markers[date],
            supplementaryLabel = supplementaryLabelFor(date),
            events = eventsByDate[date].orEmpty()
        )
        container.View {
            attr {
                width(this@CalendarView.cellWidth())
                height(this@CalendarView.attr.dayHeight)
                allCenter()
            }
            if (this@CalendarView.attr.swipeMonthEnabled) {
                event { pan { params -> this@CalendarView.handleMonthSwipe(params) } }
            }
            if (!hidden) {
                val dayContent = this@CalendarView.attr.slots.dayContent ?: this@CalendarView.attr.dayContent
                if (dayContent != null) {
                    dayContent.invoke(this, state)
                } else {
                    this@CalendarView.renderDefaultDay(this, state)
                }
                event {
                    click { this@CalendarView.onDayTapped(state) }
                }
            }
        }
    }

    private fun renderDefaultDay(container: ViewContainer<*, *>, state: CalendarDayState) {
        val indicatorEvents = state.events.filter { it.showsCalendarIndicator }
        renderRangeBackground(container, state)
        if (state.isSelected) {
            container.View {
                attr {
                    size(this@CalendarView.attr.dayIndicatorSize, this@CalendarView.attr.dayIndicatorSize)
                    borderRadius(this@CalendarView.attr.dayIndicatorCornerRadius)
                    backgroundColor(Color(this@CalendarView.attr.theme.selectedBackgroundColor))
                    allCenter()
                    zIndex(1)
                }
                this@CalendarView.renderDayTexts(this, state)
            }
        } else {
            // 仅用于布局和居中，不设置背景、圆角或边框；未选中态保持纯数字。
            container.View {
                attr {
                    size(this@CalendarView.attr.dayIndicatorSize, this@CalendarView.attr.dayIndicatorSize)
                    allCenter()
                    zIndex(1)
                }
                this@CalendarView.renderDayTexts(this, state)
            }
        }
        this@CalendarView.renderSpecialDateBadge(container, state.events)
        if (this@CalendarView.attr.slots.marker != null && (state.marker != null || indicatorEvents.isNotEmpty())) {
            this@CalendarView.attr.slots.marker?.invoke(container, state)
        } else if (indicatorEvents.isNotEmpty()) {
            this@CalendarView.renderEventIndicator(container, indicatorEvents)
        } else if (!state.isSelected && state.marker != null) {
            this@CalendarView.renderLegacyMarker(container, state.marker)
        }
    }

    /** 节假日、调休和纪念日使用独立角标呈现，不占用或计入日程点。 */
    private fun renderSpecialDateBadge(container: ViewContainer<*, *>, events: List<CalendarEventSummary>) {
        val special = events.firstOrNull { !it.showsCalendarIndicator && it.type in SPECIAL_DATE_TYPES } ?: return
        val (label, color) = when (special.type) {
            "holiday" -> "休" to 0xFFDC2626
            "makeup_workday" -> "班" to 0xFF2563EB
            else -> "纪" to 0xFF64748B
        }
        container.View {
            attr {
                size(16f, 16f)
                positionAbsolute()
                absolutePosition(top = 2f, right = 2f)
                allCenter()
                borderRadius(8f)
                backgroundColor(Color(color))
                zIndex(3)
            }
            Text {
                attr {
                    text(label)
                    fontSize(9f)
                    color(Color(0xFFFFFFFF))
                }
            }
        }
    }

    private fun renderLegacyMarker(container: ViewContainer<*, *>, marker: CalendarMarker) {
        container.View {
            attr {
                width(this@CalendarView.cellWidth())
                height(5f)
                absolutePosition(bottom = this@CalendarView.eventIndicatorBottomOffset(), left = 0f)
                flexDirectionRow()
                allCenter()
                zIndex(2)
            }
            marker.colors.firstOrNull()?.also { markerColor ->
                View {
                    attr {
                        size(4f, 4f)
                        borderRadius(1.5f)
                        marginLeft(1.5f)
                        marginRight(1.5f)
                        backgroundColor(Color(markerColor))
                    }
                }
            }
        }
    }

    private fun renderEventIndicator(container: ViewContainer<*, *>, events: List<CalendarEventSummary>) {
        when (attr.eventDisplayMode) {
            CalendarEventDisplayMode.DOTS -> renderEventDots(container, events)
            CalendarEventDisplayMode.COUNT_BADGE -> renderEventCountBadge(container, events)
            CalendarEventDisplayMode.CUSTOM -> Unit
        }
    }

    private fun renderEventDots(container: ViewContainer<*, *>, events: List<CalendarEventSummary>) {
        container.View {
            attr {
                width(this@CalendarView.cellWidth())
                height(8f)
                absolutePosition(bottom = this@CalendarView.eventIndicatorBottomOffset(), left = 0f)
                flexDirectionRow()
                allCenter()
                zIndex(2)
            }
            View {
                attr {
                    size(4f, 4f)
                    borderRadius(2f)
                    backgroundColor(Color(events.first().color))
                }
            }
        }
    }

    private fun renderDayTexts(container: ViewContainer<*, *>, state: CalendarDayState) {
        val hasBottomIndicator = state.events.any { it.showsCalendarIndicator } || (!state.isSelected && state.marker != null)
        val textAreaHeight = if (hasBottomIndicator) {
            (attr.dayIndicatorSize - EVENT_INDICATOR_RESERVED_HEIGHT).coerceAtLeast(0f)
        } else {
            attr.dayIndicatorSize
        }
        container.View {
            attr {
                width(this@CalendarView.attr.dayIndicatorSize)
                height(textAreaHeight)
                absolutePosition(top = 0f, left = 0f)
                allCenter()
            }
            Text {
                attr {
                    text(state.date.day.toString())
                    fontSize(
                        if (state.supplementaryLabel == null) {
                            this@CalendarView.attr.dayFontSize
                        } else {
                            this@CalendarView.attr.dayWithSupplementaryFontSize
                        }
                    )
                    color(Color(this@CalendarView.dayTextColor(state)))
                }
            }
            state.supplementaryLabel?.also { label ->
                Text {
                    attr {
                        text(label)
                        fontSize(this@CalendarView.attr.supplementaryFontSize)
                        color(Color(this@CalendarView.supplementaryTextColor(state)))
                    }
                }
            }
        }
    }

    private fun renderEventCountBadge(container: ViewContainer<*, *>, events: List<CalendarEventSummary>) {
        container.View {
            attr {
                size(this@CalendarView.attr.eventBadgeSize, this@CalendarView.attr.eventBadgeSize)
                borderRadius(this@CalendarView.attr.eventBadgeSize / 2f)
                absolutePosition(
                    bottom = this@CalendarView.eventIndicatorBottomOffset(),
                    left = (this@CalendarView.cellWidth() - this@CalendarView.attr.eventBadgeSize) / 2f
                )
                backgroundColor(Color(events.first().color))
                allCenter()
                zIndex(2)
            }
            Text {
                attr {
                    text(if (events.size > 9) "9+" else events.size.toString())
                    fontSize(if (events.size > 9) 7f else 8f)
                    color(Color(0xFFFFFFFF))
                }
            }
        }
    }

    private fun renderTodayAction(container: ViewContainer<*, *>) {
        container.Text {
            attr {
                text(this@CalendarView.attr.todayActionText)
                fontSize(12f)
                color(Color(this@CalendarView.attr.theme.todayTextColor))
                marginRight(8f)
            }
            event {
                click {
                    this@CalendarView.showToday()
                    this@CalendarView.event.dispatchTodayActionClick()
                }
            }
        }
    }

    /** 日程点位于农历下方；选中时始终收在圆角方形选中态的底部留白内。 */
    private fun eventIndicatorBottomOffset(): Float {
        return ((attr.dayHeight - attr.dayIndicatorSize).coerceAtLeast(0f) / 2f) + 1f
    }

    private fun renderFooter(container: ViewContainer<*, *>) {
        attr.slots.footer?.invoke(
            container,
            CalendarFooterState(displayedMonth, selection, today),
            actions
        )
    }

    private fun onDayTapped(state: CalendarDayState) {
        if (state.isDisabled) {
            event.dispatchSelectionRejected(CalendarSelectionRejectedReason.DISABLED_DATE, state.date)
            return
        }
        val outcome = CalendarSelectionMachine.select(
            current = selection,
            mode = activeSelectionMode,
            date = state.date,
            maxSelectionCount = attr.maxSelectionCount,
            maxRangeDays = attr.maxRangeDays,
            rangeOverflowPolicy = attr.rangeOverflowPolicy
        )
        outcome.rejectionReason?.also {
            event.dispatchSelectionRejected(it, state.date)
            return
        }
        if (!attr.allowRangeAcrossDisabledDates && !isRangeSelectable(outcome.selection)) {
            event.dispatchSelectionRejected(CalendarSelectionRejectedReason.RANGE_CONTAINS_DISABLED_DATE, state.date)
            return
        }
        selection = outcome.selection
        if (!state.isCurrentMonth) {
            displayedMonth = CalendarMonth(state.date.year, state.date.month)
            refreshVisibleRange(notify = false)
        }
        renderVersion++
        event.dispatchSelectionChanged(selection)
        if (activeSelectionMode == CalendarSelectionMode.SINGLE) event.dispatchDateSelected(state.date)
        if (!state.isCurrentMonth) {
            event.dispatchMonthChanged(displayedMonth)
            dispatchVisibleRangeIfChanged()
        }
    }

    private fun visibleRange(): CalendarVisibleRange = CalendarMath.visibleRange(displayedMonth, attr.firstDayOfWeek)

    private fun refreshVisibleRange(notify: Boolean) {
        val range = visibleRange()
        eventsByDate = CalendarEventIndex.forVisibleRange(attr.events, range)
        if (notify) dispatchVisibleRangeIfChanged()
    }

    private fun dispatchVisibleRangeIfChanged() {
        val range = visibleRange()
        if (range != lastDispatchedVisibleRange) {
            lastDispatchedVisibleRange = range
            event.dispatchVisibleRangeChanged(range)
        }
    }

    private fun renderMonthPicker(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr { paddingTop(8f); paddingBottom(12f) }
            View {
                attr { height(40f); flexDirectionRow(); alignItemsCenter(); justifyContentSpaceBetween() }
                ctx.renderHeaderAction(this, "‹", ctx.canNavigatePickerBackward()) {
                    ctx.monthPickerYear = if (ctx.isPickingYear) {
                        ctx.monthPickerPageStart() - ctx.attr.yearPickerStep
                    } else {
                        ctx.monthPickerYear - 1
                    }
                    ctx.renderVersion++
                }
                Text {
                    attr {
                        text(if (ctx.isPickingYear) "选择年份" else "${ctx.monthPickerYear}年 · 选择月份")
                        fontSize(15f)
                        fontWeightBold()
                        color(Color(ctx.attr.theme.titleColor))
                    }
                    if (!ctx.isPickingYear) event { click { ctx.isPickingYear = true; ctx.renderVersion++ } }
                }
                ctx.renderHeaderAction(this, "›", ctx.canNavigatePickerForward()) {
                    ctx.monthPickerYear = if (ctx.isPickingYear) {
                        ctx.monthPickerPageStart() + ctx.attr.yearPickerStep
                    } else {
                        ctx.monthPickerYear + 1
                    }
                    ctx.renderVersion++
                }
            }
            if (ctx.isPickingYear) ctx.renderYearPickerGrid(this) else ctx.renderMonthPickerGrid(this)
            Text {
                attr {
                    text("取消")
                    fontSize(13f)
                    color(Color(ctx.attr.theme.weekdayColor))
                    marginTop(12f)
                    alignSelfCenter()
                }
                event { click { ctx.hideMonthPicker() } }
            }
        }
    }

    private fun renderYearPickerGrid(container: ViewContainer<*, *>) {
        val ctx = this
        val start = monthPickerPageStart()
        container.View {
            attr { flexDirectionRow(); flexWrapWrap(); marginTop(8f) }
            (start..minOf(start + ctx.attr.yearPickerStep - 1, ctx.attr.yearRange.last)).forEach { year ->
                ctx.renderPickerItem(this, year.toString(), year == ctx.displayedMonth.year, true) {
                    ctx.monthPickerYear = year
                    ctx.isPickingYear = false
                    ctx.renderVersion++
                }
            }
        }
    }

    private fun renderMonthPickerGrid(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr { flexDirectionRow(); flexWrapWrap(); marginTop(8f) }
            (1..12).forEach { month ->
                val candidate = CalendarMonth(ctx.monthPickerYear, month)
                ctx.renderPickerItem(
                    this,
                    "${month}月",
                    candidate == ctx.displayedMonth,
                    ctx.canDisplayMonth(candidate)
                ) {
                    ctx.showMonth(candidate)
                    ctx.hideMonthPicker()
                }
            }
        }
    }

    private fun renderPickerItem(
        container: ViewContainer<*, *>,
        text: String,
        selected: Boolean,
        enabled: Boolean,
        action: () -> Unit
    ) {
        container.View {
            attr {
                width((this@CalendarView.calendarWidth() - this@CalendarView.attr.horizontalPadding * 2f) / PICKER_COLUMN_COUNT)
                height(42f)
                allCenter()
                borderRadius(8f)
                backgroundColor(Color(if (selected) this@CalendarView.attr.theme.rangeBackgroundColor else 0x00000000))
            }
            Text {
                attr {
                    text(text)
                    fontSize(14f)
                    color(Color(if (enabled) this@CalendarView.attr.theme.dayTextColor else this@CalendarView.attr.theme.disabledTextColor))
                }
            }
            if (enabled) event { click { action() } }
        }
    }

    private fun headerState(): CalendarHeaderState = CalendarHeaderState(
        displayedMonth = displayedMonth,
        canGoPrevious = canDisplayMonth(displayedMonth.previous()),
        canGoNext = canDisplayMonth(displayedMonth.next()),
        isMonthPickerVisible = isMonthPickerVisible,
        isTodayActionVisible = attr.showTodayAction && displayedMonth != CalendarMonth(today.year, today.month)
    )

    private fun monthPickerPageStart(): Int {
        val relative = (monthPickerYear - attr.yearRange.first).coerceAtLeast(0)
        return attr.yearRange.first + relative / attr.yearPickerStep * attr.yearPickerStep
    }

    private fun canShowPreviousPickerPage(): Boolean = monthPickerPageStart() > attr.yearRange.first

    private fun canShowNextPickerPage(): Boolean = monthPickerPageStart() + attr.yearPickerStep <= attr.yearRange.last

    private fun canNavigatePickerBackward(): Boolean = if (isPickingYear) {
        canShowPreviousPickerPage()
    } else {
        monthPickerYear > attr.yearRange.first
    }

    private fun canNavigatePickerForward(): Boolean = if (isPickingYear) {
        canShowNextPickerPage()
    } else {
        monthPickerYear < attr.yearRange.last
    }

    private fun hasSelectableDateInDisplayedMonth(): Boolean {
        for (day in 1..CalendarMath.daysInMonth(displayedMonth.year, displayedMonth.month)) {
            if (isSelectable(CalendarDate(displayedMonth.year, displayedMonth.month, day))) return true
        }
        return false
    }

    private fun dayTextColor(state: CalendarDayState): Long = when {
        state.isSelected -> attr.theme.selectedTextColor
        state.isDisabled -> attr.theme.disabledTextColor
        state.isToday -> attr.theme.todayTextColor
        !state.isCurrentMonth -> attr.theme.adjacentMonthTextColor
        else -> attr.theme.dayTextColor
    }

    private fun supplementaryTextColor(state: CalendarDayState): Long = when {
        state.isSelected -> attr.theme.selectedTextColor
        state.isDisabled -> attr.theme.disabledTextColor
        !state.isCurrentMonth -> attr.theme.adjacentMonthTextColor
        else -> attr.theme.supplementaryTextColor
    }

    private fun isSelectable(date: CalendarDate): Boolean {
        if (attr.minDate != null && date < attr.minDate!!) return false
        if (attr.maxDate != null && date > attr.maxDate!!) return false
        if (attr.disabledDates.contains(date)) return false
        return attr.isDateSelectable?.invoke(date) ?: true
    }

    private fun isRangeSelectable(candidate: CalendarSelection): Boolean {
        val range = candidate as? CalendarSelection.Range ?: return true
        val end = range.end ?: return true
        if (range.start == end || (attr.disabledDates.isEmpty() && attr.isDateSelectable == null)) return true
        var current = range.start
        while (current <= end) {
            if (!isSelectable(current)) return false
            current = CalendarMath.addDays(current, 1)
        }
        return true
    }

    private fun isSelectionCompatibleWithMode(
        value: CalendarSelection,
        mode: CalendarSelectionMode = activeSelectionMode
    ): Boolean = when (mode) {
        CalendarSelectionMode.NONE -> value == CalendarSelection.None
        CalendarSelectionMode.SINGLE -> value == CalendarSelection.None || value is CalendarSelection.Single
        CalendarSelectionMode.RANGE -> value == CalendarSelection.None || value is CalendarSelection.Range
        CalendarSelectionMode.MULTIPLE -> value == CalendarSelection.None || value is CalendarSelection.Multiple
    }

    private fun primaryDate(value: CalendarSelection): CalendarDate? = when (value) {
        CalendarSelection.None -> null
        is CalendarSelection.Single -> value.date
        is CalendarSelection.Range -> value.start
        is CalendarSelection.Multiple -> value.dates.firstOrNull()
    }

    private fun supplementaryLabelFor(date: CalendarDate): String? {
        if (!attr.showSupplementaryLabel) return null
        return attr.supplementaryProvider?.supplementaryLabel(date)
    }

    private fun renderRangeBackground(container: ViewContainer<*, *>, state: CalendarDayState) {
        if (!state.isInRange || state.selectionPosition == CalendarSelectionPosition.SINGLE) return
        val halfCellWidth = cellWidth() / 2f
        val (left, width) = when (state.selectionPosition) {
            CalendarSelectionPosition.RANGE_START -> halfCellWidth to halfCellWidth
            CalendarSelectionPosition.RANGE_END -> 0f to halfCellWidth
            CalendarSelectionPosition.RANGE_MIDDLE -> 0f to cellWidth()
            else -> return
        }
        container.View {
            attr {
                positionAbsolute()
                absolutePosition(
                    top = (this@CalendarView.attr.dayHeight - this@CalendarView.attr.dayIndicatorSize) / 2f,
                    left = left
                )
                size(width, this@CalendarView.attr.dayIndicatorSize)
                backgroundColor(Color(this@CalendarView.attr.theme.rangeBackgroundColor))
            }
        }
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
        const val PICKER_COLUMN_COUNT = 3
        const val EVENT_INDICATOR_RESERVED_HEIGHT = 8f
        const val MONTH_SWIPE_THRESHOLD = 48f
        val SPECIAL_DATE_TYPES = setOf("holiday", "makeup_workday", "memorial")
        val DEFAULT_WEEKDAY_LABELS = listOf("日", "一", "二", "三", "四", "五", "六")
    }
}

class CalendarAttr : ComposeAttr() {
    var width: Float = 0f
    var initialMonth: CalendarMonth? = null
    var selectedDate: CalendarDate? = null
    /** v1.1 统一选择状态；非 null 时优先于 selectedDate。 */
    var selection: CalendarSelection? = null
    var selectionMode: CalendarSelectionMode = CalendarSelectionMode.SINGLE
    var maxSelectionCount: Int = 0
    var maxRangeDays: Int = 0
    var rangeOverflowPolicy: RangeOverflowPolicy = RangeOverflowPolicy.RESTART
    var allowRangeAcrossDisabledDates: Boolean = false
    var minDate: CalendarDate? = null
    var maxDate: CalendarDate? = null
    var disabledDates: Set<CalendarDate> = emptySet()
    var isDateSelectable: ((CalendarDate) -> Boolean)? = null
    var markers: Map<CalendarDate, CalendarMarker> = emptyMap()
    var supplementaryProvider: CalendarSupplementaryProvider? = null
    var showSupplementaryLabel: Boolean = false
    /** v1.3 受控事件摘要；远程加载由业务通过 visibleRangeChanged 后回填。 */
    var events: List<CalendarEventSummary> = emptyList()
    /** Configures schedule points with a declaration DSL instead of a raw list. */
    fun events(init: CalendarEventsDsl.() -> Unit) {
        events = calendarEvents(init)
    }
    var eventDisplayMode: CalendarEventDisplayMode = CalendarEventDisplayMode.DOTS
    var showAdjacentMonths: Boolean = false
    /** 是否允许在月历区域横向滑动切换月份。 */
    var swipeMonthEnabled: Boolean = true
    var firstDayOfWeek: CalendarWeekday = CalendarWeekday.MONDAY
    var weekdayLabels: List<String> = listOf("日", "一", "二", "三", "四", "五", "六")
    var theme: CalendarTheme = CalendarTheme()
    /** v1.2 插槽集合；新 dayContent 优先于兼容属性 [dayContent]。 */
    var slots: CalendarSlots = CalendarSlots()
    var monthPickerEnabled: Boolean = false
    /** 仅限制年月导航面板，不改变 minDate/maxDate 的可选日期语义。 */
    var yearRange: IntRange = 1970..2100
    var yearPickerStep: Int = 12
    var showTodayAction: Boolean = false
    var todayActionText: String = "回到今天"
    var dayContent: CalendarDayContent? = null
    var cornerRadius: Float = 16f
    var horizontalPadding: Float = 12f
    var headerHeight: Float = 52f
    var weekdayHeight: Float = 32f
    var dayHeight: Float = 52f
    var navigationButtonSize: Float = 36f
    var dayIndicatorSize: Float = 42f
    /** 默认采用宽松的圆角方形选中态，给日期、农历和日程点留出独立空间。 */
    var dayIndicatorCornerRadius: Float = 8f
    /** COUNT_BADGE 的底部居中小圆点尺寸；常规月视图推荐使用 DOTS。 */
    var eventBadgeSize: Float = 14f
    var titleFontSize: Float = 17f
    var weekdayFontSize: Float = 12f
    var dayFontSize: Float = 15f
    var dayWithSupplementaryFontSize: Float = 13f
    var supplementaryFontSize: Float = 8f
    var weekendColor: Long = 0xFFF05A5A
}

class CalendarEvent : ComposeEvent() {
    private var dateSelectedListener: ((CalendarDate) -> Unit)? = null
    private var monthChangedListener: ((CalendarMonth) -> Unit)? = null
    private var selectionChangedListener: ((CalendarSelection) -> Unit)? = null
    private var selectionRejectedListener: ((CalendarSelectionRejectedReason, CalendarDate) -> Unit)? = null
    private var monthPickerVisibilityChangedListener: ((Boolean) -> Unit)? = null
    private var todayActionClickListener: (() -> Unit)? = null
    private var visibleRangeChangedListener: ((CalendarVisibleRange) -> Unit)? = null

    fun dateSelected(listener: (CalendarDate) -> Unit) {
        dateSelectedListener = listener
    }

    fun monthChanged(listener: (CalendarMonth) -> Unit) {
        monthChangedListener = listener
    }

    fun selectionChanged(listener: (CalendarSelection) -> Unit) {
        selectionChangedListener = listener
    }

    fun selectionRejected(listener: (CalendarSelectionRejectedReason, CalendarDate) -> Unit) {
        selectionRejectedListener = listener
    }

    fun monthPickerVisibilityChanged(listener: (Boolean) -> Unit) {
        monthPickerVisibilityChangedListener = listener
    }

    fun todayActionClick(listener: () -> Unit) {
        todayActionClickListener = listener
    }

    /** 首次渲染和可见月份改变时触发；相同 42 格范围不会重复派发。 */
    fun visibleRangeChanged(listener: (CalendarVisibleRange) -> Unit) {
        visibleRangeChangedListener = listener
    }

    internal fun dispatchDateSelected(date: CalendarDate) = dateSelectedListener?.invoke(date)

    internal fun dispatchMonthChanged(month: CalendarMonth) = monthChangedListener?.invoke(month)

    internal fun dispatchSelectionChanged(selection: CalendarSelection) = selectionChangedListener?.invoke(selection)

    internal fun dispatchSelectionRejected(reason: CalendarSelectionRejectedReason, date: CalendarDate) {
        selectionRejectedListener?.invoke(reason, date)
    }

    internal fun dispatchMonthPickerVisibilityChanged(visible: Boolean) {
        monthPickerVisibilityChangedListener?.invoke(visible)
    }

    internal fun dispatchTodayActionClick() = todayActionClickListener?.invoke()

    internal fun dispatchVisibleRangeChanged(range: CalendarVisibleRange) {
        visibleRangeChangedListener?.invoke(range)
    }
}

fun ViewContainer<*, *>.Calendar(init: CalendarView.() -> Unit) {
    addChild(CalendarView(), init)
}
