package com.fundmatrix.foliokyc.controller;

import com.fundmatrix.foliokyc.dto.CreateFolioRequest;
import com.fundmatrix.foliokyc.dto.FolioDto;
import com.fundmatrix.foliokyc.dto.FolioHoldingDto;
import com.fundmatrix.foliokyc.dto.UpdateFolioStatusRequest;
import com.fundmatrix.foliokyc.service.FolioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/folios")
@Tag(name = "Investor Folios", description = "Folio lifecycle and holdings")
public class FolioController {

    private final FolioService folioService;

    public FolioController(FolioService folioService) {
        this.folioService = folioService;
    }

    @GetMapping
    public List<FolioDto> list() {
        return folioService.listForCurrentUser();
    }

    @GetMapping("/{id}")
    public FolioDto get(@PathVariable Long id) {
        return folioService.get(id);
    }

    @GetMapping("/{id}/holdings")
    public List<FolioHoldingDto> holdings(@PathVariable Long id) {
        return folioService.holdings(id);
    }

    @PostMapping
    public ResponseEntity<FolioDto> create(@Valid @RequestBody CreateFolioRequest request) {
        return ResponseEntity.ok(folioService.create(request));
    }

    @PatchMapping("/{id}/status")
    public FolioDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateFolioStatusRequest request) {
        return folioService.updateStatus(id, request.status());
    }
}
