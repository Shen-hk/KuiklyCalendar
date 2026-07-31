package com.kuikly.kuiklycalendar.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CalendarSelectionMachineTest {
    @Test
    fun rangeSelectionClosesAfterSecondDate() {
        val start = CalendarDate(2026, 7, 10)
        val end = CalendarDate(2026, 7, 15)
        val first = select(CalendarSelection.None, CalendarSelectionMode.RANGE, start)
        val second = select(first.selection, CalendarSelectionMode.RANGE, end)

        assertEquals(CalendarSelection.Range(start, end), second.selection)
        assertEquals(CalendarSelectionPosition.RANGE_MIDDLE, CalendarSelectionMachine.positionOf(second.selection, CalendarDate(2026, 7, 12)))
        assertEquals(CalendarSelectionPosition.RANGE_END, CalendarSelectionMachine.positionOf(second.selection, end))
    }

    @Test
    fun rangeSelectionUsesConfiguredBackwardDatePolicy() {
        val start = CalendarDate(2026, 7, 15)
        val earlier = CalendarDate(2026, 7, 10)
        val pending = CalendarSelection.Range(start)

        assertEquals(CalendarSelection.Range(earlier), select(pending, CalendarSelectionMode.RANGE, earlier).selection)
        assertEquals(
            CalendarSelection.Range(earlier, start),
            select(pending, CalendarSelectionMode.RANGE, earlier, policy = RangeOverflowPolicy.SWAP).selection
        )
    }

    @Test
    fun rangeSelectionRejectsDatesBeyondMaximumLength() {
        val start = CalendarDate(2026, 7, 1)
        val result = select(CalendarSelection.Range(start), CalendarSelectionMode.RANGE, CalendarDate(2026, 7, 5), maxRangeDays = 3)

        assertEquals(CalendarSelection.Range(start), result.selection)
        assertEquals(CalendarSelectionRejectedReason.MAX_RANGE_DAYS_EXCEEDED, result.rejectionReason)
    }

    @Test
    fun multipleSelectionTogglesAndHonoursMaximumCount() {
        val one = CalendarDate(2026, 7, 1)
        val two = CalendarDate(2026, 7, 2)
        val three = CalendarDate(2026, 7, 3)
        val first = select(CalendarSelection.None, CalendarSelectionMode.MULTIPLE, one, maxSelectionCount = 2)
        val second = select(first.selection, CalendarSelectionMode.MULTIPLE, two, maxSelectionCount = 2)
        val rejected = select(second.selection, CalendarSelectionMode.MULTIPLE, three, maxSelectionCount = 2)
        val toggled = select(second.selection, CalendarSelectionMode.MULTIPLE, one, maxSelectionCount = 2)

        assertIs<CalendarSelection.Multiple>(second.selection)
        assertEquals(setOf(one, two), (second.selection as CalendarSelection.Multiple).dates)
        assertEquals(CalendarSelectionRejectedReason.MAX_SELECTION_COUNT_REACHED, rejected.rejectionReason)
        assertEquals(CalendarSelection.Multiple(setOf(two)), toggled.selection)
    }

    @Test
    fun noneModeDoesNotChangeSelection() {
        val original = CalendarSelection.Single(CalendarDate(2026, 7, 1))
        val result = select(original, CalendarSelectionMode.NONE, CalendarDate(2026, 7, 2))

        assertEquals(original, result.selection)
        assertEquals(CalendarSelectionRejectedReason.SELECTION_DISABLED, result.rejectionReason)
    }

    private fun select(
        current: CalendarSelection,
        mode: CalendarSelectionMode,
        date: CalendarDate,
        maxSelectionCount: Int = 0,
        maxRangeDays: Int = 0,
        policy: RangeOverflowPolicy = RangeOverflowPolicy.RESTART
    ): CalendarSelectionOutcome {
        return CalendarSelectionMachine.select(current, mode, date, maxSelectionCount, maxRangeDays, policy)
    }
}
