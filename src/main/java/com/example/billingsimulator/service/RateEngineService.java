package com.example.billingsimulator.service;

import com.example.billingsimulator.dto.LineItem;
import com.example.billingsimulator.dto.RateQuoteRequest;
import com.example.billingsimulator.dto.RateQuoteResponse;
import com.example.billingsimulator.exception.ContractNotFoundException;
import com.example.billingsimulator.exception.InvalidInputException;
import com.example.billingsimulator.exception.RateNotFoundException;
import com.example.billingsimulator.model.*;
import com.example.billingsimulator.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * Deterministic rate calculation engine.
 *
 * Formula (in order):
 *   1. Zone lookup (origin_zip prefix × dest_zip prefix)
 *   2. Billed weight = max(actual, dim = L×W×H / divisor)
 *   3. Base rate from rate_card (service, zone, weight, bill date)
 *   4. Discount = discount_tier.discount_pct for (program, discount_category, weekly_volume)
 *   5. net_transport = base_rate × (1 − discount_pct)
 *   6. Min floor: net = max(net, floor_rate × (1 − discount_pct) × (1 − addl_incentive_pct))
 *   7. Fuel = net_transport × fuel_pct / 100
 *   8. Accessorials: default_fee − contract surcharge reduction (for the 7 discountable codes)
 *   9. Total = net_transport + fuel + sum(accessorials)
 *
 * AI RULE: This class does all money math. The AI layer only classifies intent
 * and extracts parameters — it never computes or modifies monetary values.
 */
@Service
public class RateEngineService {

    private static final int SCALE = 2;
    private static final RoundingMode HALF_UP = RoundingMode.HALF_UP;

    private final ContractRepository contractRepo;
    private final RateCardRepository rateCardRepo;
    private final ZoneMatrixRepository zoneMatrixRepo;
    private final DimFactorRepository dimFactorRepo;
    private final MinShippingChargeRepository minChargeRepo;
    private final FuelIndexRepository fuelIndexRepo;
    private final AccessorialTypeRepository accessorialTypeRepo;
    private final DiscountTierRepository discountTierRepo;
    private final ContractIncentiveRepository incentiveRepo;
    private final ServiceLevelRepository serviceLevelRepo;

    public RateEngineService(
            ContractRepository contractRepo,
            RateCardRepository rateCardRepo,
            ZoneMatrixRepository zoneMatrixRepo,
            DimFactorRepository dimFactorRepo,
            MinShippingChargeRepository minChargeRepo,
            FuelIndexRepository fuelIndexRepo,
            AccessorialTypeRepository accessorialTypeRepo,
            DiscountTierRepository discountTierRepo,
            ContractIncentiveRepository incentiveRepo,
            ServiceLevelRepository serviceLevelRepo) {
        this.contractRepo = contractRepo;
        this.rateCardRepo = rateCardRepo;
        this.zoneMatrixRepo = zoneMatrixRepo;
        this.dimFactorRepo = dimFactorRepo;
        this.minChargeRepo = minChargeRepo;
        this.fuelIndexRepo = fuelIndexRepo;
        this.accessorialTypeRepo = accessorialTypeRepo;
        this.discountTierRepo = discountTierRepo;
        this.incentiveRepo = incentiveRepo;
        this.serviceLevelRepo = serviceLevelRepo;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Full single-package rate quote.
     *
     * @param request validated request from the AI/controller layer
     * @param avgWeeklyVolume account's current avg weekly volume (from baseline_snapshot),
     *                        used to resolve the correct discount tier band
     */
    public RateQuoteResponse quote(RateQuoteRequest request, int avgWeeklyVolume) {
        // --- 0. Load contract -------------------------------------------
        Contract contract = contractRepo.findById(request.getContractId())
                .orElseThrow(() -> new ContractNotFoundException(
                        "Contract not found: " + request.getContractId()));

        ServiceLevel service = serviceLevelRepo.findById(request.getServiceCode())
                .orElseThrow(() -> new InvalidInputException(
                        "Unknown service code: " + request.getServiceCode()));

        // --- 1. Zone lookup ---------------------------------------------
        int zone = resolveZone(request.getOriginZip(), request.getDestZip());

        // --- 2. Billed weight -------------------------------------------
        BigDecimal billedWeight = resolveBilledWeight(
                request.getServiceCode(),
                request.getActualWeightLb(),
                request.getLengthIn(), request.getWidthIn(), request.getHeightIn());
        boolean dimApplied = billedWeight.compareTo(request.getActualWeightLb()) > 0;

        // --- 3. Bill date from week code --------------------------------
        LocalDate billDate = parseBillDate(request.getBillWeek());

        // --- 4. Base rate -----------------------------------------------
        RateCard rateCard = rateCardRepo.findRate(request.getServiceCode(), zone, billedWeight, billDate)
                .orElseThrow(() -> new RateNotFoundException(String.format(
                        "No rate found for service=%s zone=%d weight=%.2f date=%s",
                        request.getServiceCode(), zone, billedWeight, billDate)));

        BigDecimal baseRate = rateCard.getBaseRate();

        // --- 5. Discount ------------------------------------------------
        DiscountTier tier = resolveDiscountTier(
                contract.getProgramId(), service.getDiscountCategory(), avgWeeklyVolume);
        BigDecimal discountPct = tier.getDiscountPct();
        BigDecimal netTransport = baseRate.multiply(BigDecimal.ONE.subtract(discountPct))
                .setScale(SCALE, HALF_UP);
        BigDecimal discountAmount = baseRate.subtract(netTransport);

        // --- 6. Minimum shipping charge floor ---------------------------
        boolean floorApplied = false;
        Optional<MinShippingCharge> minFloorOpt = minChargeRepo.findByServiceCode(request.getServiceCode());
        if (minFloorOpt.isPresent()) {
            MinShippingCharge minFloor = minFloorOpt.get();
            RateCard floorCard = rateCardRepo
                    .findRate(request.getServiceCode(), minFloor.getFloorZone(),
                            minFloor.getFloorWeightLb(), billDate)
                    .orElse(null);
            if (floorCard != null) {
                BigDecimal floorNet = floorCard.getBaseRate()
                        .multiply(BigDecimal.ONE.subtract(discountPct))
                        .multiply(BigDecimal.ONE.subtract(minFloor.getAddlIncentivePct()))
                        .setScale(SCALE, HALF_UP);
                if (netTransport.compareTo(floorNet) < 0) {
                    netTransport = floorNet;
                    discountAmount = baseRate.subtract(netTransport);
                    floorApplied = true;
                }
            }
        }

        // --- 7. Fuel surcharge -----------------------------------------
        FuelIndex fuelIndex = fuelIndexRepo.findCurrentRate(contract.getFuelProgram(), billDate)
                .orElseThrow(() -> new RateNotFoundException(
                        "No fuel index found for program=" + contract.getFuelProgram()
                                + " date=" + billDate));
        BigDecimal fuelPct = fuelIndex.getFuelPct();
        BigDecimal fuelCharge = netTransport
                .multiply(fuelPct)
                .divide(BigDecimal.valueOf(100), SCALE, HALF_UP);

        // --- 8. Accessorials -------------------------------------------
        List<ContractIncentive> incentives = incentiveRepo.findByContractId(request.getContractId());
        Map<String, BigDecimal> accessorialReductions = buildAccessorialReductionMap(incentives);

        List<LineItem> lineItems = new ArrayList<>();
        lineItems.add(new LineItem("TRANSPORTATION", "Transportation Charge",
                baseRate.multiply(BigDecimal.valueOf(request.getPackageCount())).setScale(SCALE, HALF_UP),
                netTransport.multiply(BigDecimal.valueOf(request.getPackageCount())).setScale(SCALE, HALF_UP)));
        lineItems.add(new LineItem("INCENTIVE_CREDIT", "Incentive Credit",
                BigDecimal.ZERO,
                discountAmount.negate().multiply(BigDecimal.valueOf(request.getPackageCount())).setScale(SCALE, HALF_UP)));
        lineItems.add(new LineItem("FUEL", "Fuel Surcharge",
                fuelCharge.multiply(BigDecimal.valueOf(request.getPackageCount())).setScale(SCALE, HALF_UP),
                fuelCharge.multiply(BigDecimal.valueOf(request.getPackageCount())).setScale(SCALE, HALF_UP)));

        BigDecimal totalAccessorial = BigDecimal.ZERO;
        List<String> triggeredCodes = resolveTriggeredAccessorials(request);
        for (String code : triggeredCodes) {
            AccessorialType at = accessorialTypeRepo.findById(code).orElse(null);
            if (at == null) continue;
            BigDecimal fee = at.getDefaultFee();
            BigDecimal reduction = accessorialReductions.getOrDefault(code, BigDecimal.ZERO);
            BigDecimal netFee = fee.multiply(BigDecimal.ONE.subtract(reduction)).setScale(SCALE, HALF_UP);
            int qty = "PER_SHIPMENT".equals(at.getApplyBasis()) ? 1 : request.getPackageCount();
            BigDecimal lineTotal = netFee.multiply(BigDecimal.valueOf(qty)).setScale(SCALE, HALF_UP);
            totalAccessorial = totalAccessorial.add(lineTotal);
            lineItems.add(new LineItem(code, at.getDisplayName(),
                    fee.multiply(BigDecimal.valueOf(qty)).setScale(SCALE, HALF_UP), lineTotal));
        }

        // --- 9. Total --------------------------------------------------
        BigDecimal total = netTransport
                .multiply(BigDecimal.valueOf(request.getPackageCount()))
                .add(fuelCharge.multiply(BigDecimal.valueOf(request.getPackageCount())))
                .add(totalAccessorial)
                .setScale(SCALE, HALF_UP);

        // --- Build response --------------------------------------------
        RateQuoteResponse resp = new RateQuoteResponse();
        resp.setContractId(request.getContractId());
        resp.setServiceCode(request.getServiceCode());
        resp.setServiceDisplayName(service.getDisplayName());
        resp.setZone(zone);
        resp.setActualWeightLb(request.getActualWeightLb());
        resp.setBilledWeightLb(billedWeight);
        resp.setWeightBasis(dimApplied ? "DIM" : "ACTUAL");
        resp.setPublishedCharge(baseRate.multiply(BigDecimal.valueOf(request.getPackageCount())).setScale(SCALE, HALF_UP));
        resp.setDiscountPct(discountPct);
        resp.setDiscountAmount(discountAmount.multiply(BigDecimal.valueOf(request.getPackageCount())).setScale(SCALE, HALF_UP));
        resp.setNetTransport(netTransport.multiply(BigDecimal.valueOf(request.getPackageCount())).setScale(SCALE, HALF_UP));
        resp.setMinFloorApplied(floorApplied);
        resp.setFuelPct(fuelPct);
        resp.setFuelCharge(fuelCharge.multiply(BigDecimal.valueOf(request.getPackageCount())).setScale(SCALE, HALF_UP));
        resp.setAccessorialCharge(totalAccessorial);
        resp.setTotalCharge(total);
        resp.setLineItems(lineItems);
        resp.setRateCardVersion(rateCard.getVersion());
        resp.setBillWeek(request.getBillWeek() != null ? request.getBillWeek() : currentWeekCode());
        resp.setDiscountTierBand(formatTierBand(tier));
        resp.setConfidence("HIGH");
        resp.setCaveat("Projected rate — not a guaranteed quote. Based on published rates in effect at time of query.");
        return resp;
    }

    public List<String> getRateCardVersions() {
        return rateCardRepo.findAllVersions();
    }

    // -----------------------------------------------------------------------
    // Package-private helpers (also used by SimulationService)
    // -----------------------------------------------------------------------

    int resolveZone(String originZip, String destZip) {
        String originPrefix = originZip.length() >= 2 ? originZip.substring(0, 2) : originZip;
        String destPrefix = destZip.length() >= 2 ? destZip.substring(0, 2) : destZip;
        return zoneMatrixRepo.findByOriginPrefixAndDestPrefix(originPrefix, destPrefix)
                .map(ZoneMatrix::getZone)
                .orElseThrow(() -> new RateNotFoundException(
                        "No zone found for origin=" + originZip + " dest=" + destZip));
    }

    BigDecimal resolveBilledWeight(String serviceCode, BigDecimal actualWeight,
                                   BigDecimal l, BigDecimal w, BigDecimal h) {
        if (l == null || w == null || h == null) return actualWeight;
        DimFactor dimFactor = dimFactorRepo.findById(serviceCode).orElse(null);
        if (dimFactor == null) return actualWeight;
        BigDecimal dimWeight = l.multiply(w).multiply(h)
                .divide(BigDecimal.valueOf(dimFactor.getDivisor()), SCALE, HALF_UP);
        return actualWeight.max(dimWeight);
    }

    DiscountTier resolveDiscountTier(UUID programId, String discountCategory, int avgWeeklyVolume) {
        return discountTierRepo.findTier(programId, discountCategory, avgWeeklyVolume)
                .orElseThrow(() -> new RateNotFoundException(String.format(
                        "No discount tier for program=%s category=%s volume=%d",
                        programId, discountCategory, avgWeeklyVolume)));
    }

    /**
     * Recompute projected annual transport cost using a new discount pct.
     * Used by SimulationService for volume-change and service-shift projections.
     *
     * @param baselineTransportCost annual transport-only baseline cost
     * @param baselineDiscountPct   current discount (e.g. 0.44)
     * @param newDiscountPct        projected discount
     */
    BigDecimal projectTransportCost(BigDecimal baselineTransportCost,
                                    BigDecimal baselineDiscountPct,
                                    BigDecimal newDiscountPct) {
        // baseline_transport = published × (1 − old_discount)
        // published = baseline_transport / (1 − old_discount)
        // projected = published × (1 − new_discount)
        BigDecimal publishedCost = baselineTransportCost
                .divide(BigDecimal.ONE.subtract(baselineDiscountPct), 6, HALF_UP);
        return publishedCost.multiply(BigDecimal.ONE.subtract(newDiscountPct))
                .setScale(SCALE, HALF_UP);
    }

    LocalDate parseBillDate(String billWeek) {
        if (billWeek == null || billWeek.isBlank()) {
            return LocalDate.now();
        }
        // '2026-W25' → Monday of that ISO week
        try {
            WeekFields wf = WeekFields.ISO;
            String[] parts = billWeek.split("-W");
            int year = Integer.parseInt(parts[0]);
            int week = Integer.parseInt(parts[1]);
            return LocalDate.of(year, 1, 4)
                    .with(wf.weekOfWeekBasedYear(), week)
                    .with(wf.dayOfWeek(), 1);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    String formatTierBand(DiscountTier tier) {
        if (tier.getVolMax() == null) {
            return tier.getVolMin() + "+ shipments/week";
        }
        return tier.getVolMin() + "–" + tier.getVolMax() + " shipments/week";
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private Map<String, BigDecimal> buildAccessorialReductionMap(List<ContractIncentive> incentives) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (ContractIncentive i : incentives) {
            if ("ACCESSORIAL_REDUCE".equals(i.getIncentiveType())
                    && i.getAccessorialCode() != null
                    && "PCT".equals(i.getUnit())) {
                map.put(i.getAccessorialCode(), i.getValue());
            }
        }
        return map;
    }

    private List<String> resolveTriggeredAccessorials(RateQuoteRequest req) {
        List<String> triggered = new ArrayList<>();
        if (req.isResidential()) {
            triggered.add("RESIDENTIAL");
        }
        if (req.getDeclaredValue() != null
                && req.getDeclaredValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            triggered.add("DECLARED_VALUE");
        }
        return triggered;
    }

    private String currentWeekCode() {
        LocalDate now = LocalDate.now();
        WeekFields wf = WeekFields.ISO;
        int week = now.get(wf.weekOfWeekBasedYear());
        return now.getYear() + "-W" + String.format("%02d", week);
    }
}
