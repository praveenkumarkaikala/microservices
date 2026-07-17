package com.fundmatrix.transaction.controller;

import com.fundmatrix.transaction.client.FundCatalogClient;
import com.fundmatrix.transaction.common.exception.ResourceNotFoundException;
import com.fundmatrix.transaction.domain.TransactionFlag;
import com.fundmatrix.transaction.domain.enums.FlagStatus;
import com.fundmatrix.transaction.dto.ReviewFlagRequest;
import com.fundmatrix.transaction.dto.SchemeOptionDto;
import com.fundmatrix.transaction.dto.TransactionFlagDto;
import com.fundmatrix.transaction.repository.TransactionFlagRepository;
import com.fundmatrix.transaction.service.Mapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Endpoints for compliance-service's flag review workflow (GET/PATCH /transactions/flags**,
 * per FEIGN_CONTRACTS.md's "transaction-service" section), also consumed by dashboard-service.
 * TransactionFlag stays a local aggregate (its Transaction relation is intra-service), so only
 * the scheme name shown in the DTO needs a fund-catalog-service lookup.
 */
@RestController
@RequestMapping("/transactions/flags")
@Tag(name = "Transaction Flags", description = "Compliance review of high-value transaction flags")
public class TransactionFlagController {

    private final TransactionFlagRepository flagRepository;
    private final FundCatalogClient fundCatalogClient;
    private final Mapper mapper;

    public TransactionFlagController(TransactionFlagRepository flagRepository,
                                     FundCatalogClient fundCatalogClient, Mapper mapper) {
        this.flagRepository = flagRepository;
        this.fundCatalogClient = fundCatalogClient;
        this.mapper = mapper;
    }

    /**
     * @Transactional is required here (not just on write methods) because TransactionFlag.transaction
     * is LAZY and open-in-view is disabled - safeSchemeName()'s flag.getTransaction().getOptionId()
     * must run while the Hibernate session from the repository query is still open.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public List<TransactionFlagDto> flags(@RequestParam(required = false) String status) {
        List<TransactionFlag> flags = status != null
                ? flagRepository.findByStatusOrderByCreatedDateDesc(FlagStatus.valueOf(status))
                : flagRepository.findAllByOrderByCreatedDateDesc();
        return flags.stream().map(f -> mapper.toFlagDto(f, safeSchemeName(f))).toList();
    }

    @PatchMapping("/{id}/review")
    @Transactional
    public TransactionFlagDto review(@PathVariable Long id, @Valid @RequestBody ReviewFlagRequest request) {
        TransactionFlag flag = flagRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("TransactionFlag", id));
        flag.setStatus(FlagStatus.valueOf(request.status()));
        flag.setReviewedDate(Instant.now());
        flag = flagRepository.save(flag);
        return mapper.toFlagDto(flag, safeSchemeName(flag));
    }

    private String safeSchemeName(TransactionFlag flag) {
        try {
            SchemeOptionDto option = fundCatalogClient.getOption(flag.getTransaction().getOptionId());
            return option != null ? option.schemeName() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
