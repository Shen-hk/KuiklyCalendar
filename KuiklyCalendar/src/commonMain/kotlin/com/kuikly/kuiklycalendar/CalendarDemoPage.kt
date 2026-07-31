package com.kuikly.kuiklycalendar

import com.kuikly.kuiklycalendar.base.BasePager
import com.kuikly.kuiklycalendar.calendar.Calendar
import com.kuikly.kuiklycalendar.calendar.CalendarAgenda
import com.kuikly.kuiklycalendar.calendar.CalendarAgendaView
import com.kuikly.kuiklycalendar.calendar.CalendarDate
import com.kuikly.kuiklycalendar.calendar.CalendarEventDisplayMode
import com.kuikly.kuiklycalendar.calendar.CalendarEventSummary
import com.kuikly.kuiklycalendar.calendar.CalendarEventsDsl
import com.kuikly.kuiklycalendar.calendar.CalendarMarker
import com.kuikly.kuiklycalendar.calendar.CalendarMonth
import com.kuikly.kuiklycalendar.calendar.CalendarSelection
import com.kuikly.kuiklycalendar.calendar.CalendarSelectionMode
import com.kuikly.kuiklycalendar.calendar.CalendarSlots
import com.kuikly.kuiklycalendar.calendar.CalendarTheme
import com.kuikly.kuiklycalendar.calendar.CalendarView
import com.kuikly.kuiklycalendar.calendar.ChinaMainlandHolidayCalendar
import com.kuikly.kuiklycalendar.calendar.ChinaLunarCalendar
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/** 日历组件展示页：所有能力围绕同一个可切换模式的日历呈现。 */
@Page("calendar_demo")
internal class CalendarDemoPage : BasePager() {
    private var isRangeSelectionEnabled by observable(false)
    private var isMultipleSelectionEnabled by observable(false)
    private var isLunarLabelEnabled by observable(true)
    private var isHolidayEnabled by observable(false)
    private var experienceDescription by observable("单日选择已开启，点击任意可用日期即可查看当天日程。")
    private var selectionDescription by observable("当前选择：2026-09-16")
    private var agenda: CalendarAgendaView? = null
    private var calendar: CalendarView? = null
    private val agendaEvents = CalendarEventsDsl().apply {
        event("kickoff", CalendarDate(2026, 9, 4)) { title = "项目启动会"; color = 0xFF2563EB }
        event("review", CalendarDate(2026, 9, 8)) { title = "设计评审"; color = 0xFF8B5CF6 }
        event("release", CalendarDate(2026, 9, 15), CalendarDate(2026, 9, 17)) { title = "版本发布窗口"; color = 0xFFF97316 }
        event("retrospective", CalendarDate(2026, 9, 16)) { title = "迭代复盘"; color = 0xFF14B8A6 }
        event("support", CalendarDate(2026, 9, 16)) { title = "客户支持值班"; color = 0xFFEC4899 }
        event("archive", CalendarDate(2026, 10, 1)) { title = "归档检查"; color = 0xFF64748B }
    }.build()
    private val mainlandHolidayEvents = ChinaMainlandHolidayCalendar.eventsFor(2026)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            RouterNavBar { attr { title = "日历组件展示" } }
            Scroller {
                attr {
                    flex(1f)
                    backgroundColor(Color(0xFFF6F8FC))
                    padding(12f)
                }
                Text {
                    attr {
                        text("一站式日历体验")
                        fontSize(20f)
                        fontWeightBold()
                        color(Color(0xFF1F2937))
                        marginTop(8f)
                    }
                }
                Text {
                    attr {
                        text("一个日历即可体验选择、年月跳转、日程摘要、农历标签和自定义插槽。")
                        fontSize(13f)
                        color(Color(0xFF6B7280))
                        marginTop(8f)
                        marginBottom(12f)
                    }
                }
                ctx.renderExperiencePanel(this, ctx)
                Calendar {
                    ctx.calendar = this
                    attr {
                        width = ctx.pagerData.pageViewWidth - 24f
                        initialMonth = CalendarMonth(2026, 9)
                        selectionMode = CalendarSelectionMode.SINGLE
                        selection = CalendarSelection.Single(CalendarDate(2026, 9, 16))
                        maxRangeDays = 10
                        maxSelectionCount = 3
                        showAdjacentMonths = true
                        swipeMonthEnabled = true
                        monthPickerEnabled = true
                        yearRange = 1..9999
                        showTodayAction = true
                        todayActionText = "回到今天"
                        events = ctx.eventsForDisplay()
                        eventDisplayMode = CalendarEventDisplayMode.DOTS
                        showSupplementaryLabel = true
                        supplementaryProvider = ChinaLunarCalendar
                        markers = mapOf(CalendarDate(2026, 9, 23) to CalendarMarker(listOf(0xFF10B981), "兼容标记"))
                        theme = CalendarTheme(
                            selectedBackgroundColor = 0xFF3D7EFF,
                            rangeBackgroundColor = 0x333D7EFF,
                            todayTextColor = 0xFF3D7EFF
                        )
                        slots = CalendarSlots().apply {
                            headerTitle = { state, actions ->
                                Text {
                                    attr {
                                        text("${state.displayedMonth.title()} ▾")
                                        fontSize(17f)
                                        fontWeightBold()
                                        color(Color(0xFF1D4ED8))
                                    }
                                    event { click { actions.showMonthPicker() } }
                                }
                            }
                            weekday = { state ->
                                Text {
                                    attr {
                                        text(state.label)
                                        fontSize(12f)
                                        color(Color(if (state.isWeekend) 0xFFF97316 else 0xFF64748B))
                                    }
                                }
                            }
                            footer = { state, actions ->
                                View {
                                    attr {
                                        height(38f)
                                        flexDirectionRow()
                                        alignItemsCenter()
                                        justifyContentSpaceBetween()
                                    }
                                    Text {
                                        attr {
                                            text("已选：${state.selection.toChineseDescription()}")
                                            fontSize(12f)
                                            color(Color(0xFF475569))
                                        }
                                    }
                                    Text {
                                        attr { text("清除选择"); fontSize(12f); color(Color(0xFF2563EB)) }
                                        event { click { actions.setSelection(CalendarSelection.None, emitEvent = true) } }
                                    }
                                }
                            }
                        }
                    }
                    event {
                        selectionChanged { selection ->
                            ctx.agenda?.setSelection(selection)
                            ctx.selectionDescription = "当前选择：${selection.toChineseDescription()}"
                            ctx.experienceDescription = "选择已同步到下方日程摘要。"
                        }
                        selectionRejected { reason, _ ->
                            ctx.experienceDescription = "本次选择未生效：${reason.toChineseDescription()}"
                        }
                        visibleRangeChanged { range ->
                            ctx.experienceDescription = "已切换到 ${range.month.title()}，可见范围：${range.start.format()} 至 ${range.end.format()}。"
                        }
                        monthPickerVisibilityChanged { visible ->
                            if (visible) ctx.experienceDescription = "年月面板已打开：先选年份，再选月份。"
                        }
                        todayActionClick { ctx.experienceDescription = "已回到今天所在月份，当前选择保持不变。" }
                    }
                }
                CalendarAgenda {
                    ctx.agenda = this
                    attr {
                        width = ctx.pagerData.pageViewWidth - 24f
                        selection = CalendarSelection.Single(CalendarDate(2026, 9, 16))
                        events = ctx.eventsForDisplay()
                        theme = CalendarTheme(backgroundColor = 0xFFFFFFFF)
                        emptyText = "当天暂无日程；可试试选择 9 月 4、8、15 或 16 日。"
                    }
                    event { eventClick { summary -> ctx.experienceDescription = "已点击日程：${summary.title}（id=${summary.id}）。" } }
                }
                vbind({ ctx.selectionDescription }) {
                    Text {
                        attr {
                            text(ctx.selectionDescription)
                            fontSize(13f)
                            color(Color(0xFF1D4ED8))
                            marginTop(10f)
                        }
                    }
                }
                vbind({ ctx.experienceDescription }) {
                    Text {
                        attr {
                            text(ctx.experienceDescription)
                            fontSize(13f)
                            color(Color(0xFF475569))
                            marginTop(4f)
                            marginBottom(24f)
                        }
                    }
                }
            }
        }
    }

    private fun renderExperiencePanel(container: ViewContainer<*, *>, ctx: CalendarDemoPage) {
        container.View {
            attr {
                padding(12f)
                backgroundColor(Color.WHITE)
                borderRadius(12f)
                marginBottom(12f)
            }
            Text {
                attr { text("功能体验面板"); fontSize(16f); fontWeightBold(); color(Color(0xFF1F2937)); marginBottom(8f) }
            }
            ctx.renderModeSwitch(this, "连续日期", "开启后可连续选择最多 10 天", { ctx.isRangeSelectionEnabled }) {
                val enabled = !ctx.isRangeSelectionEnabled
                ctx.isRangeSelectionEnabled = enabled
                if (enabled) ctx.isMultipleSelectionEnabled = false
                ctx.applySelectionMode()
            }
            ctx.renderModeSwitch(this, "多日期选择", "开启后最多选择 3 个日期", { ctx.isMultipleSelectionEnabled }) {
                val enabled = !ctx.isMultipleSelectionEnabled
                ctx.isMultipleSelectionEnabled = enabled
                if (enabled) ctx.isRangeSelectionEnabled = false
                ctx.applySelectionMode()
            }
            ctx.renderModeSwitch(this, "农历标签", "应用于日历中的所有日期", { ctx.isLunarLabelEnabled }) {
                ctx.isLunarLabelEnabled = !ctx.isLunarLabelEnabled
                ctx.calendar?.setSupplementaryLabelVisible(ctx.isLunarLabelEnabled)
                ctx.experienceDescription = if (ctx.isLunarLabelEnabled) {
                    "农历标签已开启，所有日期格均会显示补充标签。"
                } else {
                    "农历标签已关闭。"
                }
            }
            ctx.renderModeSwitch(this, "大陆节假日", "法定节假日、传统节日与调休", { ctx.isHolidayEnabled }) {
                ctx.isHolidayEnabled = !ctx.isHolidayEnabled
                ctx.updateCalendarEvents()
                ctx.experienceDescription = if (ctx.isHolidayEnabled) {
                    "大陆节假日与调休已载入 Agenda，不会显示或计入日程点。"
                } else {
                    "大陆节假日与调休已关闭。"
                }
            }
            Text {
                attr {
                    text("连续日期与多日期选择为互斥模式；左右滑动可切换月份，标题可打开任意年份和月份的面板。农历开关会作用于所有日期格；大陆节假日会进入 Agenda 但不产生日期格日程点。")
                    fontSize(12f)
                    color(Color(0xFF64748B))
                    marginTop(6f)
                }
            }
        }
    }

    private fun renderModeSwitch(
        container: ViewContainer<*, *>,
        title: String,
        detail: String,
        enabled: () -> Boolean,
        onClick: () -> Unit
    ) {
        container.vbind(enabled) {
            View {
                attr { height(46f); flexDirectionRow(); alignItemsCenter(); justifyContentSpaceBetween() }
                View {
                    attr { flex(1f) }
                    Text { attr { text(title); fontSize(14f); color(Color(0xFF334155)) } }
                    Text { attr { text(detail); fontSize(11f); color(Color(0xFF94A3B8)); marginTop(2f) } }
                }
                View {
                    attr {
                        width(46f)
                        height(26f)
                        allCenter()
                        borderRadius(13f)
                        backgroundColor(Color(if (enabled()) 0xFF2563EB else 0xFFE2E8F0))
                    }
                    Text { attr { text(if (enabled()) "开" else "关"); fontSize(12f); color(Color(if (enabled()) 0xFFFFFFFF else 0xFF64748B)) } }
                    event { click { onClick() } }
                }
            }
        }
    }

    private fun applySelectionMode() {
        val (mode, selection, description) = when {
            isRangeSelectionEnabled -> Triple(
                CalendarSelectionMode.RANGE,
                CalendarSelection.None,
                "连续日期已开启：依次点击开始与结束日期（最多 10 天）。"
            )
            isMultipleSelectionEnabled -> Triple(
                CalendarSelectionMode.MULTIPLE,
                CalendarSelection.None,
                "多日期选择已开启：点击日期可选中或取消（最多 3 个）。"
            )
            else -> Triple(
                CalendarSelectionMode.SINGLE,
                CalendarSelection.Single(CalendarDate(2026, 9, 16)),
                "单日选择已开启，点击任意可用日期即可查看当天日程。"
            )
        }
        calendar?.setSelectionMode(mode, selection, emitEvent = true)
        experienceDescription = description
    }

    private fun eventsForDisplay(): List<CalendarEventSummary> = agendaEvents + if (isHolidayEnabled) mainlandHolidayEvents else emptyList()

    private fun updateCalendarEvents() {
        val events = eventsForDisplay()
        calendar?.setEvents(events)
        agenda?.setEvents(events)
    }
}

private fun CalendarSelection.toChineseDescription(): String = when (this) {
    CalendarSelection.None -> "未选择日期"
    is CalendarSelection.Single -> date.format()
    is CalendarSelection.Range -> if (end == null) "起始日 ${start.format()}，请继续选择结束日" else "${start.format()} 至 ${end.format()}"
    is CalendarSelection.Multiple -> "${dates.size} 天（${dates.sorted().joinToString { it.format() }}）"
}

private fun com.kuikly.kuiklycalendar.calendar.CalendarSelectionRejectedReason.toChineseDescription(): String = when (this) {
    com.kuikly.kuiklycalendar.calendar.CalendarSelectionRejectedReason.DISABLED_DATE -> "该日期不可选"
    com.kuikly.kuiklycalendar.calendar.CalendarSelectionRejectedReason.RANGE_CONTAINS_DISABLED_DATE -> "范围内含不可选日期"
    com.kuikly.kuiklycalendar.calendar.CalendarSelectionRejectedReason.MAX_RANGE_DAYS_EXCEEDED -> "超过最大范围天数"
    com.kuikly.kuiklycalendar.calendar.CalendarSelectionRejectedReason.MAX_SELECTION_COUNT_REACHED -> "已达到最多可选天数"
    com.kuikly.kuiklycalendar.calendar.CalendarSelectionRejectedReason.SELECTION_DISABLED -> "当前日历不允许选择"
}
