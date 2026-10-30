package com.ruoyi.vlstream.test.vlstream.cloudai;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Calendar windows are independent of worker uptime and never extended by queuing. */
public final class CloudAiSchedule {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    public enum Kind { MANUAL, ONCE, DAILY, WEEKLY }

    public static final class Definition {
        public String kind;
        public Integer durationSeconds;
        public String onceStart;
        public String onceEnd;
        public String firstDate;
        public String lastDate;
        public String startTime;
        public String endTime;
        public Set<Integer> weekdays = new HashSet<>();
    }

    public static final class Window {
        public final Instant start;
        public final Instant end;

        Window(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

        public String key() { return Long.toString(start.getEpochSecond()); }
        public boolean contains(Instant instant) {
            return !instant.isBefore(start) && instant.isBefore(end);
        }
        public long seconds() { return Duration.between(start, end).getSeconds(); }
    }

    private final Kind kind;
    private final int durationSeconds;
    private final Window once;
    private final LocalDate firstDate;
    private final LocalDate lastDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Set<Integer> weekdays;

    public CloudAiSchedule(Definition definition) {
        if (definition == null) throw invalid("请选择任务类型");
        try {
            kind = Kind.valueOf(definition.kind == null ? "" : definition.kind);
        } catch (IllegalArgumentException error) {
            throw invalid("任务类型须为手工、指定日期、每天或每周");
        }
        durationSeconds = definition.durationSeconds == null ? 0 : definition.durationSeconds;
        if (kind == Kind.MANUAL) {
            if (durationSeconds < 1) {
                throw invalid("手工任务运行时长必须大于0秒");
            }
            once = null;
            firstDate = lastDate = null;
            startTime = endTime = null;
            weekdays = Collections.emptySet();
            return;
        }
        try {
            if (kind == Kind.ONCE) {
                Instant start = LocalDateTime.parse(definition.onceStart).atZone(ZONE).toInstant();
                Instant end = LocalDateTime.parse(definition.onceEnd).atZone(ZONE).toInstant();
                if (!end.isAfter(start)) throw invalid("结束时间必须晚于开始时间");
                once = new Window(start, end);
                firstDate = lastDate = null;
                startTime = endTime = null;
                weekdays = Collections.emptySet();
                return;
            }
            once = null;
            firstDate = LocalDate.parse(definition.firstDate);
            lastDate = isBlank(definition.lastDate) ? null : LocalDate.parse(definition.lastDate);
            if (lastDate != null && lastDate.isBefore(firstDate)) throw invalid("有效期结束日期不能早于开始日期");
            startTime = LocalTime.parse(definition.startTime);
            endTime = LocalTime.parse(definition.endTime);
            if (startTime.equals(endTime)) throw invalid("开始和结束时刻不能相同；跨午夜请填写不同的时刻");
            weekdays = definition.weekdays == null ? Collections.emptySet() : new HashSet<>(definition.weekdays);
            if (kind == Kind.WEEKLY && (weekdays.isEmpty() || weekdays.stream().anyMatch(day -> day == null || day < 1 || day > 7))) {
                throw invalid("每周任务至少选择一个有效星期");
            }
        } catch (DateTimeParseException | NullPointerException error) {
            throw invalid("请填写有效的日期和起止时间");
        }
    }

    public Kind kind() { return kind; }
    public int durationSeconds() { return durationSeconds; }

    /** Includes an already-started, still-open window, allowing a restart to resume its original occurrence. */
    public Window currentOrNext(Instant now) {
        if (kind == Kind.MANUAL) return null;
        if (once != null) return now.isBefore(once.end) ? once : null;
        LocalDate day = now.atZone(ZONE).toLocalDate().minusDays(1);
        if (day.isBefore(firstDate)) day = firstDate;
        // A weekly rule necessarily has an occurrence within seven days of its eligible range.
        for (int offset = 0; offset < 9; offset++, day = day.plusDays(1)) {
            if (lastDate != null && day.isAfter(lastDate)) return null;
            Window candidate = on(day);
            if (candidate != null && now.isBefore(candidate.end)) return candidate;
        }
        return null;
    }

    /** Next distinct occurrence; touching windows never execute twice at the same instant. */
    public Window after(Window previous) {
        if (kind == Kind.MANUAL || kind == Kind.ONCE || previous == null) return null;
        LocalDate day = previous.start.atZone(ZONE).toLocalDate().plusDays(1);
        for (int offset = 0; offset < 7; offset++, day = day.plusDays(1)) {
            if (lastDate != null && day.isAfter(lastDate)) return null;
            Window candidate = on(day);
            if (candidate != null) return candidate;
        }
        return null;
    }

    public Window on(LocalDate day) {
        if (kind == Kind.MANUAL) return null;
        if (once != null) return once.start.atZone(ZONE).toLocalDate().equals(day) ? once : null;
        if (day.isBefore(firstDate) || lastDate != null && day.isAfter(lastDate)) return null;
        if (kind == Kind.WEEKLY && !weekdays.contains(day.getDayOfWeek().getValue())) return null;
        LocalDate endDay = endTime.isAfter(startTime) ? day : day.plusDays(1);
        return new Window(day.atTime(startTime).atZone(ZONE).toInstant(), endDay.atTime(endTime).atZone(ZONE).toInstant());
    }

    private static boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
