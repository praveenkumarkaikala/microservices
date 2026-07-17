package com.fundmatrix.fundcatalog.controller;

import com.fundmatrix.fundcatalog.domain.enums.OptionType;
import com.fundmatrix.fundcatalog.domain.enums.RiskProfile;
import com.fundmatrix.fundcatalog.domain.enums.Role;
import com.fundmatrix.fundcatalog.domain.enums.SchemeCategory;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes enum vocabularies so UI dropdowns stay in sync with the backend.
 *
 * NOTE: the monolith's ReferenceController exposed vocabularies for many domains (KYC, expense,
 * commission, SIP frequency, tax status, mode of holding...) alongside scheme/option enums, all
 * from one shared enum package. Those other enums now live in the services that own that data
 * (compliance-kyc-service, nav-accounting-service, distributor-commission-service, auth-user-
 * service). This module only owns FundScheme/SchemeOption, so it only re-exposes the enum
 * vocabularies it actually owns; each service hosts its own slice of /reference/enums.
 */
@RestController
@RequestMapping("/reference")
@Tag(name = "Reference data", description = "Enum vocabularies for UI dropdowns")
public class ReferenceController {

    @GetMapping("/enums")
    public Map<String, List<String>> enums() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        out.put("roles", names(Role.values()));
        out.put("schemeCategories", names(SchemeCategory.values()));
        out.put("riskProfiles", names(RiskProfile.values()));
        out.put("optionTypes", names(OptionType.values()));
        return out;
    }

    private List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}
