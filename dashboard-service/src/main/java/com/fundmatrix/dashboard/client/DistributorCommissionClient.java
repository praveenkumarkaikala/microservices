package com.fundmatrix.dashboard.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** Read-only consumer of distributor-commission-service (owns Distributor, TrailCommission). */
@FeignClient(name = "distributor-commission-service", path = "/api")
public interface DistributorCommissionClient {

    /**
     * Existing monolith-derived route (DistributorController.list) - used to resolve which
     * distributor record belongs to the current DISTRIBUTOR user (matching userId), since no
     * dedicated "distributor by user id" internal contract endpoint exists.
     */
    @GetMapping("/distributors")
    List<DistributorDto> listDistributors();

    /** Existing monolith-derived route (DistributorController.get). */
    @GetMapping("/distributors/{id}")
    DistributorDto getDistributor(@PathVariable Long id);

    /** Existing monolith-derived route (CommissionController.byDistributor). */
    @GetMapping("/commissions/distributor/{distributorId}")
    List<TrailCommissionDto> commissionsByDistributor(@PathVariable Long distributorId);
}
