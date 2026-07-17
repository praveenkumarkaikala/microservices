package com.fundmatrix.distributorcommission.controller;

import com.fundmatrix.distributorcommission.dto.ComputeCommissionRequest;
import com.fundmatrix.distributorcommission.dto.TrailCommissionDto;
import com.fundmatrix.distributorcommission.service.CommissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/commissions")
@Tag(name = "Commissions", description = "Distributor trail commission computation and payout")
public class CommissionController {

    private final CommissionService commissionService;

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @GetMapping("/distributor/{distributorId}")
    public List<TrailCommissionDto> byDistributor(@PathVariable Long distributorId) {
        return commissionService.listByDistributor(distributorId);
    }

    @GetMapping("/mine")
    public List<TrailCommissionDto> mine() {
        return commissionService.listForCurrentDistributor();
    }

    @PostMapping("/compute")
    public ResponseEntity<TrailCommissionDto> compute(@Valid @RequestBody ComputeCommissionRequest request) {
        return ResponseEntity.ok(commissionService.compute(request));
    }

    @PostMapping("/{id}/approve")
    public TrailCommissionDto approve(@PathVariable Long id) {
        return commissionService.approve(id);
    }

    @PostMapping("/{id}/pay")
    public TrailCommissionDto pay(@PathVariable Long id) {
        return commissionService.pay(id);
    }
}
