package com.finmate.common.util;

import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyUtils {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");
    public static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private MoneyUtils() {}

    public static BigDecimal validateAndScale(BigDecimal amount) {
        if (amount == null) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Amount is required");
        }
        if (amount.scale() > 2) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Amount cannot have more than 2 decimal places");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Amount must be strictly positive");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Amount exceeds maximum allowable limit of 99,999,999.99");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static long toPaise(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return 0L;
        }
        return amount.setScale(2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    public static BigDecimal fromPaise(long paise) {
        return BigDecimal.valueOf(paise).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal scale(BigDecimal amount) {
        if (amount == null) return ZERO;
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculatePercentage(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    public static String formatIndianCurrency(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        boolean isNegative = scaled.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal abs = scaled.abs();

        String str = abs.toPlainString();
        String[] parts = str.split("\\.");
        String integerPart = parts[0];
        String fractionPart = parts.length > 1 ? parts[1] : "00";

        StringBuilder formatted = new StringBuilder();
        int len = integerPart.length();
        if (len <= 3) {
            formatted.append(integerPart);
        } else {
            // Last 3 digits
            String lastThree = integerPart.substring(len - 3);
            String remaining = integerPart.substring(0, len - 3);

            StringBuilder remFormatted = new StringBuilder();
            int remLen = remaining.length();
            int count = 0;
            for (int i = remLen - 1; i >= 0; i--) {
                remFormatted.insert(0, remaining.charAt(i));
                count++;
                if (count % 2 == 0 && i > 0) {
                    remFormatted.insert(0, ",");
                }
            }
            formatted.append(remFormatted).append(",").append(lastThree);
        }

        formatted.append(".").append(fractionPart);
        return (isNegative ? "-₹" : "₹") + formatted.toString();
    }

    public static String formatIndian(BigDecimal amount) {
        return formatIndianCurrency(amount);
    }
}
