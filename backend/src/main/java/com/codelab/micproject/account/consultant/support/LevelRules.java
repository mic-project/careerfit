package com.codelab.micproject.account.consultant.support;

import com.codelab.micproject.account.consultant.domain.ConsultantLevel;

public final class LevelRules {
    private LevelRules() {}

    public static ConsultantLevel fromYears(int years) {
        if (years <= 5) return ConsultantLevel.JUNIOR;
        if (years <= 10) return ConsultantLevel.SENIOR;
        return ConsultantLevel.EXECUTIVE;
    }

    public static ConsultantLevel fromCareerStartYear(Integer startYear) {
        if (startYear == null) return ConsultantLevel.JUNIOR;
        int years = java.time.Year.now().getValue() - startYear;
        return fromYears(Math.max(0, years));
    }
}