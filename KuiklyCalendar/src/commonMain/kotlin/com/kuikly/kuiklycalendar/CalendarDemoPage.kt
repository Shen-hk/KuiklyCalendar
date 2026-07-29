package com.kuikly.kuiklycalendar

import com.kuikly.kuiklycalendar.base.BasePager
import com.kuikly.kuiklycalendar.calendar.Calendar
import com.kuikly.kuiklycalendar.calendar.CalendarDate
import com.kuikly.kuiklycalendar.calendar.CalendarMarker
import com.kuikly.kuiklycalendar.calendar.CalendarMonth
import com.kuikly.kuiklycalendar.calendar.CalendarTheme
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/** Issue #1476 的可运行演示页：路由名称为 calendar_demo。 */
@Page("calendar_demo")
internal class CalendarDemoPage : BasePager() {
    private var selectedDescription by observable("请选择一个可用日期")
    private var monthDescription by observable("当前演示月份：2026年7月")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            RouterNavBar {
                attr { title = "日历组件演示" }
            }
            Scroller {
                attr {
                    flex(1f)
                    backgroundColor(Color(0xFFF6F8FC))
                    padding(12f)
                }
                Text {
                    attr {
                        text("月历选择与日程标记")
                        fontSize(20f)
                        fontWeightBold()
                        color(Color(0xFF1F2937))
                        marginTop(8f)
                    }
                }
                Text {
                    attr {
                        text("点击日期可选择；左右箭头切换月份。灰色日期不可选，圆点表示当天有日程。")
                        fontSize(13f)
                        color(Color(0xFF6B7280))
                        marginTop(8f)
                        marginBottom(12f)
                    }
                }
                Calendar {
                    attr {
                        width = ctx.pagerData.pageViewWidth - 24f
                        initialMonth = CalendarMonth(2026, 7)
                        selectedDate = CalendarDate(2026, 7, 15)
                        minDate = CalendarDate(2026, 7, 3)
                        maxDate = CalendarDate(2026, 9, 25)
                        disabledDates = setOf(CalendarDate(2026, 7, 18), CalendarDate(2026, 7, 19))
                        showAdjacentMonths = true
                        markers = mapOf(
                            CalendarDate(2026, 7, 8) to CalendarMarker(listOf(0xFF4F8FFF), "项目例会"),
                            CalendarDate(2026, 7, 15) to CalendarMarker(listOf(0xFF22C55E, 0xFFF59E0B), "发布与评审"),
                            CalendarDate(2026, 7, 23) to CalendarMarker(listOf(0xFFEF4444), "截止日期")
                        )
                        theme = CalendarTheme(
                            selectedBackgroundColor = 0xFF5B6EF5,
                            todayTextColor = 0xFF5B6EF5
                        )
                    }
                    event {
                        dateSelected { date ->
                            ctx.selectedDescription = "已选择：${date.format()}（已通过 dateSelected 回调返回）"
                        }
                        monthChanged { month ->
                            ctx.monthDescription = "当前演示月份：${month.title()}"
                        }
                    }
                }
                View {
                    attr {
                        marginTop(16f)
                        padding(14f)
                        backgroundColor(Color.WHITE)
                        borderRadius(12f)
                    }
                    vbind({ ctx.selectedDescription }) {
                        Text {
                            attr {
                                text(ctx.selectedDescription)
                                fontSize(14f)
                                color(Color(0xFF374151))
                            }
                        }
                    }
                    vbind({ ctx.monthDescription }) {
                        Text {
                            attr {
                                text(ctx.monthDescription)
                                fontSize(13f)
                                color(Color(0xFF6B7280))
                                marginTop(8f)
                            }
                        }
                    }
                }
                Text {
                    attr {
                        text("示例说明：本页固定展示 2026 年 7 月，便于稳定验收日期网格、禁用态、选中态和多色日程标记。")
                        fontSize(12f)
                        color(Color(0xFF94A3B8))
                        marginTop(16f)
                        marginBottom(24f)
                    }
                }
            }
        }
    }
}
