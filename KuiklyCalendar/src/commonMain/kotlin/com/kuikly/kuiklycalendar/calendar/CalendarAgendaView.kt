package com.kuikly.kuiklycalendar.calendar

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

typealias CalendarAgendaEventContent = ViewContainer<*, *>.(CalendarEventSummary, CalendarAgendaState) -> Unit
typealias CalendarAgendaEmptyContent = ViewContainer<*, *>.(CalendarAgendaState) -> Unit

/**
 * 与 Calendar 松耦合的日程列表。它不加载网络数据，业务只需同步 selection 和 events。
 */
class CalendarAgendaView : ComposeView<CalendarAgendaAttr, CalendarAgendaEvent>() {
    private var agendaSelection: CalendarSelection by observable(CalendarSelection.None)
    private var agendaEvents: List<CalendarEventSummary> by observable(emptyList())
    private var renderVersion: Int by observable(0)

    override fun createAttr(): CalendarAgendaAttr = CalendarAgendaAttr()

    override fun createEvent(): CalendarAgendaEvent = CalendarAgendaEvent()

    override fun created() {
        super.created()
        agendaSelection = attr.selection
        agendaEvents = attr.events.toList()
    }

    fun setSelection(value: CalendarSelection) {
        agendaSelection = value
        renderVersion++
    }

    fun setEvents(value: List<CalendarEventSummary>) {
        agendaEvents = value.toList()
        renderVersion++
    }

    fun currentState(): CalendarAgendaState = state()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            vbind({ ctx.renderVersion }) {
                val state = ctx.state()
                View {
                    attr {
                        width(if (ctx.attr.width > 0f) ctx.attr.width else ctx.pagerData.pageViewWidth)
                        padding(ctx.attr.padding)
                        backgroundColor(Color(ctx.attr.theme.backgroundColor))
                        borderRadius(ctx.attr.cornerRadius)
                    }
                    Text {
                        attr {
                            text(ctx.attr.titleFormatter(state.date))
                            fontSize(ctx.attr.titleFontSize)
                            fontWeightBold()
                            color(Color(ctx.attr.theme.titleColor))
                            marginBottom(8f)
                        }
                    }
                    if (state.date == null || state.events.isEmpty()) {
                        ctx.renderEmpty(this, state)
                    } else {
                        state.events.forEach { summary -> ctx.renderEventItem(this, summary, state) }
                    }
                }
            }
        }
    }

    private fun state(): CalendarAgendaState {
        val date = attr.selectedDate ?: agendaSelection.primaryDate()
        return CalendarAgendaState(date, agendaSelection, date?.let { selected -> agendaEvents.filter { it.occursOn(selected) } }.orEmpty())
    }

    private fun renderEmpty(container: ViewContainer<*, *>, state: CalendarAgendaState) {
        attr.emptyContent?.invoke(container, state) ?: container.Text {
            attr {
                text(if (state.date == null) this@CalendarAgendaView.attr.noSelectionText else this@CalendarAgendaView.attr.emptyText)
                fontSize(13f)
                color(Color(this@CalendarAgendaView.attr.theme.weekdayColor))
                marginTop(8f)
                marginBottom(8f)
            }
        }
    }

    private fun renderEventItem(
        container: ViewContainer<*, *>,
        summary: CalendarEventSummary,
        state: CalendarAgendaState
    ) {
        attr.eventContent?.invoke(container, summary, state) ?: container.View {
            attr {
                minHeight(44f)
                flexDirectionRow()
                alignItemsCenter()
                backgroundColor(Color(0xFFF8FAFC))
                borderRadius(8f)
                paddingLeft(10f)
                paddingRight(10f)
                marginBottom(6f)
            }
            View {
                attr {
                    size(8f, 8f)
                    borderRadius(4f)
                    backgroundColor(Color(summary.color))
                    marginRight(8f)
                }
            }
            Text {
                attr {
                    text(if (summary.title.isBlank()) "未命名日程" else summary.title)
                    fontSize(14f)
                    color(Color(this@CalendarAgendaView.attr.theme.dayTextColor))
                    flex(1f)
                }
            }
            if (!summary.isAllDay) {
                Text {
                    attr {
                        text("非全天")
                        fontSize(11f)
                        color(Color(this@CalendarAgendaView.attr.theme.weekdayColor))
                    }
                }
            }
            event { click { this@CalendarAgendaView.event.dispatchEventClick(summary) } }
        }
    }

    private fun CalendarSelection.primaryDate(): CalendarDate? = when (this) {
        CalendarSelection.None -> null
        is CalendarSelection.Single -> date
        is CalendarSelection.Range -> start
        is CalendarSelection.Multiple -> dates.minOrNull()
    }
}

class CalendarAgendaAttr : ComposeAttr() {
    var width: Float = 0f
    var selection: CalendarSelection = CalendarSelection.None
    /** 非 null 时优先于 selection，用于仅展示一个指定自然日。 */
    var selectedDate: CalendarDate? = null
    var events: List<CalendarEventSummary> = emptyList()
    /** Uses the same declaration DSL as Calendar for fixed local schedules. */
    fun events(init: CalendarEventsDsl.() -> Unit) {
        events = calendarEvents(init)
    }
    var theme: CalendarTheme = CalendarTheme()
    var titleFormatter: (CalendarDate?) -> String = { date ->
        if (date == null) "日程" else "${date.format()} 的日程"
    }
    var eventContent: CalendarAgendaEventContent? = null
    var emptyContent: CalendarAgendaEmptyContent? = null
    var noSelectionText: String = "请选择日期查看日程"
    var emptyText: String = "当天暂无日程"
    var padding: Float = 12f
    var cornerRadius: Float = 12f
    var titleFontSize: Float = 16f
}

class CalendarAgendaEvent : ComposeEvent() {
    private var eventClickListener: ((CalendarEventSummary) -> Unit)? = null

    fun eventClick(listener: (CalendarEventSummary) -> Unit) {
        eventClickListener = listener
    }

    internal fun dispatchEventClick(summary: CalendarEventSummary) = eventClickListener?.invoke(summary)
}

fun ViewContainer<*, *>.CalendarAgenda(init: CalendarAgendaView.() -> Unit) {
    addChild(CalendarAgendaView(), init)
}
