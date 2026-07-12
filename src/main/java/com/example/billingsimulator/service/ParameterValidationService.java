package com.example.billingsimulator.service;

import com.example.billingsimulator.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Deterministic business-rule validation for extracted simulation parameters.
 * No AI — just if/else checks, range validations, and null checks.
 *
 * Returns an empty list if parameters are valid.
 * Returns clarification questions if anything is missing or ambiguous.
 */
@Service
public class ParameterValidationService {

    private static final Logger log = LoggerFactory.getLogger(ParameterValidationService.class);

    // Known UPS service levels
    private static final Set<String> VALID_SERVICES = Set.of(
            "Ground", "Express", "Next Day Air", "2nd Day Air", "3 Day Select",
            "SurePost", "Standard", "Expedited", "Next Day Air Saver",
            "Next Day Air Early", "2nd Day Air AM"
    );

    // Known accessorial surcharge codes
    private static final Set<String> VALID_SURCHARGE_CODES = Set.of(
            "DAS", "AH", "DS", "SAT", "DV", "LPS", "PAF", "RES"
    );

    private static final Set<String> VALID_CHANGE_TYPES = Set.of("ABSOLUTE", "PERCENTAGE");
    private static final Set<String> VALID_FREQUENCIES = Set.of("DAILY", "WEEKLY", "MONTHLY");
    private static final Set<String> VALID_WEIGHT_UNITS = Set.of("LB", "KG");
    private static final Set<String> VALID_DIMENSION_UNITS = Set.of("IN", "CM");
    private static final Set<String> VALID_TIMEFRAMES = Set.of("WEEKLY", "MONTHLY", "QUARTERLY", "ANNUALLY");

    /**
     * Main entry point. Validates the full SimulationParameters object.
     * Returns empty list if everything is valid.
     */
    public List<ClarificationQuestion> validate(SimulationParameters parameters) {
        List<ClarificationQuestion> questions = new ArrayList<>();

        // Rule 1: parameters object itself must not be null
        if (parameters == null) {
            questions.add(new ClarificationQuestion(
                    "query",
                    "I couldn't understand your request. Could you describe the shipping change you'd like to simulate?",
                    List.of(
                            "Shift shipments between service levels",
                            "Change shipping volume",
                            "Change package weight or dimensions",
                            "Explore fuel surcharge impact",
                            "Change residential vs commercial mix"
                    )
            ));
            return questions;
        }

        // Rule 2: at least one scenario dimension must be present
        if (!parameters.hasAnyScenario()) {
            questions.add(new ClarificationQuestion(
                    "scenario",
                    "What type of change would you like to simulate?",
                    List.of(
                            "Shift shipments between service levels (e.g., Air to Ground)",
                            "Change shipping volume (e.g., increase by 20%)",
                            "Change package weight or dimensions",
                            "Explore fuel surcharge impact",
                            "Change residential vs commercial delivery mix",
                            "Reduce specific surcharges (DAS, Additional Handling, etc.)"
                    )
            ));
            return questions;
        }

        // Validate each non-null scenario dimension
        if (parameters.getServiceShifts() != null && !parameters.getServiceShifts().isEmpty()) {
            questions.addAll(validateServiceShifts(parameters.getServiceShifts()));
        }

        if (questions.isEmpty() && parameters.getVolumeChange() != null) {
            questions.addAll(validateVolumeChange(parameters.getVolumeChange()));
        }

        if (questions.isEmpty() && parameters.getPackageProfile() != null) {
            questions.addAll(validatePackageProfile(parameters.getPackageProfile()));
        }

        if (questions.isEmpty() && parameters.getFuelChange() != null) {
            questions.addAll(validateFuelChange(parameters.getFuelChange()));
        }

        if (questions.isEmpty() && parameters.getDeliveryTypeChange() != null) {
            questions.addAll(validateDeliveryTypeChange(parameters.getDeliveryTypeChange()));
        }

        if (questions.isEmpty() && parameters.getAccessorialChanges() != null && !parameters.getAccessorialChanges().isEmpty()) {
            questions.addAll(validateAccessorialChanges(parameters.getAccessorialChanges()));
        }

        if (questions.isEmpty() && parameters.getTimeframePeriod() != null && !VALID_TIMEFRAMES.contains(parameters.getTimeframePeriod())) {
            questions.add(new ClarificationQuestion(
                    "timeframePeriod",
                    "What time period should this simulation cover?",
                    List.of("Weekly", "Monthly", "Quarterly", "Annually")
            ));
        }

        log.info("Validation complete: {} clarification question(s)", questions.size());
        return questions;
    }

    /**
     * Convenience method — true if no clarification needed.
     */
    public boolean isValid(SimulationParameters parameters) {
        return validate(parameters).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Service Shift Validation
    // ──────────────────────────────────────────────────────────────────────

    private List<ClarificationQuestion> validateServiceShifts(List<ServiceShift> shifts) {
        List<ClarificationQuestion> questions = new ArrayList<>();
        Map<String, Double> totalBySource = new HashMap<>();

        for (ServiceShift shift : shifts) {
            // fromService must be present and valid
            if (shift.getFromService() == null || shift.getFromService().isBlank()) {
                questions.add(new ClarificationQuestion(
                        "fromService",
                        "Which service level are you shifting shipments FROM?",
                        List.copyOf(VALID_SERVICES)
                ));
            } else if (!isValidService(shift.getFromService())) {
                questions.add(new ClarificationQuestion(
                        "fromService",
                        "'" + shift.getFromService() + "' is not a recognized service level. Which did you mean?",
                        List.copyOf(VALID_SERVICES)
                ));
            }

            // toService must be present and valid
            if (shift.getToService() == null || shift.getToService().isBlank()) {
                questions.add(new ClarificationQuestion(
                        "toService",
                        "Which service level are you shifting shipments TO?",
                        List.copyOf(VALID_SERVICES)
                ));
            } else if (!isValidService(shift.getToService())) {
                questions.add(new ClarificationQuestion(
                        "toService",
                        "'" + shift.getToService() + "' is not a recognized service level. Which did you mean?",
                        List.copyOf(VALID_SERVICES)
                ));
            }

            // from and to can't be the same
            if (shift.getFromService() != null && shift.getToService() != null
                    && shift.getFromService().equalsIgnoreCase(shift.getToService())) {
                questions.add(new ClarificationQuestion(
                        "serviceShift",
                        "Source and destination are the same ('" + shift.getFromService() + "'). Did you mean a different service?",
                        List.copyOf(VALID_SERVICES)
                ));
            }

            // percentage must be present and in range 1-100
            if (shift.getPercentage() == null) {
                questions.add(new ClarificationQuestion(
                        "percentage",
                        "What percentage of shipments would you like to shift?",
                        List.of("10%", "20%", "30%", "50%", "100%")
                ));
            } else if (shift.getPercentage() <= 0 || shift.getPercentage() > 100) {
                questions.add(new ClarificationQuestion(
                        "percentage",
                        "Percentage must be between 1% and 100%. You specified " + shift.getPercentage() + "%. What did you mean?",
                        List.of("10%", "20%", "50%", "100%")
                ));
            }

            // Track total percentage per source service
            if (shift.getFromService() != null && shift.getPercentage() != null) {
                totalBySource.merge(shift.getFromService().toLowerCase(), shift.getPercentage(), Double::sum);
            }
        }

        // Total shifts from any single source can't exceed 100%
        for (Map.Entry<String, Double> entry : totalBySource.entrySet()) {
            if (entry.getValue() > 100) {
                questions.add(new ClarificationQuestion(
                        "totalPercentage",
                        "Total shift from '" + entry.getKey() + "' adds up to " + entry.getValue() + "%, which exceeds 100%. Please adjust.",
                        List.of()
                ));
            }
        }

        return questions;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Volume Change Validation
    // ──────────────────────────────────────────────────────────────────────

    private List<ClarificationQuestion> validateVolumeChange(VolumeChange vc) {
        List<ClarificationQuestion> questions = new ArrayList<>();

        if (vc.getChangeType() == null || !VALID_CHANGE_TYPES.contains(vc.getChangeType())) {
            questions.add(new ClarificationQuestion(
                    "changeType",
                    "Is this an absolute number or a percentage change?",
                    List.of("Absolute (e.g., 500 more packages)", "Percentage (e.g., 20% increase)")
            ));
        }

        if (vc.getChangeValue() == null || vc.getChangeValue() == 0) {
            questions.add(new ClarificationQuestion(
                    "changeValue",
                    "By how much would you like to change the volume?",
                    List.of()
            ));
        }

        // Sanity check: percentage shouldn't be absurd
        if ("PERCENTAGE".equals(vc.getChangeType()) && vc.getChangeValue() != null) {
            if (Math.abs(vc.getChangeValue()) > 500) {
                questions.add(new ClarificationQuestion(
                        "changeValue",
                        "A " + vc.getChangeValue() + "% change seems very large. Did you mean " + vc.getChangeValue().intValue() + " packages instead?",
                        List.of(vc.getChangeValue() + "%", vc.getChangeValue().intValue() + " packages")
                ));
            }
        }

        if (vc.getFrequency() != null && !VALID_FREQUENCIES.contains(vc.getFrequency())) {
            questions.add(new ClarificationQuestion(
                    "frequency",
                    "What time period does this volume change apply to?",
                    List.of("Daily", "Weekly", "Monthly")
            ));
        }

        // If service level is specified, it must be valid
        if (vc.getServiceLevel() != null && !isValidService(vc.getServiceLevel())) {
            questions.add(new ClarificationQuestion(
                    "serviceLevel",
                    "'" + vc.getServiceLevel() + "' is not a recognized service level.",
                    List.copyOf(VALID_SERVICES)
            ));
        }

        return questions;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Package Profile Validation
    // ──────────────────────────────────────────────────────────────────────

    private List<ClarificationQuestion> validatePackageProfile(PackageProfile pp) {
        List<ClarificationQuestion> questions = new ArrayList<>();

        // Weight validation
        if (pp.getAverageWeight() != null) {
            String unit = pp.getWeightUnit() != null ? pp.getWeightUnit() : "LB";
            double maxWeight = "KG".equals(unit) ? 70.0 : 150.0;

            if (pp.getAverageWeight() <= 0) {
                questions.add(new ClarificationQuestion(
                        "averageWeight",
                        "Package weight must be greater than zero.",
                        List.of()
                ));
            } else if (pp.getAverageWeight() > maxWeight) {
                questions.add(new ClarificationQuestion(
                        "averageWeight",
                        "Weight of " + pp.getAverageWeight() + " " + unit + " exceeds UPS maximum (" + maxWeight + " " + unit + "). Did you mean a different weight?",
                        List.of()
                ));
            }
        }

        if (pp.getWeightUnit() != null && !VALID_WEIGHT_UNITS.contains(pp.getWeightUnit())) {
            questions.add(new ClarificationQuestion(
                    "weightUnit",
                    "Which weight unit: pounds (LB) or kilograms (KG)?",
                    List.of("LB", "KG")
            ));
        }

        // Dimension validation — if any dimension is given, all three must be
        boolean hasLength = pp.getAverageLength() != null;
        boolean hasWidth = pp.getAverageWidth() != null;
        boolean hasHeight = pp.getAverageHeight() != null;

        if (hasLength || hasWidth || hasHeight) {
            if (!(hasLength && hasWidth && hasHeight)) {
                questions.add(new ClarificationQuestion(
                        "dimensions",
                        "Please provide all three dimensions (length, width, height).",
                        List.of()
                ));
            } else {
                String dUnit = pp.getDimensionUnit() != null ? pp.getDimensionUnit() : "IN";
                double maxDim = "CM".equals(dUnit) ? 274.0 : 108.0;

                if (pp.getAverageLength() <= 0 || pp.getAverageWidth() <= 0 || pp.getAverageHeight() <= 0) {
                    questions.add(new ClarificationQuestion(
                            "dimensions",
                            "All dimensions must be greater than zero.",
                            List.of()
                    ));
                } else if (pp.getAverageLength() > maxDim || pp.getAverageWidth() > maxDim || pp.getAverageHeight() > maxDim) {
                    questions.add(new ClarificationQuestion(
                            "dimensions",
                            "One or more dimensions exceed UPS maximum (" + maxDim + " " + dUnit + ").",
                            List.of()
                    ));
                }
            }
        }

        if (pp.getDimensionUnit() != null && !VALID_DIMENSION_UNITS.contains(pp.getDimensionUnit())) {
            questions.add(new ClarificationQuestion(
                    "dimensionUnit",
                    "Which dimension unit: inches (IN) or centimeters (CM)?",
                    List.of("IN", "CM")
            ));
        }

        return questions;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Fuel Change Validation
    // ──────────────────────────────────────────────────────────────────────

    private List<ClarificationQuestion> validateFuelChange(FuelChange fc) {
        List<ClarificationQuestion> questions = new ArrayList<>();

        // At least one field must be set
        boolean hasPriceChange = fc.getFuelPriceChangePct() != null;
        boolean hasSurchargePct = fc.getTargetFuelSurchargePct() != null;
        boolean hasDieselPrice = fc.getTargetDieselPrice() != null;

        if (!hasPriceChange && !hasSurchargePct && !hasDieselPrice) {
            questions.add(new ClarificationQuestion(
                    "fuelChange",
                    "How should fuel costs change? You can specify a diesel price change %, a target surcharge %, or a target diesel price.",
                    List.of("Diesel increases 10%", "Fuel surcharge goes to 16%", "Diesel drops to $4.50/gallon")
            ));
            return questions;
        }

        // Only one should be set
        long fieldsSet = (hasPriceChange ? 1 : 0) + (hasSurchargePct ? 1 : 0) + (hasDieselPrice ? 1 : 0);
        if (fieldsSet > 1) {
            questions.add(new ClarificationQuestion(
                    "fuelChange",
                    "Please specify only one fuel change: a price change %, a target surcharge %, or a target diesel price.",
                    List.of("Diesel price change %", "Target fuel surcharge %", "Target diesel price per gallon")
            ));
            return questions;
        }

        // Range checks
        if (hasPriceChange) {
            if (fc.getFuelPriceChangePct() < -50 || fc.getFuelPriceChangePct() > 200) {
                questions.add(new ClarificationQuestion(
                        "fuelPriceChangePct",
                        "A " + fc.getFuelPriceChangePct() + "% diesel price change seems unusual. Typical range is -50% to +200%.",
                        List.of()
                ));
            }
        }

        if (hasSurchargePct) {
            if (fc.getTargetFuelSurchargePct() < 0 || fc.getTargetFuelSurchargePct() > 40) {
                questions.add(new ClarificationQuestion(
                        "targetFuelSurchargePct",
                        "Fuel surcharge of " + fc.getTargetFuelSurchargePct() + "% is outside historical range (0-40%). Did you mean something else?",
                        List.of()
                ));
            }
        }

        if (hasDieselPrice) {
            if (fc.getTargetDieselPrice() < 1.0 || fc.getTargetDieselPrice() > 15.0) {
                questions.add(new ClarificationQuestion(
                        "targetDieselPrice",
                        "$" + fc.getTargetDieselPrice() + "/gallon is outside realistic range ($1.00-$15.00). Did you mean a different price?",
                        List.of()
                ));
            }
        }

        return questions;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Delivery Type Change Validation
    // ──────────────────────────────────────────────────────────────────────

    private List<ClarificationQuestion> validateDeliveryTypeChange(DeliveryTypeChange dtc) {
        List<ClarificationQuestion> questions = new ArrayList<>();

        boolean hasChangePct = dtc.getResidentialChangePct() != null;
        boolean hasTargetPct = dtc.getTargetResidentialPct() != null;

        // At least one must be set
        if (!hasChangePct && !hasTargetPct) {
            questions.add(new ClarificationQuestion(
                    "deliveryTypeChange",
                    "How should the residential/commercial mix change?",
                    List.of("Reduce residential by 20%", "Set residential to 40% of deliveries")
            ));
            return questions;
        }

        // Only one should be set
        if (hasChangePct && hasTargetPct) {
            questions.add(new ClarificationQuestion(
                    "deliveryTypeChange",
                    "Did you mean to reduce residential by " + dtc.getResidentialChangePct() + "% or set it to " + dtc.getTargetResidentialPct() + "%?",
                    List.of("Reduce by " + dtc.getResidentialChangePct() + "%", "Set to " + dtc.getTargetResidentialPct() + "%")
            ));
            return questions;
        }

        // Range checks
        if (hasChangePct && (dtc.getResidentialChangePct() < -100 || dtc.getResidentialChangePct() > 100)) {
            questions.add(new ClarificationQuestion(
                    "residentialChangePct",
                    "Residential change must be between -100% and +100%.",
                    List.of()
            ));
        }

        if (hasTargetPct && (dtc.getTargetResidentialPct() < 0 || dtc.getTargetResidentialPct() > 100)) {
            questions.add(new ClarificationQuestion(
                    "targetResidentialPct",
                    "Target residential percentage must be between 0% and 100%.",
                    List.of()
            ));
        }

        return questions;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Accessorial Change Validation
    // ──────────────────────────────────────────────────────────────────────

    private List<ClarificationQuestion> validateAccessorialChanges(List<AccessorialChange> changes) {
        List<ClarificationQuestion> questions = new ArrayList<>();

        for (AccessorialChange ac : changes) {
            // Must have code or name
            if ((ac.getSurchargeCode() == null || ac.getSurchargeCode().isBlank())
                    && (ac.getSurchargeName() == null || ac.getSurchargeName().isBlank())) {
                questions.add(new ClarificationQuestion(
                        "surchargeCode",
                        "Which surcharge are you asking about?",
                        List.of("Delivery Area Surcharge (DAS)", "Additional Handling", "Demand Surcharge",
                                "Saturday Delivery", "Declared Value", "Large Package Surcharge")
                ));
            }

            // If code is provided, must be valid
            if (ac.getSurchargeCode() != null && !ac.getSurchargeCode().isBlank()
                    && !VALID_SURCHARGE_CODES.contains(ac.getSurchargeCode())) {
                questions.add(new ClarificationQuestion(
                        "surchargeCode",
                        "'" + ac.getSurchargeCode() + "' is not a recognized surcharge code.",
                        List.of("DAS", "AH", "DS", "SAT", "DV", "LPS", "PAF")
                ));
            }

            // changePct must be present
            if (ac.getChangePct() == null) {
                questions.add(new ClarificationQuestion(
                        "changePct",
                        "Do you want to eliminate this surcharge or reduce it by a percentage?",
                        List.of("Eliminate completely (-100%)", "Reduce by 50%", "Reduce by 25%")
                ));
            } else if (ac.getChangePct() < -100 || ac.getChangePct() > 500) {
                questions.add(new ClarificationQuestion(
                        "changePct",
                        "Surcharge change of " + ac.getChangePct() + "% is outside valid range (-100% to +500%).",
                        List.of()
                ));
            }
        }

        return questions;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────────────

    private boolean isValidService(String service) {
        return VALID_SERVICES.stream().anyMatch(s -> s.equalsIgnoreCase(service));
    }
}
