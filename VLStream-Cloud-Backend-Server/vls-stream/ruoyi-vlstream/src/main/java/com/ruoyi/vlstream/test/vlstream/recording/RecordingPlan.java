package com.ruoyi.vlstream.test.vlstream.recording;

import com.ruoyi.common.exception.ServiceException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** A recurring window in Asia/Shanghai; a cross-midnight window belongs to its starting weekday. */
public class RecordingPlan {
    public boolean enabled;
    public List<Integer> weekDays = new ArrayList<>();
    public String startTime = "00:00";
    public String endTime = "00:00";

    public void validate() {
        try {
            if (weekDays == null || weekDays.isEmpty() || weekDays.size() > 7
                || weekDays.stream().anyMatch(day -> day == null || day < 1 || day > 7)) throw new IllegalArgumentException();
            if (!startTime.matches("\\d{2}:\\d{2}") || !endTime.matches("\\d{2}:\\d{2}")) throw new IllegalArgumentException();
            LocalTime.parse(startTime); LocalTime.parse(endTime);
        } catch (RuntimeException invalid) { throw new ServiceException("请选择星期和有效的录像时间段"); }
    }

    public boolean matches(LocalDateTime now) {
        if (!enabled) return false;
        validate();
        LocalTime start = LocalTime.parse(startTime), end = LocalTime.parse(endTime), time = now.toLocalTime();
        int today = now.getDayOfWeek().getValue();
        if (start.equals(end)) return weekDays.contains(today);
        if (start.isBefore(end)) return weekDays.contains(today) && !time.isBefore(start) && time.isBefore(end);
        return !time.isBefore(start) ? weekDays.contains(today) : time.isBefore(end) && weekDays.contains(today == 1 ? 7 : today - 1);
    }
}
