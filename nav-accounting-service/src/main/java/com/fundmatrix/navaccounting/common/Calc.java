package com.fundmatrix.navaccounting.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money/unit arithmetic helpers with consistent scales and HALF_UP rounding. */
public final class Calc {

    public static final int UNIT_SCALE = 4;
    public static final int AMOUNT_SCALE = 2;
    public static final int RATE_SCALE = 4;
    public static final RoundingMode RM = RoundingMode.HALF_UP;

    private Calc() {
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public static BigDecimal units(BigDecimal v) {
        return v == null ? null : v.setScale(UNIT_SCALE, RM);
    }

    public static BigDecimal money(BigDecimal v) {
        return v == null ? null : v.setScale(AMOUNT_SCALE, RM);
    }

    public static BigDecimal rate(BigDecimal v) {
        return v == null ? null : v.setScale(RATE_SCALE, RM);
    }

    /** units = amount / nav, rounded to unit scale. */
    public static BigDecimal unitsFor(BigDecimal amount, BigDecimal nav) {
        if (amount == null || nav == null || nav.signum() == 0) {
            return BigDecimal.ZERO.setScale(UNIT_SCALE, RM);
        }
        return amount.divide(nav, UNIT_SCALE, RM);
    }

    /** amount = units * nav, rounded to money scale. */
    public static BigDecimal amountFor(BigDecimal units, BigDecimal nav) {
        if (units == null || nav == null) {
            return BigDecimal.ZERO.setScale(AMOUNT_SCALE, RM);
        }
        return units.multiply(nav).setScale(AMOUNT_SCALE, RM);
    }

    /** value * percent / 100, rounded to money scale. */
    public static BigDecimal percentOf(BigDecimal value, BigDecimal percent) {
        if (value == null || percent == null) {
            return BigDecimal.ZERO.setScale(AMOUNT_SCALE, RM);
        }
        return value.multiply(percent).divide(BigDecimal.valueOf(100), AMOUNT_SCALE, RM);
    }
}
