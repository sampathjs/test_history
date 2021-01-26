package com.olf.jm.reportbuilder;

import java.time.*;
import java.time.temporal.*;
import java.sql.Timestamp;
import java.util.stream.*;

public class WorkingMinutesCalculator {
    private static final int WORK_HOUR_START = 9;
    private static final int WORK_HOUR_END = 17; //17 means 17:59:59
    private static final long MINUTES = 60;

    private static final long WORKING_HOURS_PER_DAY = WORK_HOUR_END - WORK_HOUR_START;
    private static final long WORKING_MINUTES_PER_DAY = WORKING_HOURS_PER_DAY * MINUTES;

    public static int getWorkingMinutesSince(final Timestamp startTime) {
        Timestamp now = Timestamp.from(Instant.now());
        return getWorkingMinutes(startTime, now);
    }

    public static int getWorkingMinutes(final Timestamp startTime, final Timestamp endTime) {
        if (null == startTime || null == endTime) {
            throw new IllegalStateException();
        }
        if (endTime.before(startTime)) {
            return 0;
        }

        LocalDateTime from = startTime.toLocalDateTime();
        LocalDateTime to = endTime.toLocalDateTime();

        LocalDate fromDay = from.toLocalDate();
        LocalDate toDay = to.toLocalDate();

        int allDaysBetween = (int) (ChronoUnit.DAYS.between(fromDay, toDay) + 1);
        long allWorkingMinutes = IntStream.range(0, allDaysBetween)
                .filter(i -> isWorkingDay(from.plusDays(i)))
                .count() * WORKING_MINUTES_PER_DAY ;

        // from - working_day_from_start
        long tailRedundantMinutes = 0;
        if (isWorkingDay(from)) {
            if (isWorkingHours(from)) {
                tailRedundantMinutes = Duration.between(fromDay.atTime(WORK_HOUR_START, 0), from).toMinutes();
            } else if (from.getHour() > WORK_HOUR_START) {
                tailRedundantMinutes = WORKING_MINUTES_PER_DAY;
            }
        }

        // working_day_end - to
        long headRedundanMinutes = 0;
        if (isWorkingDay(to)) {
            if (isWorkingHours(to)) {
                headRedundanMinutes = Duration.between(to, toDay.atTime(WORK_HOUR_END, 0)).toMinutes();
            } else if (to.getHour() < WORK_HOUR_START) {
                headRedundanMinutes = WORKING_MINUTES_PER_DAY;
            }
        }
        return (int) (allWorkingMinutes - Math.min(tailRedundantMinutes, WORKING_MINUTES_PER_DAY) - Math.min(headRedundanMinutes, WORKING_MINUTES_PER_DAY));
    }

    private static boolean isWorkingDay(final LocalDateTime time) {
        return time.getDayOfWeek().getValue() < DayOfWeek.SATURDAY.getValue();
    }

    private static boolean isWorkingHours(final LocalDateTime time) {
        int hour = time.getHour();
        return WORK_HOUR_START <= hour && hour <= WORK_HOUR_END;
    }
}