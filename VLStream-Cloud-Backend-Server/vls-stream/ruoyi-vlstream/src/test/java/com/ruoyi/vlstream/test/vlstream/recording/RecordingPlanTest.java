package com.ruoyi.vlstream.test.vlstream.recording;

import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class RecordingPlanTest {
    private RecordingPlan plan(String start, String end, Integer... days) {
        RecordingPlan plan = new RecordingPlan(); plan.enabled = true;
        plan.startTime = start; plan.endTime = end; plan.weekDays = Arrays.asList(days); return plan;
    }
    @Test void sameDayIncludesStartExcludesEnd() {
        RecordingPlan plan = plan("08:00", "18:00", 1);
        assertFalse(plan.matches(LocalDateTime.parse("2026-11-09T07:59:59")));
        assertTrue(plan.matches(LocalDateTime.parse("2026-11-09T08:00:00")));
        assertFalse(plan.matches(LocalDateTime.parse("2026-11-09T18:00:00")));
    }
    @Test void crossMidnightUsesStartingWeekdayAndWrapsSunday() {
        RecordingPlan plan = plan("22:00", "06:00", 7);
        assertTrue(plan.matches(LocalDateTime.parse("2026-11-08T23:00:00")));
        assertTrue(plan.matches(LocalDateTime.parse("2026-11-09T05:59:59")));
        assertFalse(plan.matches(LocalDateTime.parse("2026-11-09T06:00:00")));
        assertFalse(plan.matches(LocalDateTime.parse("2026-11-09T22:00:00")));
    }
    @Test void equalTimesMeanCalendarDay() {
        RecordingPlan plan = plan("00:00", "00:00", 5);
        assertTrue(plan.matches(LocalDateTime.parse("2026-11-06T23:59:59")));
        assertFalse(plan.matches(LocalDateTime.parse("2026-11-07T00:00:00")));
    }
    @Test void disabledPlanDoesNotRecord() {
        RecordingPlan plan = plan("00:00", "00:00", 5); plan.enabled = false;
        assertFalse(plan.matches(LocalDateTime.parse("2026-11-06T12:00:00")));
    }
    @Test void invalidTimesAndWeekdaysAreRejected() {
        assertThrows(ServiceException.class, () -> plan("24:00", "06:00", 1).validate());
        assertThrows(ServiceException.class, () -> plan("12:00", "06:00", 8).validate());
        assertThrows(ServiceException.class, () -> plan("12:00", "06:00").validate());
    }
}
