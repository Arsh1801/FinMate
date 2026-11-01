package com.finmate.common.util;

import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    public static final ZoneId KOLKATA_ZONE = ZoneId.of("Asia/Kolkata");
    public static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtils() {}

    public static LocalDate today() {
        return LocalDate.now(KOLKATA_ZONE);
    }

    public static java.time.LocalDateTime now() {
        return java.time.LocalDateTime.now(KOLKATA_ZONE);
    }

    public static boolean isWeekend(LocalDate date) {
        if (date == null) return false;
        java.time.DayOfWeek dow = date.getDayOfWeek();
        return dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY;
    }

    public static void validateTransactionDate(LocalDate date) {
        if (date == null) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Transaction date is required");
        }
        LocalDate now = today();
        if (date.isAfter(now)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Transaction date cannot be in the future");
        }
        if (date.isBefore(MIN_DATE)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Transaction date cannot be before 2000-01-01");
        }
    }
}
