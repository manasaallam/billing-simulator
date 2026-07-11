package com.example.billingsimulator.util;

import java.util.Map;

public final class ServiceType {

    private ServiceType() {
    }

    private static final Map<String, String> SERVICE_MAPPING = Map.ofEntries(

            Map.entry("GROUND", "GROUND"),
            Map.entry("UPS GROUND", "GROUND"),
            Map.entry("GROUND SERVICE", "GROUND"),

            Map.entry("AIR", "AIR"),
            Map.entry("UPS AIR", "AIR"),

            Map.entry("NEXT DAY AIR", "NEXT_DAY_AIR"),
            Map.entry("NEXTDAY AIR", "NEXT_DAY_AIR"),
            Map.entry("UPS NEXT DAY AIR", "NEXT_DAY_AIR"),

            Map.entry("2ND DAY AIR", "SECOND_DAY_AIR"),
            Map.entry("SECOND DAY AIR", "SECOND_DAY_AIR"),
            Map.entry("UPS SECOND DAY AIR", "SECOND_DAY_AIR"),

            Map.entry("3 DAY SELECT", "THREE_DAY_SELECT"),
            Map.entry("THREE DAY SELECT", "THREE_DAY_SELECT"),
            Map.entry("UPS 3 DAY SELECT", "THREE_DAY_SELECT"),

            Map.entry("EXPRESS SAVER", "EXPRESS_SAVER"),
            Map.entry("UPS EXPRESS SAVER", "EXPRESS_SAVER"),

            Map.entry("WORLDWIDE EXPRESS", "WORLDWIDE_EXPRESS"),
            Map.entry("UPS WORLDWIDE EXPRESS", "WORLDWIDE_EXPRESS"),

            Map.entry("WORLDWIDE EXPEDITED", "WORLDWIDE_EXPEDITED"),
            Map.entry("UPS WORLDWIDE EXPEDITED", "WORLDWIDE_EXPEDITED"),

            Map.entry("STANDARD", "STANDARD"),
            Map.entry("UPS STANDARD", "STANDARD"),

            Map.entry("FREIGHT", "FREIGHT"),
            Map.entry("UPS FREIGHT", "FREIGHT")
    );

    public static String normalize(String service) {

        if (service == null || service.isBlank()) {
            return null;
        }

        return SERVICE_MAPPING.getOrDefault(
                service.trim().toUpperCase(),
                service.trim().toUpperCase().replace(' ', '_')
        );
    }
}
