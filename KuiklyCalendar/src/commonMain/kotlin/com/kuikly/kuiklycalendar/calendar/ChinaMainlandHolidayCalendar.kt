package com.kuikly.kuiklycalendar.calendar

/**
 * 中国大陆节假日与调休数据。
 *
 * 调休安排由国务院按年度发布；每年公布后向 [eventsFor] 补充相应年度数据即可。
 * 返回的条目仅用于 Agenda，不显示或计入月历日程点。
 */
object ChinaMainlandHolidayCalendar {
    fun eventsFor(year: Int): List<CalendarEventSummary> = when (year) {
        2026 -> eventsFor2026()
        else -> emptyList()
    }

    private fun eventsFor2026(): List<CalendarEventSummary> = listOf(
        holiday("new-year", "元旦", 1, 1, 1, 3),
        workday("new-year-makeup", "元旦调休上班", 1, 4),
        holiday("spring-festival", "春节", 2, 15, 2, 23),
        workday("spring-festival-makeup-1", "春节调休上班", 2, 14),
        workday("spring-festival-makeup-2", "春节调休上班", 2, 28),
        holiday("qingming", "清明节", 4, 4, 4, 6),
        holiday("labour-day", "劳动节", 5, 1, 5, 5),
        workday("labour-day-makeup", "劳动节调休上班", 5, 9),
        holiday("dragon-boat", "端午节", 6, 19, 6, 21),
        holiday("mid-autumn", "中秋节", 9, 25, 9, 27),
        workday("national-day-makeup-1", "国庆节调休上班", 9, 20),
        holiday("national-day", "国庆节", 10, 1, 10, 7),
        workday("national-day-makeup-2", "国庆节调休上班", 10, 10),
        CalendarEventSummary(
            id = "memorial-918",
            startDate = CalendarDate(2026, 9, 18),
            title = "九一八事变纪念日",
            color = 0xFF64748B,
            type = "memorial",
            showsCalendarIndicator = false
        )
    )

    private fun holiday(id: String, title: String, startMonth: Int, startDay: Int, endMonth: Int, endDay: Int) =
        CalendarEventSummary(
            id = id,
            startDate = CalendarDate(2026, startMonth, startDay),
            endDate = CalendarDate(2026, endMonth, endDay),
            title = title,
            color = 0xFFDC2626,
            type = "holiday",
            showsCalendarIndicator = false
        )

    private fun workday(id: String, title: String, month: Int, day: Int) = CalendarEventSummary(
        id = id,
        startDate = CalendarDate(2026, month, day),
        title = title,
        color = 0xFF475569,
        type = "makeup_workday",
        showsCalendarIndicator = false
    )
}
