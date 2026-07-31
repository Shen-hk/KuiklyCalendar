package com.kuikly.kuiklycalendar.calendar

import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarEventsDslTest {
    @Test
    fun topLevelDslBuildsReusableSingleAndRangeEvents() {
        val events = calendarEvents {
            event("review", CalendarDate(2026, 9, 8)) {
                title = "Design review"
            }
            event {
                id = "release"
                during(CalendarDate(2026, 9, 15), CalendarDate(2026, 9, 17))
                title = "Release window"
            }
        }

        assertEquals(2, events.size)
        assertEquals(CalendarDate(2026, 9, 8), events[0].startDate)
        assertEquals(CalendarDate(2026, 9, 8), events[0].endDate)
        assertEquals(CalendarDate(2026, 9, 17), events[1].endDate)
    }
}
