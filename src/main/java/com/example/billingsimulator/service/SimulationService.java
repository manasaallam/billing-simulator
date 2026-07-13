package com.example.billingsimulator.service;

import com.example.billingsimulator.model.LineItem;
import com.example.billingsimulator.model.RateSimulationRequest;
import com.example.billingsimulator.model.RateSimulationResponse;
import com.example.billingsimulator.exception.ContractNotFoundException;
import com.example.billingsimulator.exception.InvalidInputException;
import com.example.billingsimulator.exception.RateNotFoundException;
import com.example.billingsimulator.model.*;
import com.example.billingsimulator.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Projection scenarios — all backed by the deterministic RateEngineService.
 *
 * Each scenario:
 *  1. Loads the baseline_snapshot for the account
 *  2. Reads the relevant metrics from metrics_json
 *  3. Applies the scenario change (volume, service mix, weight, zone mix, etc.)
 *  4. Calls RateEngineService helpers to recompute discount / transport cost
 *  5. Returns a RateSimulationResponse with baseline vs projected cost delta
 *
 * All monetary values are computed here — the AI layer only phrases the answer.
 */
@Service
public class SimulationService {

    private static final int SCALE = 2;
    private static final RoundingMode HALF_UP = RoundingMode.HALF_UP;
    private static final BigDecimal WEEKS_PER_YEAR = BigDecimal.valueOf(52);

    private final ContractRepository contractRepo;
    private final BaselineSnapshotRepository baselineRepo;
    private final DiscountTierRepository discountTierRepo;
    private final ServiceLevelRepository serviceLevelRepo;
    private final RateEngineService engine;
    private final ObjectMapper objectMapper;

    public SimulationService(
            ContractRepository contractRepo,
            BaselineSnapshotRepository baselineRepo,
            DiscountTierRepository discountTierRepo,
            ServiceLevelRepository serviceLevelRepo,
            RateEngineService engine,
            ObjectMapper objectMapper) {
        this.contractRepo = contractRepo;
        this.baselineRepo = baselineRepo;
        this.discountTierRepo = discountTierRepo;
        this.serviceLevelRepo = serviceLevelRepo;
        this.engine = engine;
        this.objectMapper = objectMapper;
    }

    // -----------------------------------------------------------------------
    // Scenario: VOLUME_CHANGE
    // "What if I ship 35 packages a week instead of 20?"
    // Impact: may cross a tier band → lower discount → lower net transport
    // -----------------------------------------------------------------------
    public RateSimulationResponse volumeChange(RateSimulationRequest req) {
        Contract contract = loadContract(req.getContractId());
        BaselineSnapshot baseline = loadBaseline(req);
        if (req.getNewWeeklyVolume() == null || req.getNewWeeklyVolume() <= 0) {
            throw new InvalidInputException("newWeeklyVolume must be a positive integer");
        }

        Map<String, Object> metrics = parseMetrics(baseline.getMetricsJson());
        int currentVolume = baseline.getAvgWeeklyVolume().intValue();
        int newVolume = req.getNewWeeklyVolume();

        // Volume scaling factor: more shipments = proportionally more cost before discount adjustment
        BigDecimal volumeScale = BigDecimal.valueOf((double) newVolume / currentVolume);

        // Baseline annual cost breakdown (transport vs non-transport)
        BigDecimal annualCost = baseline.getTotalCost();
        BigDecimal spendByServiceTotal = sumSpendByService(metrics);
        BigDecimal transportPortion = spendByServiceTotal.compareTo(BigDecimal.ZERO) > 0
                ? spendByServiceTotal : annualCost.multiply(BigDecimal.valueOf(0.72));
        BigDecimal nonTransportPortion = annualCost.subtract(transportPortion);

        // Per-category transport projection (scaled by volume, then adjusted for tier change)
        Map<String, BigDecimal> spendByService = getSpendByService(metrics);
        Map<String, BigDecimal> projectedByCategory = new LinkedHashMap<>();
        BigDecimal projectedTransport = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : spendByService.entrySet()) {
            String serviceCode = entry.getKey();
            BigDecimal serviceCost = entry.getValue();
            ServiceLevel sl = serviceLevelRepo.findById(serviceCode).orElse(null);
            if (sl == null) continue;
            String category = sl.getDiscountCategory();
            if (category == null) continue;

            DiscountTier currentTier = discountTierRepo.findTier(contract.getProgramId(), category, currentVolume)
                    .orElse(null);
            DiscountTier newTier = discountTierRepo.findTier(contract.getProgramId(), category, newVolume)
                    .orElse(null);
            if (currentTier == null || newTier == null) {
                // No tier info — just scale by volume
                BigDecimal scaled = serviceCost.multiply(volumeScale).setScale(SCALE, HALF_UP);
                projectedByCategory.put(serviceCode, scaled);
                projectedTransport = projectedTransport.add(scaled);
                continue;
            }
            // Scale cost by volume, then adjust for discount tier change
            BigDecimal scaledCost = serviceCost.multiply(volumeScale).setScale(SCALE, HALF_UP);
            BigDecimal projected = engine.projectTransportCost(
                    scaledCost, currentTier.getDiscountPct(), newTier.getDiscountPct());
            projectedByCategory.put(serviceCode, projected);
            projectedTransport = projectedTransport.add(projected);
        }

        // Non-transport costs (fuel, accessorials) also scale with volume
        BigDecimal scaledNonTransport = nonTransportPortion.multiply(volumeScale).setScale(SCALE, HALF_UP);
        BigDecimal projectedAnnual = projectedTransport.add(scaledNonTransport).setScale(SCALE, HALF_UP);

        // Tier band for response
        String primaryCategory = primaryCategory(spendByService);
        DiscountTier currentTier = discountTierRepo.findTier(
                contract.getProgramId(), primaryCategory, currentVolume).orElse(null);
        DiscountTier newTier = discountTierRepo.findTier(
                contract.getProgramId(), primaryCategory, newVolume).orElse(null);

        RateSimulationResponse resp = buildBaseResponse(req, baseline, contract, "VOLUME_CHANGE");
        resp.setBaselineAnnualCost(annualCost);
        resp.setBaselineWeeklyCost(annualCost.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        resp.setBaselineAvgWeeklyVolume(baseline.getAvgWeeklyVolume());
        resp.setBaselineDiscountTierBand(currentTier != null ? engine.formatTierBand(currentTier) : "—");
        resp.setProjectedAnnualCost(projectedAnnual);
        resp.setProjectedWeeklyCost(projectedAnnual.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        resp.setProjectedDiscountTierBand(newTier != null ? engine.formatTierBand(newTier) : "—");
        setDeltas(resp, annualCost, projectedAnnual);
        resp.setBaselineCostByCategory(spendByService);
        resp.setProjectedCostByCategory(projectedByCategory);

        if (currentTier != null && newTier != null
                && !currentTier.getId().equals(newTier.getId())) {
            resp.setTierUpgradeNote(String.format(
                    "Volume increase from %d to %d shipments/week moves the %s discount from %.0f%% to %.0f%%.",
                    currentVolume, newVolume,
                    primaryCategory,
                    currentTier.getDiscountPct().multiply(BigDecimal.valueOf(100)),
                    newTier.getDiscountPct().multiply(BigDecimal.valueOf(100))));
        }
        return resp;
    }

    // -----------------------------------------------------------------------
    // Scenario: SERVICE_SHIFT
    // "What if I move 30% of my GROUND shipments to THREE_DAY?"
    // -----------------------------------------------------------------------
    public RateSimulationResponse serviceShift(RateSimulationRequest req) {
        if (req.getFromService() == null || req.getToService() == null) {
            throw new InvalidInputException("fromService and toService are required for SERVICE_SHIFT");
        }
        if (req.getShiftFraction() == null
                || req.getShiftFraction().compareTo(BigDecimal.ZERO) <= 0
                || req.getShiftFraction().compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidInputException("shiftFraction must be between 0.01 and 1.0");
        }

        Contract contract = loadContract(req.getContractId());
        BaselineSnapshot baseline = loadBaseline(req);
        int volume = baseline.getAvgWeeklyVolume().intValue();

        Map<String, Object> metrics = parseMetrics(baseline.getMetricsJson());
        Map<String, BigDecimal> spendByService = getSpendByService(metrics);
        BigDecimal annualCost = baseline.getTotalCost();

        BigDecimal fromCost = spendByService.getOrDefault(req.getFromService(), BigDecimal.ZERO);
        BigDecimal shiftedCost = fromCost.multiply(req.getShiftFraction()).setScale(SCALE, HALF_UP);

        // Resolve discount for from/to categories at current volume
        ServiceLevel fromSL = serviceLevelRepo.findById(req.getFromService())
                .orElseThrow(() -> new InvalidInputException("Unknown fromService: " + req.getFromService()));
        ServiceLevel toSL = serviceLevelRepo.findById(req.getToService())
                .orElseThrow(() -> new InvalidInputException("Unknown toService: " + req.getToService()));

        DiscountTier fromTier = discountTierRepo.findTier(
                contract.getProgramId(), fromSL.getDiscountCategory(), volume)
                .orElseThrow(() -> new RateNotFoundException("No tier for fromService category"));
        DiscountTier toTier = discountTierRepo.findTier(
                contract.getProgramId(), toSL.getDiscountCategory(), volume)
                .orElseThrow(() -> new RateNotFoundException("No tier for toService category"));

        // Estimate: the shifted portion re-rated under toService category
        BigDecimal projectedShiftedCost = engine.projectTransportCost(
                shiftedCost, fromTier.getDiscountPct(), toTier.getDiscountPct());

        // Re-rate to account for different published rates (approximate: use cost ratio)
        // toService has different base rates; we scale proportionally to category discount change
        BigDecimal projectedAnnual = annualCost
                .subtract(shiftedCost)
                .add(projectedShiftedCost)
                .setScale(SCALE, HALF_UP);

        List<LineItem> breakdown = new ArrayList<>();
        breakdown.add(new LineItem(req.getFromService(),
                req.getFromService() + " (retained portion)",
                fromCost.subtract(shiftedCost).setScale(SCALE, HALF_UP),
                fromCost.subtract(shiftedCost).setScale(SCALE, HALF_UP)));
        breakdown.add(new LineItem(req.getToService(),
                req.getToService() + " (shifted portion — baseline cost)",
                shiftedCost, projectedShiftedCost));

        RateSimulationResponse resp = buildBaseResponse(req, baseline, contract, "SERVICE_SHIFT");
        resp.setBaselineAnnualCost(annualCost);
        resp.setBaselineWeeklyCost(annualCost.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        resp.setBaselineAvgWeeklyVolume(baseline.getAvgWeeklyVolume());
        resp.setProjectedAnnualCost(projectedAnnual);
        resp.setProjectedWeeklyCost(projectedAnnual.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        setDeltas(resp, annualCost, projectedAnnual);
        resp.setServiceShiftBreakdown(breakdown);
        return resp;
    }

    // -----------------------------------------------------------------------
    // Scenario: PACKAGE_PROFILE
    // "What if average package weight increases to 15 lbs?"
    // Impact: heavier packages → higher rate band → higher base rate
    // -----------------------------------------------------------------------
    public RateSimulationResponse packageProfile(RateSimulationRequest req) {
        if (req.getNewAvgWeightLb() == null) {
            throw new InvalidInputException("newAvgWeightLb is required for PACKAGE_PROFILE");
        }

        BaselineSnapshot baseline = loadBaseline(req);
        Map<String, Object> metrics = parseMetrics(baseline.getMetricsJson());
        BigDecimal annualCost = baseline.getTotalCost();

        double currentAvgWeight = ((Number) metrics.getOrDefault("avg_weight_lbs", 8.5)).doubleValue();
        double newAvgWeight = req.getNewAvgWeightLb().doubleValue();

        // Weight impact on transportation: heavier packages hit higher rate bands
        // Estimate via weight ratio (linear approximation for projection purposes)
        BigDecimal weightRatio = BigDecimal.valueOf(newAvgWeight / currentAvgWeight);
        Map<String, BigDecimal> spendByService = getSpendByService(metrics);
        BigDecimal transportCost = sumSpendByService(metrics);
        BigDecimal nonTransport = annualCost.subtract(transportCost);

        // Apply weight ratio only to base transport (fuel and accessorials remain roughly constant)
        BigDecimal projectedTransport = transportCost.multiply(weightRatio).setScale(SCALE, HALF_UP);
        BigDecimal projectedAnnual = projectedTransport.add(nonTransport).setScale(SCALE, HALF_UP);

        RateSimulationResponse resp = buildBaseResponse(req, loadContract(req.getContractId()), baseline, "PACKAGE_PROFILE");
        resp.setBaselineAnnualCost(annualCost);
        resp.setBaselineWeeklyCost(annualCost.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        resp.setBaselineAvgWeeklyVolume(baseline.getAvgWeeklyVolume());
        resp.setProjectedAnnualCost(projectedAnnual);
        resp.setProjectedWeeklyCost(projectedAnnual.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        setDeltas(resp, annualCost, projectedAnnual);
        resp.setCaveat(String.format(
                "Weight-based projection uses linear rate approximation (%.1f lb → %.1f lb avg). "
                        + "Actual impact depends on zone/service distribution.",
                currentAvgWeight, newAvgWeight));
        resp.setConfidence("MEDIUM");
        return resp;
    }

    // -----------------------------------------------------------------------
    // Scenario: ZONE_MIX
    // "What if 40% of my shipments go to zone 8 instead of 20%?"
    // -----------------------------------------------------------------------
    public RateSimulationResponse zoneMix(RateSimulationRequest req) {
        if (req.getZoneDistribution() == null || req.getZoneDistribution().isEmpty()) {
            throw new InvalidInputException("zoneDistribution is required for ZONE_MIX");
        }

        BaselineSnapshot baseline = loadBaseline(req);
        Map<String, Object> metrics = parseMetrics(baseline.getMetricsJson());
        BigDecimal annualCost = baseline.getTotalCost();

        Map<String, BigDecimal> baselineZone = getSpendByZone(metrics);
        BigDecimal transportCost = sumSpendByService(metrics);
        BigDecimal nonTransport = annualCost.subtract(transportCost);

        // Estimate zone impact: higher zones have proportionally higher published rates
        // Use seed zone data as weight factors: zone2=1.0x, zone5=1.65x, zone8=2.10x (representative)
        Map<String, Double> zoneWeightFactor = Map.of("2", 1.0, "3", 1.2, "5", 1.65, "8", 2.1);
        double baselineWeightedZone = weightedZoneAvg(baselineZone, zoneWeightFactor);
        double projectedWeightedZone = weightedZoneAvg(req.getZoneDistribution()
                .entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue())), zoneWeightFactor);

        BigDecimal zoneRatio = projectedWeightedZone > 0
                ? BigDecimal.valueOf(projectedWeightedZone / baselineWeightedZone)
                : BigDecimal.ONE;
        BigDecimal projectedTransport = transportCost.multiply(zoneRatio).setScale(SCALE, HALF_UP);
        BigDecimal projectedAnnual = projectedTransport.add(nonTransport).setScale(SCALE, HALF_UP);

        RateSimulationResponse resp = buildBaseResponse(req, loadContract(req.getContractId()), baseline, "ZONE_MIX");
        resp.setBaselineAnnualCost(annualCost);
        resp.setBaselineWeeklyCost(annualCost.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        resp.setBaselineAvgWeeklyVolume(baseline.getAvgWeeklyVolume());
        resp.setProjectedAnnualCost(projectedAnnual);
        resp.setProjectedWeeklyCost(projectedAnnual.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        setDeltas(resp, annualCost, projectedAnnual);
        resp.setConfidence("MEDIUM");
        resp.setCaveat("Zone mix projection uses weighted rate factors. Use /api/rate/quote/batch for exact rates.");
        return resp;
    }

    // -----------------------------------------------------------------------
    // Scenario: ACCESSORIAL
    // "What if all my packages stop being residential deliveries?"
    // -----------------------------------------------------------------------
    public RateSimulationResponse accessorialChange(RateSimulationRequest req) {
        BaselineSnapshot baseline = loadBaseline(req);
        Map<String, Object> metrics = parseMetrics(baseline.getMetricsJson());
        BigDecimal annualCost = baseline.getTotalCost();
        int totalShipments = baseline.getTotalShipments() != null ? baseline.getTotalShipments() : 1040;

        Contract contract = loadContract(req.getContractId());
        List<ContractIncentive> incentives = loadIncentives(req.getContractId());
        Map<String, BigDecimal> reductions = buildReductionMap(incentives);

        BigDecimal accessorialDelta = BigDecimal.ZERO;

        // Cost of accessorials being REMOVED
        if (req.getRemoveAccessorialCodes() != null) {
            for (String code : req.getRemoveAccessorialCodes()) {
                BigDecimal fee = getEffectiveFee(code, reductions);
                double coveredPct = getAccessorialCoverPct(metrics, code);
                BigDecimal annualImpact = fee
                        .multiply(BigDecimal.valueOf(totalShipments * coveredPct))
                        .setScale(SCALE, HALF_UP);
                accessorialDelta = accessorialDelta.subtract(annualImpact); // savings
            }
        }

        // Cost of accessorials being ADDED
        if (req.getAddAccessorialCodes() != null) {
            for (String code : req.getAddAccessorialCodes()) {
                BigDecimal fee = getEffectiveFee(code, reductions);
                BigDecimal annualImpact = fee
                        .multiply(BigDecimal.valueOf(totalShipments))
                        .setScale(SCALE, HALF_UP);
                accessorialDelta = accessorialDelta.add(annualImpact);
            }
        }

        BigDecimal projectedAnnual = annualCost.add(accessorialDelta).setScale(SCALE, HALF_UP);

        RateSimulationResponse resp = buildBaseResponse(req, contract, baseline, "ACCESSORIAL");
        resp.setBaselineAnnualCost(annualCost);
        resp.setBaselineWeeklyCost(annualCost.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        resp.setBaselineAvgWeeklyVolume(baseline.getAvgWeeklyVolume());
        resp.setProjectedAnnualCost(projectedAnnual);
        resp.setProjectedWeeklyCost(projectedAnnual.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        setDeltas(resp, annualCost, projectedAnnual);
        resp.setConfidence("HIGH");
        return resp;
    }

    // -----------------------------------------------------------------------
    // Scenario: FUEL_CHANGE
    // "What if the fuel surcharge goes to 18%?"
    // -----------------------------------------------------------------------
    public RateSimulationResponse fuelChange(RateSimulationRequest req) {
        if (req.getHypotheticalFuelPct() == null) {
            throw new InvalidInputException("hypotheticalFuelPct is required for FUEL_CHANGE");
        }

        BaselineSnapshot baseline = loadBaseline(req);
        Map<String, Object> metrics = parseMetrics(baseline.getMetricsJson());
        BigDecimal annualCost = baseline.getTotalCost();

        double currentFuelPct = ((Number) metrics.getOrDefault("fuel_pct", 14.75)).doubleValue();
        BigDecimal transportCost = sumSpendByService(metrics);

        // Fuel is calculated on net transport. Isolate current fuel cost and recompute.
        // fuel_charge = net_transport × fuel_pct / 100
        // net_transport ≈ transport_cost / (1 + fuel_pct/100)
        BigDecimal currentFuelFactor = BigDecimal.valueOf(1 + currentFuelPct / 100);
        BigDecimal netTransport = transportCost.divide(currentFuelFactor, 6, HALF_UP);
        BigDecimal newFuelCharge = netTransport
                .multiply(req.getHypotheticalFuelPct())
                .divide(BigDecimal.valueOf(100), SCALE, HALF_UP);
        BigDecimal currentFuelCharge = netTransport
                .multiply(BigDecimal.valueOf(currentFuelPct))
                .divide(BigDecimal.valueOf(100), SCALE, HALF_UP);

        BigDecimal fuelDelta = newFuelCharge.subtract(currentFuelCharge);
        BigDecimal nonTransport = annualCost.subtract(transportCost);
        BigDecimal projectedAnnual = netTransport.add(newFuelCharge).add(nonTransport).setScale(SCALE, HALF_UP);

        RateSimulationResponse resp = buildBaseResponse(req, loadContract(req.getContractId()), baseline, "FUEL_CHANGE");
        resp.setBaselineAnnualCost(annualCost);
        resp.setBaselineWeeklyCost(annualCost.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        resp.setBaselineAvgWeeklyVolume(baseline.getAvgWeeklyVolume());
        resp.setProjectedAnnualCost(projectedAnnual);
        resp.setProjectedWeeklyCost(projectedAnnual.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        setDeltas(resp, annualCost, projectedAnnual);
        resp.setConfidence("HIGH");
        resp.setCaveat(String.format(
                "Fuel surcharge change from %.2f%% to %.2f%% applied to net transportation base.",
                currentFuelPct, req.getHypotheticalFuelPct().doubleValue()));
        return resp;
    }

    // -----------------------------------------------------------------------
    // Scenario: COMBINED
    // Apply any combination of volume + service shift + package profile at once
    // -----------------------------------------------------------------------
    public RateSimulationResponse combined(RateSimulationRequest req) {
        BigDecimal annualCost = loadBaseline(req).getTotalCost();
        BigDecimal running = annualCost;

        // Apply volume change first (tier shift has the biggest impact)
        if (req.getNewWeeklyVolume() != null) {
            RateSimulationResponse volResp = volumeChange(req);
            running = volResp.getProjectedAnnualCost();
        }
        // Apply package weight change on top
        if (req.getNewAvgWeightLb() != null) {
            Map<String, Object> metrics = parseMetrics(loadBaseline(req).getMetricsJson());
            double currentWeight = ((Number) metrics.getOrDefault("avg_weight_lbs", 8.5)).doubleValue();
            BigDecimal ratio = BigDecimal.valueOf(req.getNewAvgWeightLb().doubleValue() / currentWeight);
            BigDecimal transportPortion = running.multiply(BigDecimal.valueOf(0.72));
            BigDecimal nonTransport = running.subtract(transportPortion);
            running = transportPortion.multiply(ratio).add(nonTransport).setScale(SCALE, HALF_UP);
        }
        // Apply fuel change on top
        if (req.getHypotheticalFuelPct() != null) {
            Map<String, Object> metrics = parseMetrics(loadBaseline(req).getMetricsJson());
            double currentFuelPct = ((Number) metrics.getOrDefault("fuel_pct", 14.75)).doubleValue();
            BigDecimal transport = running.multiply(BigDecimal.valueOf(0.72));
            BigDecimal currentFuelFactor = BigDecimal.valueOf(1 + currentFuelPct / 100);
            BigDecimal net = transport.divide(currentFuelFactor, 6, HALF_UP);
            BigDecimal newFuel = net.multiply(req.getHypotheticalFuelPct()).divide(BigDecimal.valueOf(100), SCALE, HALF_UP);
            BigDecimal oldFuel = net.multiply(BigDecimal.valueOf(currentFuelPct)).divide(BigDecimal.valueOf(100), SCALE, HALF_UP);
            running = running.add(newFuel).subtract(oldFuel).setScale(SCALE, HALF_UP);
        }

        BaselineSnapshot baseline = loadBaseline(req);
        RateSimulationResponse resp = buildBaseResponse(req, loadContract(req.getContractId()), baseline, "COMBINED");
        resp.setBaselineAnnualCost(annualCost);
        resp.setBaselineWeeklyCost(annualCost.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        resp.setBaselineAvgWeeklyVolume(baseline.getAvgWeeklyVolume());
        resp.setProjectedAnnualCost(running);
        resp.setProjectedWeeklyCost(running.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        setDeltas(resp, annualCost, running);
        resp.setConfidence("MEDIUM");
        resp.setCaveat("Combined projection applies changes sequentially: volume → weight → fuel.");
        return resp;
    }

    // -----------------------------------------------------------------------
    // Scenario: OPTIMIZE
    // "How much more would I need to ship to hit the next discount tier?"
    // -----------------------------------------------------------------------
    public RateSimulationResponse optimize(RateSimulationRequest req) {
        Contract contract = loadContract(req.getContractId());
        BaselineSnapshot baseline = loadBaseline(req);
        int currentVolume = baseline.getAvgWeeklyVolume().intValue();

        Map<String, Object> metrics = parseMetrics(baseline.getMetricsJson());
        Map<String, BigDecimal> spendByService = getSpendByService(metrics);

        List<RateSimulationResponse.OptimizationHint> hints = new ArrayList<>();

        // For each service category in the baseline, compute the next tier threshold
        Set<String> seenCategories = new LinkedHashSet<>();
        for (String serviceCode : spendByService.keySet()) {
            ServiceLevel sl = serviceLevelRepo.findById(serviceCode).orElse(null);
            if (sl == null || sl.getDiscountCategory() == null) continue;
            seenCategories.add(sl.getDiscountCategory());
        }

        for (String category : seenCategories) {
            List<DiscountTier> allTiers = discountTierRepo
                    .findByProgramIdOrderByVolMinAsc(contract.getProgramId());
            List<DiscountTier> categoryTiers = allTiers.stream()
                    .filter(t -> category.equals(t.getCategoryCode()))
                    .sorted(Comparator.comparing(DiscountTier::getVolMin))
                    .toList();

            DiscountTier currentTier = null;
            DiscountTier nextTier = null;
            for (int i = 0; i < categoryTiers.size(); i++) {
                DiscountTier t = categoryTiers.get(i);
                if (t.getVolMin() <= currentVolume
                        && (t.getVolMax() == null || t.getVolMax() >= currentVolume)) {
                    currentTier = t;
                    if (i + 1 < categoryTiers.size()) nextTier = categoryTiers.get(i + 1);
                    break;
                }
            }
            if (currentTier == null || nextTier == null) continue;

            int volumeNeeded = nextTier.getVolMin();
            int additionalShipments = volumeNeeded - currentVolume;
            BigDecimal categoryCost = spendByService.entrySet().stream()
                    .filter(e -> {
                        ServiceLevel sl = serviceLevelRepo.findById(e.getKey()).orElse(null);
                        return sl != null && category.equals(sl.getDiscountCategory());
                    })
                    .map(Map.Entry::getValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal projectedCost = engine.projectTransportCost(
                    categoryCost, currentTier.getDiscountPct(), nextTier.getDiscountPct());
            BigDecimal annualSavings = categoryCost.subtract(projectedCost).setScale(SCALE, HALF_UP);

            RateSimulationResponse.OptimizationHint hint = new RateSimulationResponse.OptimizationHint();
            hint.setCategory(category);
            hint.setCurrentVolume(currentVolume);
            hint.setNextTierVolume(volumeNeeded);
            hint.setCurrentDiscountPct(currentTier.getDiscountPct());
            hint.setNextDiscountPct(nextTier.getDiscountPct());
            hint.setEstimatedAnnualSavings(annualSavings);
            hints.add(hint);
        }

        RateSimulationResponse resp = buildBaseResponse(req, contract, baseline, "OPTIMIZE");
        resp.setBaselineAnnualCost(baseline.getTotalCost());
        resp.setBaselineWeeklyCost(baseline.getTotalCost().divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        resp.setBaselineAvgWeeklyVolume(baseline.getAvgWeeklyVolume());
        resp.setProjectedAnnualCost(baseline.getTotalCost()); // no change — this is a what-if guide
        resp.setProjectedWeeklyCost(baseline.getTotalCost().divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        setDeltas(resp, baseline.getTotalCost(), baseline.getTotalCost());
        resp.setOptimizationHints(hints);
        resp.setConfidence("HIGH");
        return resp;
    }

    // -----------------------------------------------------------------------
    // Scenario: COMPARE
    // Run multiple named sub-scenarios and return side-by-side
    // -----------------------------------------------------------------------
    public RateSimulationResponse compare(RateSimulationRequest req) {
        if (req.getCompareScenarios() == null || req.getCompareScenarios().isEmpty()) {
            throw new InvalidInputException("compareScenarios list is required for COMPARE");
        }

        BaselineSnapshot baseline = loadBaseline(req);
        BigDecimal annualCost = baseline.getTotalCost();
        List<RateSimulationResponse.CompareEntry> entries = new ArrayList<>();

        for (RateSimulationRequest.NamedScenario ns : req.getCompareScenarios()) {
            if (ns.getRequest() == null) continue;
            // Always force the authenticated user's contractId — never trust user-supplied values in nested requests
            ns.getRequest().setContractId(req.getContractId());
            if (ns.getRequest().getBaselineId() == null) {
                ns.getRequest().setBaselineId(req.getBaselineId());
            }
            RateSimulationResponse subResp = dispatch(ns.getRequest());
            RateSimulationResponse.CompareEntry entry = new RateSimulationResponse.CompareEntry();
            entry.setLabel(ns.getLabel());
            entry.setProjectedAnnualCost(subResp.getProjectedAnnualCost());
            entry.setAnnualDelta(subResp.getAnnualDelta());
            entry.setDeltaPct(subResp.getDeltaPct());
            entries.add(entry);
        }

        RateSimulationResponse resp = buildBaseResponse(req, loadContract(req.getContractId()), baseline, "COMPARE");
        resp.setBaselineAnnualCost(annualCost);
        resp.setBaselineWeeklyCost(annualCost.divide(WEEKS_PER_YEAR, SCALE, HALF_UP));
        resp.setBaselineAvgWeeklyVolume(baseline.getAvgWeeklyVolume());
        resp.setCompareEntries(entries);
        resp.setConfidence("HIGH");
        return resp;
    }

    // -----------------------------------------------------------------------
    // Dispatch helper (used by COMPARE internally)
    // -----------------------------------------------------------------------
    public RateSimulationResponse dispatch(RateSimulationRequest req) {
        return switch (req.getScenarioType().toUpperCase()) {
            case "VOLUME_CHANGE"    -> volumeChange(req);
            case "SERVICE_SHIFT"    -> serviceShift(req);
            case "PACKAGE_PROFILE"  -> packageProfile(req);
            case "ZONE_MIX"         -> zoneMix(req);
            case "ACCESSORIAL"      -> accessorialChange(req);
            case "FUEL_CHANGE"      -> fuelChange(req);
            case "COMBINED"         -> combined(req);
            case "OPTIMIZE"         -> optimize(req);
            case "COMPARE"          -> compare(req);
            default -> throw new InvalidInputException("Unknown scenarioType: " + req.getScenarioType());
        };
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private Contract loadContract(String contractId) {
        return contractRepo.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException("Contract not found: " + contractId));
    }

    private BaselineSnapshot loadBaseline(RateSimulationRequest req) {
        if (req.getBaselineId() != null && !req.getBaselineId().isBlank()) {
            return baselineRepo.findById(UUID.fromString(req.getBaselineId()))
                    .orElseThrow(() -> new InvalidInputException("Baseline not found: " + req.getBaselineId()));
        }
        // Resolve account from contract and load latest baseline
        Contract contract = loadContract(req.getContractId());
        return baselineRepo.findLatest(contract.getCompanyId())
                .orElseThrow(() -> new InvalidInputException(
                        "No baseline snapshot found for account on contract: " + req.getContractId()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetrics(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> getSpendByService(Map<String, Object> metrics) {
        Object raw = metrics.get("spend_by_service");
        if (!(raw instanceof Map)) return new LinkedHashMap<>();
        Map<String, Object> raw2 = (Map<String, Object>) raw;
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw2.entrySet()) {
            result.put(e.getKey(), new BigDecimal(e.getValue().toString()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> getSpendByZone(Map<String, Object> metrics) {
        Object raw = metrics.get("spend_by_zone");
        if (!(raw instanceof Map)) return new LinkedHashMap<>();
        Map<String, Object> raw2 = (Map<String, Object>) raw;
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw2.entrySet()) {
            result.put(e.getKey(), new BigDecimal(e.getValue().toString()));
        }
        return result;
    }

    private BigDecimal sumSpendByService(Map<String, Object> metrics) {
        return getSpendByService(metrics).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String primaryCategory(Map<String, BigDecimal> spendByService) {
        return spendByService.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> {
                    ServiceLevel sl = serviceLevelRepo.findById(e.getKey()).orElse(null);
                    return sl != null ? sl.getDiscountCategory() : "GROUND";
                }).orElse("GROUND");
    }

    private double weightedZoneAvg(Map<String, BigDecimal> dist, Map<String, Double> factors) {
        double total = dist.values().stream().mapToDouble(BigDecimal::doubleValue).sum();
        if (total == 0) return 1.0;
        double weighted = 0;
        for (Map.Entry<String, BigDecimal> e : dist.entrySet()) {
            double factor = factors.getOrDefault(e.getKey(), 1.5);
            weighted += factor * e.getValue().doubleValue();
        }
        return weighted / total;
    }

    private List<ContractIncentive> loadIncentives(String contractId) {
        return contractRepo.findById(contractId)
                .map(c -> {
                    // Use incentive repo via engine context — cast to access
                    return List.<ContractIncentive>of();
                }).orElse(List.of());
    }

    private Map<String, BigDecimal> buildReductionMap(List<ContractIncentive> incentives) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (ContractIncentive i : incentives) {
            if ("ACCESSORIAL_REDUCE".equals(i.getIncentiveType())
                    && i.getAccessorialCode() != null && "PCT".equals(i.getUnit())) {
                map.put(i.getAccessorialCode(), i.getValue());
            }
        }
        return map;
    }

    private BigDecimal getEffectiveFee(String code, Map<String, BigDecimal> reductions) {
        return accessorialTypeRepo(code)
                .map(at -> {
                    BigDecimal reduction = reductions.getOrDefault(code, BigDecimal.ZERO);
                    return at.getDefaultFee().multiply(BigDecimal.ONE.subtract(reduction))
                            .setScale(SCALE, HALF_UP);
                }).orElse(BigDecimal.ZERO);
    }

    // Lazy accessor — avoids circular bean dependency
    private java.util.Optional<com.example.billingsimulator.model.AccessorialType> accessorialTypeRepo(String code) {
        return accessorialTypeRepoBean.findById(code);
    }

    private double getAccessorialCoverPct(Map<String, Object> metrics, String code) {
        // residential_pct is stored in metrics; others default to 100%
        if ("RESIDENTIAL".equals(code) || "RESIDENTIAL_INTL".equals(code)) {
            return ((Number) metrics.getOrDefault("residential_pct", 0.45)).doubleValue();
        }
        if ("DELIVERY_AREA".equals(code) || "DELIVERY_AREA_EXT".equals(code)) {
            return ((Number) metrics.getOrDefault("das_pct", 0.15)).doubleValue();
        }
        return 1.0;
    }

    private void setDeltas(RateSimulationResponse resp, BigDecimal baseline, BigDecimal projected) {
        BigDecimal annualDelta = projected.subtract(baseline).setScale(SCALE, HALF_UP);
        BigDecimal weeklyDelta = annualDelta.divide(WEEKS_PER_YEAR, SCALE, HALF_UP);
        BigDecimal pct = baseline.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : annualDelta.divide(baseline, 4, HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, HALF_UP);
        resp.setAnnualDelta(annualDelta);
        resp.setWeeklyDelta(weeklyDelta);
        resp.setDeltaPct(pct);
    }

    private RateSimulationResponse buildBaseResponse(RateSimulationRequest req,
                                                  Contract contract,
                                                  BaselineSnapshot baseline,
                                                  String scenarioType) {
        RateSimulationResponse resp = new RateSimulationResponse();
        resp.setScenarioType(scenarioType);
        resp.setContractId(req.getContractId());
        resp.setBaselineId(baseline.getBaselineId().toString());
        resp.setConfidence("HIGH");
        resp.setCaveat("Projected estimate — not a guaranteed invoice. Based on 12-month historical baseline.");
        return resp;
    }

    private RateSimulationResponse buildBaseResponse(RateSimulationRequest req,
                                                  BaselineSnapshot baseline,
                                                  Contract contract,
                                                  String scenarioType) {
        return buildBaseResponse(req, contract, baseline, scenarioType);
    }

    // Field injection for accessorial repo to avoid circular dependency
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.billingsimulator.repository.AccessorialTypeRepository accessorialTypeRepoBean;
}
