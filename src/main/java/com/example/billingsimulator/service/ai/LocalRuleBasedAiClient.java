package com.example.billingsimulator.service.ai;

import com.example.billingsimulator.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based "AI" for local development. Uses regex to extract parameters
 * from common natural-language patterns.
 *
 * Will be replaced by GeminiAiClient on hackathon day (Vertex AI).
 *
 * Handles:
 *   - "Shift/move/switch X% from <service> to <service>"
 *   - "Increase/decrease volume by X% or X packages"
 *   - "Change weight to X lb/kg"
 *   - "Diesel increases X%" / "fuel surcharge goes to X%"
 *   - "Reduce residential by X%"
 *   - "Avoid/eliminate <surcharge>"
 */
@Component
public class LocalRuleBasedAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(LocalRuleBasedAiClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SERVICES_PATTERN =
            "ground|express|next day air|2nd day air|3 day select|surepost|standard|next day air saver";

    @Override
    public String generate(String systemPrompt, String userMessage) {
        log.info("Local rule-based extraction for: {}", userMessage);

        try {
            SimulationParameters params = new SimulationParameters();
            String input = userMessage.toLowerCase().trim();

            extractServiceShifts(input, params);
            extractVolumeChange(input, params);
            extractPackageProfile(input, params);
            extractFuelChange(input, params);
            extractDeliveryTypeChange(input, params);
            extractAccessorialChange(input, params);
            extractTimeframe(input, params);

            String json = objectMapper.writeValueAsString(params);
            log.info("Extracted: {}", json);
            return json;
        } catch (Exception e) {
            log.error("Extraction failed", e);
            return "{}";
        }
    }

    @Override
    public String clarify(String originalQuery, String validationErrors) {
        log.info("Local rule-based clarification (passthrough)");
        return null; // Fall back to hardcoded questions in validation service
    }

    @Override
    public String explain(String resultsJson) {
        log.info("Local rule-based explanation");
        try {
            JsonNode r = objectMapper.readTree(resultsJson);
            String scenario  = r.path("scenarioType").asText("simulation");
            double baseline  = r.path("baselineAnnualCost").asDouble(0);
            double projected = r.path("projectedAnnualCost").asDouble(0);
            double delta     = r.path("annualDelta").asDouble(0);
            double pct       = r.path("deltaPct").asDouble(0);
            String tierNote  = r.path("tierUpgradeNote").asText(null);

            String direction = delta < 0 ? "decrease" : "increase";
            String savingsWord = delta < 0 ? "saving" : "costing";

            StringBuilder sb = new StringBuilder();
            sb.append(String.format(
                "Based on the %s scenario, your projected annual shipping cost would %s from $%,.0f to $%,.0f, %s approximately $%,.0f (%.1f%%).",
                scenario.replace("_", " ").toLowerCase(), direction, baseline, projected, savingsWord, Math.abs(delta), Math.abs(pct)
            ));
            if (tierNote != null && !tierNote.isBlank()) {
                sb.append(" ").append(tierNote);
            }
            sb.append(" These results are projections based on historical shipping data and are not a final quote.");
            return sb.toString();
        } catch (Exception e) {
            log.error("Local explanation failed", e);
            return "Results are projections based on historical data and are not a final quote.";
        }
    }

    private void extractServiceShifts(String input, SimulationParameters params) {
        Pattern pattern = Pattern.compile(
                "(?:shift|move|switch|convert)\\s+(\\d+)\\s*%?" +
                "(?:\\s+of)?(?:\\s+my)?(?:\\s+shipments?)?(?:\\s+from)?\\s+" +
                "(" + SERVICES_PATTERN + ")\\s+" +
                "(?:to|into)\\s+" +
                "(" + SERVICES_PATTERN + ")"
        );
        Matcher m = pattern.matcher(input);
        List<ServiceShift> shifts = new ArrayList<>();

        while (m.find()) {
            shifts.add(new ServiceShift(
                    capitalize(m.group(2)),
                    capitalize(m.group(3)),
                    Double.parseDouble(m.group(1))
            ));
        }

        if (shifts.isEmpty()) {
            Pattern simplePattern = Pattern.compile(
                    "(?:shift|move|switch)\\s+(?:everything\\s+)?(?:to|into)\\s+(" + SERVICES_PATTERN + ")"
            );
            m = simplePattern.matcher(input);
            if (m.find()) {
                shifts.add(new ServiceShift(null, capitalize(m.group(1)), 100.0));
            }
        }

        if (!shifts.isEmpty()) {
            params.setServiceShifts(shifts);
        }
    }

    private void extractVolumeChange(String input, SimulationParameters params) {
        Pattern pctPattern = Pattern.compile(
                "(increase|decrease|reduce|double|triple)\\s+" +
                "(?:my\\s+)?(?:shipping\\s+)?(?:volume|shipments?)\\s+" +
                "(?:by\\s+)?(\\d+)\\s*%"
        );
        Matcher m = pctPattern.matcher(input);
        if (m.find()) {
            VolumeChange vc = new VolumeChange();
            vc.setChangeType("PERCENTAGE");
            double value = Double.parseDouble(m.group(2));
            if (m.group(1).matches("decrease|reduce")) value = -value;
            vc.setChangeValue(value);
            vc.setFrequency("MONTHLY");
            params.setVolumeChange(vc);
            return;
        }

        if (input.contains("double") && (input.contains("volume") || input.contains("shipping"))) {
            VolumeChange vc = new VolumeChange();
            vc.setChangeType("PERCENTAGE");
            vc.setChangeValue(100.0);
            vc.setFrequency("MONTHLY");
            params.setVolumeChange(vc);
            return;
        }

        Pattern absPattern = Pattern.compile(
                "(increase|add|decrease|reduce)\\s+(?:by\\s+)?(\\d+)\\s+(?:packages?|shipments?)"
        );
        m = absPattern.matcher(input);
        if (m.find()) {
            VolumeChange vc = new VolumeChange();
            vc.setChangeType("ABSOLUTE");
            double value = Double.parseDouble(m.group(2));
            if (m.group(1).matches("decrease|reduce")) value = -value;
            vc.setChangeValue(value);
            vc.setFrequency(extractFrequency(input));
            params.setVolumeChange(vc);
        }
    }

    private void extractPackageProfile(String input, SimulationParameters params) {
        Pattern weightPattern = Pattern.compile(
                "weight\\s+(?:increases?|changes?|goes?)\\s+(?:from\\s+\\d+\\s*(?:lb|kg)?\\s+)?(?:to\\s+)?" +
                "(\\d+(?:\\.\\d+)?)\\s*(lb|kg|lbs|pounds|kilos?)?"
        );
        Matcher m = weightPattern.matcher(input);
        if (m.find()) {
            PackageProfile pp = new PackageProfile();
            pp.setAverageWeight(Double.parseDouble(m.group(1)));
            String unit = m.group(2);
            pp.setWeightUnit(unit != null && (unit.startsWith("kg") || unit.startsWith("kilo")) ? "KG" : "LB");
            params.setPackageProfile(pp);
        }
    }

    private void extractFuelChange(String input, SimulationParameters params) {
        Pattern fuelPctPattern = Pattern.compile(
                "(?:diesel|fuel)\\s+(?:price\\s+)?(?:increases?|goes up|rises?)\\s+(?:by\\s+)?(\\d+)\\s*%"
        );
        Matcher m = fuelPctPattern.matcher(input);
        if (m.find()) {
            FuelChange fc = new FuelChange();
            fc.setFuelPriceChangePct(Double.parseDouble(m.group(1)));
            params.setFuelChange(fc);
            return;
        }

        Pattern surchargePctPattern = Pattern.compile(
                "(?:fuel\\s+)?surcharge\\s+(?:goes|set|hits|reaches?)\\s+(?:to\\s+)?(\\d+(?:\\.\\d+)?)\\s*%"
        );
        m = surchargePctPattern.matcher(input);
        if (m.find()) {
            FuelChange fc = new FuelChange();
            fc.setTargetFuelSurchargePct(Double.parseDouble(m.group(1)));
            params.setFuelChange(fc);
            return;
        }

        Pattern dieselPricePattern = Pattern.compile(
                "diesel\\s+(?:drops?|falls?|goes?)\\s+to\\s+\\$?(\\d+(?:\\.\\d+)?)"
        );
        m = dieselPricePattern.matcher(input);
        if (m.find()) {
            FuelChange fc = new FuelChange();
            fc.setTargetDieselPrice(Double.parseDouble(m.group(1)));
            params.setFuelChange(fc);
        }
    }

    private void extractDeliveryTypeChange(String input, SimulationParameters params) {
        Pattern resPctPattern = Pattern.compile(
                "(reduce|decrease|increase)\\s+residential\\s*(?:deliveries?)?\\s+(?:by\\s+)?(\\d+)\\s*%"
        );
        Matcher m = resPctPattern.matcher(input);
        if (m.find()) {
            DeliveryTypeChange dtc = new DeliveryTypeChange();
            double value = Double.parseDouble(m.group(2));
            if (m.group(1).matches("reduce|decrease")) value = -value;
            dtc.setResidentialChangePct(value);
            params.setDeliveryTypeChange(dtc);
            return;
        }

        Pattern resTargetPattern = Pattern.compile(
                "residential\\s*(?:mix|percentage|ratio)?\\s*(?:to|at|drops? to)\\s+(\\d+)\\s*%"
        );
        m = resTargetPattern.matcher(input);
        if (m.find()) {
            DeliveryTypeChange dtc = new DeliveryTypeChange();
            dtc.setTargetResidentialPct(Double.parseDouble(m.group(1)));
            params.setDeliveryTypeChange(dtc);
        }
    }

    private void extractAccessorialChange(String input, SimulationParameters params) {
        List<AccessorialChange> changes = new ArrayList<>();

        Map<String, String[]> surchargeKeywords = Map.of(
                "delivery area|das|remote area|extended area", new String[]{"DAS", "Delivery Area Surcharge"},
                "additional handling|oversized|overweight", new String[]{"AH", "Additional Handling"},
                "demand surcharge|peak", new String[]{"DS", "Demand Surcharge"},
                "saturday|weekend", new String[]{"SAT", "Saturday Delivery"},
                "declared value|insurance", new String[]{"DV", "Declared Value"},
                "large package", new String[]{"LPS", "Large Package Surcharge"}
        );

        for (Map.Entry<String, String[]> entry : surchargeKeywords.entrySet()) {
            Pattern p = Pattern.compile("(?:avoid|eliminate|stop|remove|reduce).*(?:" + entry.getKey() + ")");
            if (p.matcher(input).find()) {
                Double changePct = -100.0;

                Pattern reducePct = Pattern.compile("reduce.*(?:" + entry.getKey() + ").*?(\\d+)\\s*%");
                Matcher m = reducePct.matcher(input);
                if (m.find()) {
                    changePct = -Double.parseDouble(m.group(1));
                }

                changes.add(new AccessorialChange(entry.getValue()[0], entry.getValue()[1], changePct));
            }
        }

        if (!changes.isEmpty()) {
            params.setAccessorialChanges(changes);
        }
    }

    private void extractTimeframe(String input, SimulationParameters params) {
        if (input.contains("next quarter") || input.contains("quarterly")) {
            params.setTimeframePeriod("QUARTERLY");
        } else if (input.contains("next month") || input.contains("monthly")) {
            params.setTimeframePeriod("MONTHLY");
        } else if (input.contains("next week") || input.contains("weekly")) {
            params.setTimeframePeriod("WEEKLY");
        } else if (input.contains("annual") || input.contains("yearly") || input.contains("next year")) {
            params.setTimeframePeriod("ANNUALLY");
        }
    }

    private String extractFrequency(String input) {
        if (input.contains("per day") || input.contains("daily")) return "DAILY";
        if (input.contains("per week") || input.contains("weekly")) return "WEEKLY";
        return "MONTHLY";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return sb.toString();
    }
}
