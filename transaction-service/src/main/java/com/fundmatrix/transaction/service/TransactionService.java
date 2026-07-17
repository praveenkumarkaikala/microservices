package com.fundmatrix.transaction.service;

import com.fundmatrix.transaction.client.FolioKycClient;
import com.fundmatrix.transaction.client.FundCatalogClient;
import com.fundmatrix.transaction.client.NavAccountingClient;
import com.fundmatrix.transaction.common.Calc;
import com.fundmatrix.transaction.common.exception.BusinessException;
import com.fundmatrix.transaction.common.exception.ResourceNotFoundException;
import com.fundmatrix.transaction.domain.Allotment;
import com.fundmatrix.transaction.domain.SipMandate;
import com.fundmatrix.transaction.domain.SwpMandate;
import com.fundmatrix.transaction.domain.Transaction;
import com.fundmatrix.transaction.domain.TransactionFlag;
import com.fundmatrix.transaction.domain.enums.AllotmentStatus;
import com.fundmatrix.transaction.domain.enums.CutOffStatus;
import com.fundmatrix.transaction.domain.enums.FlagStatus;
import com.fundmatrix.transaction.domain.enums.NotificationCategory;
import com.fundmatrix.transaction.domain.enums.Role;
import com.fundmatrix.transaction.domain.enums.TransactionStatus;
import com.fundmatrix.transaction.domain.enums.TransactionType;
import com.fundmatrix.transaction.dto.AllotmentDto;
import com.fundmatrix.transaction.dto.AllotmentResultDto;
import com.fundmatrix.transaction.dto.CreditUnitsRequest;
import com.fundmatrix.transaction.dto.DebitUnitsRequest;
import com.fundmatrix.transaction.dto.FolioDto;
import com.fundmatrix.transaction.dto.HoldingDto;
import com.fundmatrix.transaction.dto.KycStatusDto;
import com.fundmatrix.transaction.dto.RedemptionRequest;
import com.fundmatrix.transaction.dto.SchemeOptionDto;
import com.fundmatrix.transaction.dto.SubscriptionRequest;
import com.fundmatrix.transaction.dto.SwitchRequest;
import com.fundmatrix.transaction.dto.TransactionDto;
import com.fundmatrix.transaction.repository.AllotmentRepository;
import com.fundmatrix.transaction.repository.TransactionFlagRepository;
import com.fundmatrix.transaction.repository.TransactionRepository;
import com.fundmatrix.transaction.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction processing: subscription, redemption, switch and SIP/SWP instalment flows,
 * with cut-off enforcement, the operations accept→allot workflow, NAV-based unit
 * allotment, exit-load handling and holding updates.
 *
 * Cross-service rewrites vs. the old folio-transaction-service (Transaction/Allotment/
 * TransactionFlag/SipMandate/SwpMandate half only - Folio/Holding/KYC now live in
 * folio-kyc-service):
 *  - Folio access-control/active-folio checks go through {@link FolioAccessService} (Feign to
 *    folio-kyc-service) instead of an in-process FolioService.
 *  - Holdings credit/debit go through {@link FolioKycClient#creditUnits}/{@link FolioKycClient#debitUnits}.
 *    The debit endpoint didn't exist in the first cut of this split (folio-kyc-service only ever
 *    exposed /holdings/credit, inherited from the pre-split monolith where debit was in-process
 *    only) - it was added to folio-kyc-service's HoldingInternalController once this gap surfaced.
 *  - NAV lookups go through {@link NavAccountingClient} (unchanged from the old code - these
 *    were already Feign calls before this split).
 *  - The KYC gate calls {@link FolioKycClient#kycStatus} (previously ComplianceKycClient
 *    pointed at compliance-kyc-service; KYC moved to folio-kyc-service in this split).
 *  - Investor-name-in-message call sites (activeFolio's KYC-gate message and the SWP
 *    instalment KYC-gate message) stay SIMPLIFIED to reference the folio number instead of the
 *    investor's name, consistent with the old folio-transaction-service build (see its
 *    TransactionService class javadoc) - both are hot paths invoked on every transaction
 *    placement and instalment run.
 */
@Service
public class TransactionService {

    /** Transactions at or above this value are surfaced for compliance review. */
    private static final BigDecimal LARGE_TXN_THRESHOLD = new BigDecimal("1000000");

    private final TransactionRepository transactionRepository;
    private final AllotmentRepository allotmentRepository;
    private final TransactionFlagRepository flagRepository;
    private final FolioAccessService folioAccessService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;
    private final FundCatalogClient fundCatalogClient;
    private final NavAccountingClient navAccountingClient;
    private final FolioKycClient folioKycClient;
    private final TransactionService self;   // self-proxy: per-item transactions in batch allotment

    private final LocalTime standardCutoff;
    private final LocalTime liquidCutoff;

    public TransactionService(TransactionRepository transactionRepository,
                              AllotmentRepository allotmentRepository,
                              TransactionFlagRepository flagRepository,
                              FolioAccessService folioAccessService,
                              NotificationService notificationService,
                              AuditService auditService, CurrentUserService currentUser, Mapper mapper,
                              FundCatalogClient fundCatalogClient, NavAccountingClient navAccountingClient,
                              FolioKycClient folioKycClient,
                              @Lazy TransactionService self,
                              @Value("${fundmatrix.operations.cutoff-time}") String cutoffTime,
                              @Value("${fundmatrix.operations.liquid-cutoff-time}") String liquidCutoffTime) {
        this.transactionRepository = transactionRepository;
        this.allotmentRepository = allotmentRepository;
        this.flagRepository = flagRepository;
        this.folioAccessService = folioAccessService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.fundCatalogClient = fundCatalogClient;
        this.navAccountingClient = navAccountingClient;
        this.folioKycClient = folioKycClient;
        this.self = self;
        this.standardCutoff = LocalTime.parse(cutoffTime);
        this.liquidCutoff = LocalTime.parse(liquidCutoffTime);
    }

    // ----------------------------------------------------------------- placement

    @Transactional
    public TransactionDto placeSubscription(SubscriptionRequest req) {
        FolioDto folio = activeFolio(req.folioId());
        SchemeOptionDto option = activeOption(req.optionId());

        BigDecimal amount = Calc.money(req.amount());
        HoldingDto existingHolding = getHolding(folio.id(), option.id());
        boolean firstPurchase = existingHolding == null;
        if (firstPurchase && option.minInvestment() != null
                && amount.compareTo(option.minInvestment()) < 0) {
            throw new BusinessException("Minimum investment for " + option.schemeName()
                    + " is " + option.minInvestment());
        }

        Transaction txn = newTransaction(folio, option, TransactionType.SUBSCRIPTION);
        txn.setAmount(amount);
        txn.setStatus(TransactionStatus.RECEIVED);
        txn = save(txn);
        flagIfLarge(txn);   // flag large subscriptions immediately at placement

        notifyInvestor(folio.investorId(), NotificationCategory.TRANSACTION,
                "Subscription of " + amount + " into " + option.schemeName() + " received ("
                        + txn.getTransactionRef() + ")");
        auditService.record("SUBSCRIPTION_PLACE", "Transaction", txn.getId(),
                "Subscription " + amount + " folio " + folio.folioNumber());
        return toTxnDto(txn, option);
    }

    @Transactional
    public TransactionDto placeRedemption(RedemptionRequest req) {
        FolioDto folio = activeFolio(req.folioId());
        SchemeOptionDto option = activeOption(req.optionId());
        HoldingDto holding = getHolding(folio.id(), option.id());
        if (holding == null) {
            throw new BusinessException("No holding to redeem in this option");
        }

        BigDecimal units = resolveRedemptionUnits(req, option, holding);
        if (units.signum() <= 0) {
            throw new BusinessException("Redemption units must be greater than zero");
        }
        if (units.compareTo(Calc.nz(holding.unitsHeld())) > 0) {
            throw new BusinessException("Insufficient units: holding " + holding.unitsHeld());
        }

        Transaction txn = newTransaction(folio, option, TransactionType.REDEMPTION);
        txn.setUnits(Calc.units(units));
        txn.setStatus(TransactionStatus.RECEIVED);
        txn = save(txn);

        notifyInvestor(folio.investorId(), NotificationCategory.TRANSACTION,
                "Redemption of " + txn.getUnits() + " units from "
                        + option.schemeName() + " received (" + txn.getTransactionRef() + ")");
        auditService.record("REDEMPTION_PLACE", "Transaction", txn.getId(),
                "Redemption " + txn.getUnits() + " units folio " + folio.folioNumber());
        return toTxnDto(txn, option);
    }

    @Transactional
    public List<TransactionDto> switchUnits(SwitchRequest req) {
        FolioDto folio = activeFolio(req.folioId());
        SchemeOptionDto fromOption = activeOption(req.fromOptionId());
        SchemeOptionDto toOption = activeOption(req.toOptionId());
        if (fromOption.id().equals(toOption.id())) {
            throw new BusinessException("Source and target options must differ");
        }
        HoldingDto holding = getHolding(folio.id(), fromOption.id());
        if (holding == null) {
            throw new BusinessException("No holding to switch in the source option");
        }

        BigDecimal units = req.switchAll() ? Calc.nz(holding.unitsHeld()) : Calc.units(req.units());
        if (units == null || units.signum() <= 0 || units.compareTo(Calc.nz(holding.unitsHeld())) > 0) {
            throw new BusinessException("Invalid switch units; holding is " + holding.unitsHeld());
        }

        BigDecimal fromNav = navAccountingClient.requirePublishedNav(fromOption.id());
        BigDecimal toNav = navAccountingClient.requirePublishedNav(toOption.id());
        BigDecimal grossAmount = Calc.amountFor(units, fromNav);

        // Switch-out leg
        debitUnits(folio.id(), fromOption.schemeId(), fromOption.id(), units, fromNav);
        Transaction out = newTransaction(folio, fromOption, TransactionType.SWITCH);
        out.setUnits(Calc.units(units));
        out.setApplicableNav(fromNav);
        out.setAmount(grossAmount);
        out.setStatus(TransactionStatus.ALLOTTED);
        out.setRemarks("Switch-out to " + toOption.schemeName());
        out = save(out);
        recordAllotment(out, units, fromNav);

        // Switch-in leg
        BigDecimal toUnits = Calc.unitsFor(grossAmount, toNav);
        folioKycClient.creditUnits(new CreditUnitsRequest(folio.id(), toOption.schemeId(), toOption.id(),
                toUnits, grossAmount, toNav));
        Transaction in = newTransaction(folio, toOption, TransactionType.SWITCH);
        in.setUnits(toUnits);
        in.setApplicableNav(toNav);
        in.setAmount(grossAmount);
        in.setStatus(TransactionStatus.ALLOTTED);
        in.setRemarks("Switch-in from " + fromOption.schemeName());
        in = save(in);
        recordAllotment(in, toUnits, toNav);

        notifyInvestor(folio.investorId(), NotificationCategory.TRANSACTION,
                "Switch of " + units + " units from " + fromOption.schemeName()
                        + " to " + toOption.schemeName() + " completed");
        auditService.record("SWITCH", "Transaction", in.getId(),
                "Switch " + units + " units folio " + folio.folioNumber());
        return List.of(toTxnDto(out, fromOption), toTxnDto(in, toOption));
    }

    // ----------------------------------------------------------------- ops workflow

    @Transactional
    public TransactionDto accept(Long id) {
        Transaction txn = require(id);
        if (txn.getStatus() != TransactionStatus.RECEIVED) {
            throw new BusinessException("Only RECEIVED transactions can be accepted");
        }
        txn.setStatus(TransactionStatus.ACCEPTED);
        auditService.record("TXN_ACCEPT", "Transaction", id, "Accepted " + txn.getTransactionRef());
        return toTxnDto(transactionRepository.save(txn));
    }

    @Transactional
    public TransactionDto allot(Long id) {
        Transaction txn = require(id);
        if (txn.getStatus() != TransactionStatus.ACCEPTED && txn.getStatus() != TransactionStatus.RECEIVED) {
            throw new BusinessException("Only RECEIVED/ACCEPTED transactions can be allotted");
        }
        return switch (txn.getTransactionType()) {
            case SUBSCRIPTION, SIP_INSTALMENT -> allotSubscription(txn);
            case REDEMPTION, SWP_INSTALMENT -> allotRedemption(txn);
            default -> throw new BusinessException(
                    "Transaction type " + txn.getTransactionType() + " is not allotted via this workflow");
        };
    }

    @Transactional
    public TransactionDto reject(Long id, String reason) {
        Transaction txn = require(id);
        if (txn.getStatus() == TransactionStatus.ALLOTTED || txn.getStatus() == TransactionStatus.REVERSED) {
            throw new BusinessException("Allotted/reversed transactions cannot be rejected");
        }
        txn.setStatus(TransactionStatus.REJECTED);
        txn.setRemarks(reason);
        notifyInvestor(txn.getInvestorId(), NotificationCategory.TRANSACTION,
                "Transaction " + txn.getTransactionRef() + " was rejected: " + reason);
        auditService.record("TXN_REJECT", "Transaction", id, "Rejected: " + reason);
        return toTxnDto(transactionRepository.save(txn));
    }

    /** Allots each transaction in its OWN transaction (via self-proxy); one failure never aborts the rest. */
    public List<AllotmentResultDto> allotBatch(List<Long> ids) {
        List<AllotmentResultDto> results = new ArrayList<>();
        for (Long id : ids) {
            try {
                TransactionDto dto = self.allot(id);
                results.add(new AllotmentResultDto(id, dto.transactionRef(), true, dto.status().name(),
                        "Allotted " + dto.units() + " units @ NAV " + dto.applicableNav()));
            } catch (Exception ex) {
                String ref = transactionRepository.findById(id)
                        .map(Transaction::getTransactionRef).orElse(String.valueOf(id));
                results.add(new AllotmentResultDto(id, ref, false, "FAILED", ex.getMessage()));
            }
        }
        auditService.record("ALLOT_BATCH", "Transaction", null,
                "Batch allotment attempted for " + ids.size() + " transaction(s)");
        return results;
    }

    private TransactionDto allotSubscription(Transaction txn) {
        BigDecimal nav = navAccountingClient.requirePublishedNav(txn.getOptionId());
        BigDecimal units = Calc.unitsFor(txn.getAmount(), nav);

        folioKycClient.creditUnits(new CreditUnitsRequest(txn.getFolioId(), txn.getSchemeId(), txn.getOptionId(),
                units, txn.getAmount(), nav));
        txn.setUnits(units);
        txn.setApplicableNav(nav);
        txn.setStatus(TransactionStatus.ALLOTTED);
        transactionRepository.save(txn);
        recordAllotment(txn, units, nav);

        notifyInvestor(txn.getInvestorId(), NotificationCategory.TRANSACTION,
                units + " units allotted at NAV " + nav + " for " + txn.getTransactionRef());
        auditService.record("ALLOT_SUBSCRIPTION", "Transaction", txn.getId(),
                units + " units @ " + nav);
        return toTxnDto(txn);
    }

    private TransactionDto allotRedemption(Transaction txn) {
        SchemeOptionDto option = fundCatalogClient.getOption(txn.getOptionId());
        BigDecimal nav = navAccountingClient.requirePublishedNav(txn.getOptionId());
        BigDecimal units = txn.getUnits();
        BigDecimal gross = Calc.amountFor(units, nav);
        BigDecimal exitLoad = computeExitLoad(option, gross);
        BigDecimal net = Calc.money(gross.subtract(exitLoad));

        debitUnits(txn.getFolioId(), txn.getSchemeId(), txn.getOptionId(), units, nav);
        txn.setApplicableNav(nav);
        txn.setAmount(net);
        txn.setExitLoadAmount(exitLoad);
        txn.setStatus(TransactionStatus.ALLOTTED);
        transactionRepository.save(txn);
        recordAllotment(txn, units, nav);

        notifyInvestor(txn.getInvestorId(), NotificationCategory.TRANSACTION,
                "Redemption " + txn.getTransactionRef() + " processed: net payout " + net
                        + (exitLoad.signum() > 0 ? " (exit load " + exitLoad + ")" : ""));
        auditService.record("ALLOT_REDEMPTION", "Transaction", txn.getId(),
                "Redeemed " + units + " units @ " + nav + ", net " + net);
        return toTxnDto(txn, option);
    }

    // ----------------------------------------------------------------- SIP support

    /** Creates and immediately allots a SIP instalment subscription for a mandate. */
    @Transactional
    public Transaction placeAndAllotSipInstalment(SipMandate mandate) {
        SchemeOptionDto option = fundCatalogClient.getOption(mandate.getOptionId());
        Transaction txn = Transaction.builder()
                .folioId(mandate.getFolioId()).folioNumber(mandate.getFolioNumber())
                .investorId(mandate.getInvestorId()).distributorId(mandate.getDistributorId())
                .schemeId(option.schemeId()).optionId(option.id())
                .transactionType(TransactionType.SIP_INSTALMENT)
                .transactionDate(Instant.now())
                .cutOffStatus(cutOff(option))
                .amount(Calc.money(mandate.getAmount()))
                .status(TransactionStatus.ACCEPTED)
                .sipMandate(mandate)
                .remarks("SIP instalment for mandate " + mandate.getMandateRef())
                .build();
        txn = save(txn);
        allotSubscription(txn);
        return txn;
    }

    /**
     * Creates and immediately allots one SWP (withdrawal) instalment for a mandate.
     * Blocks if the folio is frozen, the investor's KYC is not compliant, or there are
     * insufficient units to cover the fixed withdrawal amount.
     */
    @Transactional
    public Transaction placeAndAllotSwpInstalment(SwpMandate mandate) {
        FolioDto folio = folioKycClient.getFolio(mandate.getFolioId());
        if (folio == null) {
            throw ResourceNotFoundException.of("InvestorFolio", mandate.getFolioId());
        }

        if (!"ACTIVE".equals(folio.status())) {
            throw new BusinessException("Folio " + folio.folioNumber() + " is " + folio.status()
                    + "; SWP instalment is blocked");
        }
        if (!isKycCompliant(folio.investorId())) {
            // Simplified: uses the folio number rather than the investor's name (see class javadoc).
            throw new BusinessException("KYC is not verified for folio " + folio.folioNumber()
                    + "; SWP instalment is blocked");
        }

        HoldingDto holding = getHolding(folio.id(), mandate.getOptionId());
        if (holding == null) {
            throw new BusinessException("No holding to withdraw from for this option");
        }
        BigDecimal nav = navAccountingClient.requirePublishedNav(mandate.getOptionId());
        BigDecimal units = Calc.unitsFor(mandate.getAmount(), nav);
        if (units.signum() <= 0) {
            throw new BusinessException("SWP amount is too small to redeem any units");
        }
        if (units.compareTo(Calc.nz(holding.unitsHeld())) > 0) {
            throw new BusinessException("Insufficient units for SWP instalment; holding is "
                    + holding.unitsHeld());
        }

        SchemeOptionDto option = fundCatalogClient.getOption(mandate.getOptionId());
        Transaction txn = Transaction.builder()
                .folioId(folio.id()).folioNumber(folio.folioNumber()).investorId(folio.investorId())
                .distributorId(folio.distributorId())
                .schemeId(option.schemeId()).optionId(option.id())
                .transactionType(TransactionType.SWP_INSTALMENT)
                .transactionDate(Instant.now())
                .cutOffStatus(cutOff(option))
                .units(Calc.units(units))
                .status(TransactionStatus.ACCEPTED)
                .remarks("SWP instalment for mandate " + mandate.getMandateRef())
                .build();
        txn = save(txn);
        allotRedemption(txn);
        return txn;
    }

    // ----------------------------------------------------------------- queries

    @Transactional(readOnly = true)
    public List<TransactionDto> queue() {
        return transactionRepository.findByStatusInOrderByTransactionDateAsc(
                        List.of(TransactionStatus.RECEIVED, TransactionStatus.ACCEPTED))
                .stream().map(this::toTxnDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> listForCurrentUser() {
        Role role = currentUser.getRole();
        return switch (role) {
            case INVESTOR -> transactionRepository
                    .findByInvestorIdOrderByTransactionDateDesc(currentUser.getId())
                    .stream().map(this::toTxnDto).toList();
            // See FolioAccessService's class-level note: distributorId is assumed == the
            // DISTRIBUTOR user's id.
            case DISTRIBUTOR -> transactionRepository
                    .findByDistributorIdOrderByTransactionDateDesc(currentUser.getId())
                    .stream().map(this::toTxnDto).toList();
            default -> transactionRepository.findAll().stream().map(this::toTxnDto).toList();
        };
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> listByFolio(Long folioId) {
        folioAccessService.loadAccessible(folioId);
        return transactionRepository.findByFolioIdOrderByTransactionDateDesc(folioId)
                .stream().map(this::toTxnDto).toList();
    }

    @Transactional(readOnly = true)
    public AllotmentDto getAllotment(Long transactionId) {
        return allotmentRepository.findByTransaction_Id(transactionId).map(mapper::toAllotmentDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No allotment for transaction " + transactionId));
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> flaggedTransactions() {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getAmount() != null && t.getAmount().compareTo(LARGE_TXN_THRESHOLD) >= 0)
                .map(this::toTxnDto).toList();
    }

    // ----------------------------------------------------------------- helpers

    private Transaction newTransaction(FolioDto folio, SchemeOptionDto option, TransactionType type) {
        return Transaction.builder()
                .folioId(folio.id()).folioNumber(folio.folioNumber())
                .investorId(folio.investorId()).distributorId(folio.distributorId())
                .schemeId(option.schemeId()).optionId(option.id())
                .transactionType(type)
                .transactionDate(Instant.now())
                .cutOffStatus(cutOff(option))
                .status(TransactionStatus.RECEIVED)
                .build();
    }

    private Transaction save(Transaction txn) {
        boolean isNew = txn.getId() == null;
        txn = transactionRepository.save(txn);
        if (isNew) {
            txn.setTransactionRef(String.format("TXN%08d", txn.getId()));
            txn = transactionRepository.save(txn);
        }
        return txn;
    }

    private void recordAllotment(Transaction txn, BigDecimal units, BigDecimal nav) {
        Allotment allotment = allotmentRepository.findByTransaction_Id(txn.getId())
                .orElseGet(Allotment::new);
        allotment.setTransaction(txn);
        allotment.setUnitsAllotted(Calc.units(units));
        allotment.setAllotmentNav(nav);
        allotment.setAllotmentDate(LocalDate.now());
        allotment.setStatus(AllotmentStatus.ALLOTTED);
        allotmentRepository.save(allotment);
        flagIfLarge(txn);
    }

    /** Auto-raises an OPEN compliance flag for high-value transactions (>= threshold), once per txn. */
    private void flagIfLarge(Transaction txn) {
        if (txn.getId() == null || txn.getAmount() == null
                || txn.getAmount().compareTo(LARGE_TXN_THRESHOLD) < 0
                || flagRepository.existsByTransaction_Id(txn.getId())) {
            return;
        }
        flagRepository.save(TransactionFlag.builder()
                .transaction(txn)
                .reason("High-value transaction (>= " + LARGE_TXN_THRESHOLD + ")")
                .amount(txn.getAmount())
                .status(FlagStatus.OPEN)
                .createdDate(Instant.now())
                .build());
        auditService.record("TXN_FLAGGED", "Transaction", txn.getId(),
                "Auto-flagged high-value transaction " + txn.getAmount());
    }

    private CutOffStatus cutOff(SchemeOptionDto option) {
        LocalTime cutoff = "LIQUID".equals(option.schemeCategory()) ? liquidCutoff : standardCutoff;
        if (option.cutoffTime() != null && !option.cutoffTime().isBlank()) {
            try {
                cutoff = LocalTime.parse(option.cutoffTime().trim());   // per-scheme override
            } catch (Exception ignored) {
                // fall back to the category default on malformed config
            }
        }
        return LocalTime.now().isAfter(cutoff) ? CutOffStatus.AFTER_CUTOFF : CutOffStatus.BEFORE_CUTOFF;
    }

    private BigDecimal computeExitLoad(SchemeOptionDto option, BigDecimal gross) {
        if (option.exitLoadRate() == null || option.exitLoadRate().signum() <= 0) {
            return Calc.money(BigDecimal.ZERO);
        }
        // Simplification: lot-level holding periods are not tracked in Phase 1, so the
        // configured exit-load rate is applied to the redemption value.
        return Calc.percentOf(gross, option.exitLoadRate());
    }

    private BigDecimal resolveRedemptionUnits(RedemptionRequest req, SchemeOptionDto option,
                                              HoldingDto holding) {
        if (req.redeemAll()) {
            return Calc.nz(holding.unitsHeld());
        }
        if (req.units() != null) {
            return Calc.units(req.units());
        }
        if (req.amount() != null) {
            BigDecimal nav = navAccountingClient.requirePublishedNav(option.id());
            return Calc.unitsFor(req.amount(), nav);
        }
        throw new BusinessException("Provide units, amount, or set redeemAll for a redemption");
    }

    private FolioDto activeFolio(Long folioId) {
        FolioDto folio = folioAccessService.loadAccessible(folioId);
        if (!"ACTIVE".equals(folio.status())) {
            throw new BusinessException("Folio " + folio.folioNumber() + " is " + folio.status()
                    + "; transactions are not permitted");
        }
        // KYC gate: subscriptions, redemptions and switches are only permitted once the
        // folio's investor has a verified (COMPLIANT) KYC record.
        if (!isKycCompliant(folio.investorId())) {
            // Simplified: uses the folio number rather than the investor's name (see class javadoc).
            throw new BusinessException("KYC is not verified for folio " + folio.folioNumber()
                    + ". Subscriptions, redemptions and switches are blocked until KYC is COMPLIANT.");
        }
        return folio;
    }

    private boolean isKycCompliant(Long investorId) {
        KycStatusDto status = folioKycClient.kycStatus(investorId);
        return status != null && status.compliant();
    }

    private SchemeOptionDto activeOption(Long optionId) {
        SchemeOptionDto option = fundCatalogClient.getOption(optionId);
        if (option == null) {
            throw ResourceNotFoundException.of("SchemeOption", optionId);
        }
        if (!"ACTIVE".equals(option.optionStatus())) {
            throw new BusinessException("Scheme option is inactive");
        }
        if ("CLOSED".equals(option.schemeStatus()) || "WOUND_UP".equals(option.schemeStatus())) {
            throw new BusinessException("Scheme " + option.schemeName() + " is " + option.schemeStatus());
        }
        return option;
    }

    /** Looks up the current holding for a folio/option via folio-kyc-service, or null if none. */
    private HoldingDto getHolding(Long folioId, Long optionId) {
        return folioKycClient.getHoldingsByOption(optionId).stream()
                .filter(h -> folioId.equals(h.folioId()))
                .findFirst().orElse(null);
    }

    /**
     * Debits units from a holding via folio-kyc-service's POST /holdings/debit. Average cost is
     * unchanged on redemption; insufficient-units validation happens server-side there.
     */
    private void debitUnits(Long folioId, Long schemeId, Long optionId,
                           BigDecimal redeemUnits, BigDecimal navValue) {
        // folio-kyc-service now exposes a dedicated POST /holdings/debit endpoint (added after
        // this split surfaced the gap); insufficient-units validation happens there, same as
        // the original in-process HoldingService.debitUnits check.
        folioKycClient.debitUnits(new DebitUnitsRequest(folioId, optionId, redeemUnits, navValue));
    }

    private Transaction require(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", id));
    }

    private void notifyInvestor(Long investorId, NotificationCategory category, String message) {
        notificationService.notify(investorId, category, message);
    }

    private TransactionDto toTxnDto(Transaction txn) {
        SchemeOptionDto option = safeOption(txn.getOptionId());
        return toTxnDto(txn, option);
    }

    private TransactionDto toTxnDto(Transaction txn, SchemeOptionDto option) {
        return mapper.toTxnDto(txn, option != null ? option.schemeName() : null,
                option != null ? option.optionType() : null);
    }

    private SchemeOptionDto safeOption(Long optionId) {
        try {
            return fundCatalogClient.getOption(optionId);
        } catch (Exception ex) {
            return null;
        }
    }
}
