package com.libraflow.model;

import java.io.Serializable;

/**
 * Standard book categories with shelf prefixes and default loan periods.
 */
public enum BookCategory implements Serializable {
    COMPUTER_SCIENCE("Computer Science & AI", "CS-100", 14),
    SOFTWARE_ENGINEERING("Software Engineering", "SE-200", 14),
    DATA_SCIENCE("Data Science & Analytics", "DS-300", 14),
    MATHEMATICS("Mathematics & Logic", "MT-400", 21),
    FICTION("Fiction & Literature", "FC-500", 14),
    BUSINESS("Business & Economics", "BZ-600", 14),
    SCIENCE("Physics & Natural Science", "SC-700", 14),
    HISTORY("History & Philosophy", "HS-800", 21);

    private final String displayName;
    private final String shelfPrefix;
    private final int defaultLoanDays;

    BookCategory(String displayName, String shelfPrefix, int defaultLoanDays) {
        this.displayName = displayName;
        this.shelfPrefix = shelfPrefix;
        this.defaultLoanDays = defaultLoanDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShelfPrefix() {
        return shelfPrefix;
    }

    public int getDefaultLoanDays() {
        return defaultLoanDays;
    }
}
