package com.kuikly.kuiklycalendar.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarMathTest {
    @Test
    fun leapYearRulesFollowGregorianCalendar() {
        assertTrue(CalendarMath.isLeapYear(2024))
        assertTrue(CalendarMath.isLeapYear(2000))
        assertFalse(CalendarMath.isLeapYear(1900))
        assertEquals(29, CalendarMath.daysInMonth(2024, 2))
        assertEquals(28, CalendarMath.daysInMonth(1900, 2))
    }

    @Test
    fun dayOfWeekUsesSundayAsZero() {
        assertEquals(CalendarWeekday.SUNDAY.index, CalendarMath.dayOfWeek(2026, 7, 5))
        assertEquals(CalendarWeekday.MONDAY.index, CalendarMath.dayOfWeek(2026, 7, 6))
        assertEquals(CalendarWeekday.THURSDAY.index, CalendarMath.dayOfWeek(1970, 1, 1))
    }

    @Test
    fun addDaysCrossesMonthAndYearInBothDirections() {
        assertEquals(CalendarDate(2025, 1, 1), CalendarMath.addDays(CalendarDate(2024, 12, 31), 1))
        assertEquals(CalendarDate(2024, 2, 29), CalendarMath.addDays(CalendarDate(2024, 3, 1), -1))
        assertEquals(CalendarDate(2024, 3, 1), CalendarMath.addDays(CalendarDate(2024, 2, 28), 2))
    }

    @Test
    fun dateRejectsInvalidMonthOrDay() {
        assertFailsWith<IllegalArgumentException> { CalendarDate(2026, 13, 1) }
        assertFailsWith<IllegalArgumentException> { CalendarDate(2026, 2, 29) }
        assertFailsWith<IllegalArgumentException> { CalendarDate(2024, 2, 30) }
    }

    @Test
    fun dateFormatsAndComparesAsNaturalDate() {
        val date = CalendarDate(2026, 7, 5)
        assertEquals("2026-07-05", date.format())
        assertTrue(CalendarDate(2026, 7, 4) < date)
        assertTrue(CalendarDate(2026, 8, 1) > date)
    }

    @Test
    fun daysBetweenHandlesLeapYearBoundaries() {
        assertEquals(1, CalendarMath.daysBetween(CalendarDate(2024, 2, 28), CalendarDate(2024, 2, 29)))
        assertEquals(2, CalendarMath.daysBetween(CalendarDate(2024, 2, 28), CalendarDate(2024, 3, 1)))
        assertEquals(365, CalendarMath.daysBetween(CalendarDate(2025, 1, 1), CalendarDate(2026, 1, 1)))
    }

    @Test
    fun monthNavigationCrossesYearBoundariesAndKeepsChineseTitle() {
        assertEquals(CalendarMonth(2025, 12), CalendarMonth(2026, 1).previous())
        assertEquals(CalendarMonth(2027, 1), CalendarMonth(2026, 12).next())
        assertEquals("2026年11月", CalendarMonth(2026, 11).title())
    }
}
