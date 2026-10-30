package com.ruoyi.vlstream.test.vlstream.cloudai;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class CloudAiScheduleTest {
    private CloudAiSchedule.Definition daily(String start, String end) {
        CloudAiSchedule.Definition definition = new CloudAiSchedule.Definition();
        definition.kind = "DAILY";
        definition.firstDate = "2026-10-30";
        definition.startTime = start;
        definition.endTime = end;
        return definition;
    }

    @Test
    void restartWithinWindowKeepsOriginalIdentityAndDeadline() {
        CloudAiSchedule schedule = new CloudAiSchedule(daily("09:00", "18:00"));
        CloudAiSchedule.Window before = schedule.currentOrNext(Instant.parse("2026-10-30T00:00:00Z"));
        CloudAiSchedule.Window restarted = schedule.currentOrNext(Instant.parse("2026-10-30T05:00:00Z"));
        assertEquals(before.key(), restarted.key());
        assertEquals(Instant.parse("2026-10-30T10:00:00Z"), restarted.end);
        assertEquals(32400, restarted.seconds());
    }

    @Test
    void overnightWindowBelongsToItsStartDate() {
        CloudAiSchedule.Definition definition = daily("22:00", "06:00");
        definition.lastDate = "2026-10-30";
        CloudAiSchedule schedule = new CloudAiSchedule(definition);
        CloudAiSchedule.Window window = schedule.currentOrNext(Instant.parse("2026-10-30T19:00:00Z"));
        assertEquals(Instant.parse("2026-10-30T14:00:00Z"), window.start);
        assertEquals(Instant.parse("2026-10-30T22:00:00Z"), window.end);
        assertNull(schedule.currentOrNext(window.end));
        assertNull(schedule.after(window));
    }

    @Test
    void missedWindowDoesNotMoveItsEndOrRunAfterExpiry() {
        CloudAiSchedule.Definition definition = new CloudAiSchedule.Definition();
        definition.kind = "ONCE";
        definition.onceStart = "2026-10-30T09:00:00";
        definition.onceEnd = "2026-10-30T09:10:00";
        CloudAiSchedule schedule = new CloudAiSchedule(definition);
        assertNotNull(schedule.currentOrNext(Instant.parse("2026-10-30T01:09:59Z")));
        assertNull(schedule.currentOrNext(Instant.parse("2026-10-30T01:10:00Z")));
    }

    @Test
    void weeklyRuleFindsNextWeekAndRespectsEndDate() {
        CloudAiSchedule.Definition definition = daily("08:00", "09:00");
        definition.kind = "WEEKLY";
        definition.weekdays = new HashSet<>(Arrays.asList(1, 5));
        definition.lastDate = "2026-11-02";
        CloudAiSchedule schedule = new CloudAiSchedule(definition);
        CloudAiSchedule.Window friday = schedule.on(LocalDate.parse("2026-10-30"));
        assertNotNull(friday);
        CloudAiSchedule.Window monday = schedule.after(friday);
        assertEquals(Instant.parse("2026-11-02T00:00:00Z"), monday.start);
        assertNull(schedule.after(monday));
        assertNull(schedule.on(LocalDate.parse("2026-10-31")));
    }

    @Test
    void futureValidityStartsAtFirstDateInsteadOfToday() {
        CloudAiSchedule.Definition definition = daily("08:00", "09:00");
        definition.firstDate = "2030-01-01";
        assertEquals(Instant.parse("2030-01-01T00:00:00Z"),
            new CloudAiSchedule(definition).currentOrNext(Instant.parse("2026-10-30T00:00:00Z")).start);
    }

    @Test
    void manualDurationIsSeparateFromCalendarTime() {
        CloudAiSchedule.Definition definition = new CloudAiSchedule.Definition();
        definition.kind = "MANUAL";
        definition.durationSeconds = 600;
        CloudAiSchedule schedule = new CloudAiSchedule(definition);
        assertEquals(600, schedule.durationSeconds());
        assertNull(schedule.currentOrNext(Instant.parse("2026-10-30T00:00:00Z")));
        definition.durationSeconds = 0;
        assertThrows(IllegalArgumentException.class, () -> new CloudAiSchedule(definition));
    }

    @Test
    void invalidAndAmbiguousRulesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CloudAiSchedule(daily("09:00", "09:00")));
        assertThrows(IllegalArgumentException.class, () -> new CloudAiSchedule(daily("25:00", "09:00")));
        CloudAiSchedule.Definition definition = daily("08:00", "09:00");
        definition.kind = "WEEKLY";
        assertThrows(IllegalArgumentException.class, () -> new CloudAiSchedule(definition));
        definition.weekdays.add(0);
        assertThrows(IllegalArgumentException.class, () -> new CloudAiSchedule(definition));
    }
}
