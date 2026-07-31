package com.kuikly.kuiklycalendar.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarEventSummaryTest {
    @Test
    fun eventCoversEveryNaturalDayInItsInclusiveRange() {
        val event = CalendarEventSummary(
            id = "release",
            startDate = CalendarDate(2026, 9, 15),
            endDate = CalendarDate(2026, 9, 17)
        )

        assertFalse(event.occursOn(CalendarDate(2026, 9, 14)))
        assertTrue(event.occursOn(CalendarDate(2026, 9, 15)))
        assertTrue(event.occursOn(CalendarDate(2026, 9, 16)))
        assertTrue(event.occursOn(CalendarDate(2026, 9, 17)))
        assertFalse(event.occursOn(CalendarDate(2026, 9, 18)))
    }

    @Test
    fun eventRejectsBlankIdAndReversedDateRange() {
        assertFailsWith<IllegalArgumentException> {
            CalendarEventSummary("", CalendarDate(2026, 9, 1))
        }
        assertFailsWith<IllegalArgumentException> {
            CalendarEventSummary("invalid", CalendarDate(2026, 9, 2), CalendarDate(2026, 9, 1))
        }
    }

    @Test
    fun holidayOrMemorialCanAppearInAgendaWithoutCalendarIndicator() {
        val holiday = CalendarEventSummary(
            id = "national-day",
            startDate = CalendarDate(2026, 10, 1),
            type = "holiday",
            showsCalendarIndicator = false
        )

        assertTrue(holiday.occursOn(CalendarDate(2026, 10, 1)))
        assertFalse(holiday.showsCalendarIndicator)
        assertTrue(CalendarEventSummary("normal", CalendarDate(2026, 10, 2)).showsCalendarIndicator)
    }

    @Test
    fun eventDslBuildsSingleAndCrossDaySchedulePoints() {
        val events = CalendarEventsDsl().apply {
            event {
                id = "review"
                startDate = CalendarDate(2026, 9, 8)
                title = "Design review"
                color = 0xFF8B5CF6
                extra("projectId", "calendar")
            }
            event("release", CalendarDate(2026, 9, 15), CalendarDate(2026, 9, 17)) {
                title = "Release window"
                showsCalendarIndicator = false
            }
        }.build()

        assertEquals(2, events.size)
        assertEquals(CalendarDate(2026, 9, 8), events[0].endDate)
        assertEquals("calendar", events[0].extra["projectId"])
        assertEquals(CalendarDate(2026, 9, 17), events[1].endDate)
        assertFalse(events[1].showsCalendarIndicator)
    }

    @Test
    fun eventDslRequiresStartDate() {
        assertFailsWith<IllegalArgumentException> {
            CalendarEventsDsl().apply {
                event { id = "invalid" }
            }
        }
    }

    @Test
    fun visibleRangeAlwaysContainsFortyTwoConsecutiveDays() {
        val range = CalendarMath.visibleRange(CalendarMonth(2026, 9), CalendarWeekday.MONDAY)

        assertEquals(CalendarDate(2026, 8, 31), range.start)
        assertEquals(CalendarDate(2026, 10, 11), range.end)
        assertEquals(41, CalendarMath.daysBetween(range.start, range.end))
    }

    @Test
    fun eventIndexClipsCrossMonthEventsToVisibleRange() {
        val range = CalendarVisibleRange(
            CalendarMonth(2026, 9),
            CalendarDate(2026, 8, 31),
            CalendarDate(2026, 10, 11)
        )
        val crossMonth = CalendarEventSummary(
            "cross-month",
            CalendarDate(2026, 8, 29),
            CalendarDate(2026, 9, 2)
        )
        val outside = CalendarEventSummary("outside", CalendarDate(2026, 11, 1))
        val index = CalendarEventIndex.forVisibleRange(listOf(crossMonth, outside), range)

        assertEquals(listOf(crossMonth), index[CalendarDate(2026, 8, 31)])
        assertEquals(listOf(crossMonth), index[CalendarDate(2026, 9, 2)])
        assertTrue(index[CalendarDate(2026, 9, 3)].isNullOrEmpty())
        assertFalse(index.values.flatten().contains(outside))
    }
}
