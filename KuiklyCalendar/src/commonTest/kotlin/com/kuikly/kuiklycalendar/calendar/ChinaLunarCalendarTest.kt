package com.kuikly.kuiklycalendar.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChinaLunarCalendarTest {
    @Test
    fun convertsKnown2026TraditionalFestivalDates() {
        val springFestival = ChinaLunarCalendar.lunarDate(CalendarDate(2026, 2, 17))
        val midAutumn = ChinaLunarCalendar.lunarDate(CalendarDate(2026, 9, 25))

        assertEquals(ChinaLunarDate(2026, 1, 1, false), springFestival)
        assertEquals(ChinaLunarDate(2026, 8, 15, false), midAutumn)
        assertEquals("正月", ChinaLunarCalendar.supplementaryLabel(CalendarDate(2026, 2, 17)))
        assertEquals("十五", ChinaLunarCalendar.supplementaryLabel(CalendarDate(2026, 9, 25)))
    }

    @Test
    fun returnsNoLabelOutsideSupportedRange() {
        assertNull(ChinaLunarCalendar.supplementaryLabel(CalendarDate(1899, 12, 31)))
        assertNull(ChinaLunarCalendar.supplementaryLabel(CalendarDate(2101, 1, 1)))
    }
}
