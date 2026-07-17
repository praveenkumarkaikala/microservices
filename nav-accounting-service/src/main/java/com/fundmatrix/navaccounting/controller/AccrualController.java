package com.fundmatrix.navaccounting.controller;

import com.fundmatrix.navaccounting.dto.CreateAccrualRequest;
import com.fundmatrix.navaccounting.dto.ExpenseAccrualDto;
import com.fundmatrix.navaccounting.dto.ExpenseComplianceDto;
import com.fundmatrix.navaccounting.dto.ReverseAccrualRequest;
import com.fundmatrix.navaccounting.service.AccrualService;
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
@RequestMapping("/accruals")
@Tag(name = "Expense Accruals", description = "Fund-level expense accrual booking")
public class AccrualController {

    private final AccrualService accrualService;

    public AccrualController(AccrualService accrualService) {
        this.accrualService = accrualService;
    }

    @GetMapping("/scheme/{schemeId}")
    public List<ExpenseAccrualDto> byScheme(@PathVariable Long schemeId) {
        return accrualService.listByScheme(schemeId);
    }

    @GetMapping("/scheme/{schemeId}/compliance")
    public ExpenseComplianceDto compliance(@PathVariable Long schemeId) {
        return accrualService.compliance(schemeId);
    }

    @PostMapping
    public ResponseEntity<ExpenseAccrualDto> create(@Valid @RequestBody CreateAccrualRequest request) {
        return ResponseEntity.ok(accrualService.create(request));
    }

    @PostMapping("/{id}/reverse")
    public ExpenseAccrualDto reverse(@PathVariable Long id, @Valid @RequestBody ReverseAccrualRequest request) {
        return accrualService.reverse(id, request.reason());
    }
}
