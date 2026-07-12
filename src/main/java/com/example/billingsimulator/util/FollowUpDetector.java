package com.example.billingsimulator.util;

import java.util.Set;
import java.util.regex.Pattern;

public final class FollowUpDetector {

    private FollowUpDetector() {
    }

    private static final Pattern PERCENTAGE =
            Pattern.compile("^\\d+(\\.\\d+)?\\s*%$");

    private static final Pattern WEIGHT =
            Pattern.compile("^\\d+(\\.\\d+)?\\s*(lb|lbs|kg|kgs)$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern NUMBER =
            Pattern.compile("^\\d+(\\.\\d+)?$");

    private static final Pattern PACKAGE_COUNT =
            Pattern.compile("^\\d+(\\.\\d+)?\\s*(packages|shipments)$",
                    Pattern.CASE_INSENSITIVE);

    private static final Set<String> SERVICES = Set.of(
            "GROUND",
            "AIR",
            "NEXT DAY AIR",
            "2ND DAY AIR",
            "SECOND DAY AIR",
            "3 DAY SELECT",
            "EXPRESS",
            "EXPRESS SAVER",
            "WORLDWIDE EXPRESS",
            "WORLDWIDE EXPEDITED",
            "STANDARD",
            "FREIGHT"
    );

    private static final Set<String> PERIODS = Set.of(
            "NEXT MONTH",
            "NEXT WEEK",
            "Q1",
            "Q2",
            "Q3",
            "Q4",
            "HOLIDAY",
            "HOLIDAY SEASON",
            "SUMMER",
            "WINTER",
            "SPRING",
            "FALL"
    );

    public static boolean isFollowUpAnswer(String text) {

        if (text == null || text.isBlank()) {
            return false;
        }

        String value = text.trim().toUpperCase();

        return PERCENTAGE.matcher(value).matches()
                || WEIGHT.matcher(value).matches()
                || NUMBER.matcher(value).matches()
                || PACKAGE_COUNT.matcher(value).matches()
                || SERVICES.contains(value)
                || PERIODS.contains(value);
    }
}
