package com.fundmatrix.fundcatalog.controller;

import com.fundmatrix.fundcatalog.dto.FundSchemeDto;
import com.fundmatrix.fundcatalog.dto.SaveOptionRequest;
import com.fundmatrix.fundcatalog.dto.SaveSchemeRequest;
import com.fundmatrix.fundcatalog.dto.SchemeOptionDto;
import com.fundmatrix.fundcatalog.dto.response.SchemeOptionDetailDto;
import com.fundmatrix.fundcatalog.service.SchemeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/schemes")
@Tag(name = "Fund Schemes", description = "Scheme catalogue and scheme-option administration")
public class SchemeController {

    private final SchemeService schemeService;

    public SchemeController(SchemeService schemeService) {
        this.schemeService = schemeService;
    }

    @GetMapping
    public List<FundSchemeDto> list() {
        return schemeService.list();
    }

    @GetMapping("/{id}")
    public FundSchemeDto get(@PathVariable Long id) {
        return schemeService.get(id);
    }

    @GetMapping("/{id}/options")
    public List<SchemeOptionDto> options(@PathVariable Long id) {
        return schemeService.listOptions(id);
    }

    /** Service-to-service lookup - consumed by folio-transaction, nav-accounting,
     *  distributor-commission and dashboard via FundCatalogClient (FEIGN_CONTRACTS.md). */
    @GetMapping("/options/{optionId}")
    public SchemeOptionDetailDto optionDetail(@PathVariable Long optionId) {
        return schemeService.getOptionDetail(optionId);
    }

    @PostMapping
    public ResponseEntity<FundSchemeDto> create(@Valid @RequestBody SaveSchemeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemeService.create(request));
    }

    @PutMapping("/{id}")
    public FundSchemeDto update(@PathVariable Long id, @Valid @RequestBody SaveSchemeRequest request) {
        return schemeService.update(id, request);
    }

    @PostMapping("/{id}/options")
    public ResponseEntity<SchemeOptionDto> addOption(@PathVariable Long id,
                                                     @Valid @RequestBody SaveOptionRequest request) {
        return ResponseEntity.ok(schemeService.addOption(id, request));
    }
}
