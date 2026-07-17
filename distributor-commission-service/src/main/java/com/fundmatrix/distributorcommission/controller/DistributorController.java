package com.fundmatrix.distributorcommission.controller;

import com.fundmatrix.distributorcommission.dto.DistributorDto;
import com.fundmatrix.distributorcommission.dto.SaveDistributorRequest;
import com.fundmatrix.distributorcommission.service.DistributorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/distributors")
@Tag(name = "Distributors", description = "Distributor empanelment and lookup")
public class DistributorController {

    private final DistributorService distributorService;

    public DistributorController(DistributorService distributorService) {
        this.distributorService = distributorService;
    }

    @GetMapping
    public List<DistributorDto> list() {
        return distributorService.list();
    }

    @GetMapping("/{id}")
    public DistributorDto get(@PathVariable Long id) {
        return distributorService.get(id);
    }

    @PostMapping
    public ResponseEntity<DistributorDto> create(@Valid @RequestBody SaveDistributorRequest request) {
        return ResponseEntity.ok(distributorService.create(request));
    }

    @PutMapping("/{id}")
    public DistributorDto update(@PathVariable Long id, @Valid @RequestBody SaveDistributorRequest request) {
        return distributorService.update(id, request);
    }
}
