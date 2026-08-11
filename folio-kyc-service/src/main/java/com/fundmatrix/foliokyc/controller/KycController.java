package com.fundmatrix.foliokyc.controller;

import com.fundmatrix.foliokyc.domain.enums.KycStatus;
import com.fundmatrix.foliokyc.dto.KycRecordDto;
import com.fundmatrix.foliokyc.dto.KycStatusDto;
import com.fundmatrix.foliokyc.dto.SubmitKycRequest;
import com.fundmatrix.foliokyc.dto.UpdateKycRequest;
import com.fundmatrix.foliokyc.dto.UpdateKycStatusRequest;
import com.fundmatrix.foliokyc.service.KycService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/kyc")
@Tag(name = "KYC", description = "KYC capture and verification")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @GetMapping
    public ResponseEntity<List<KycRecordDto>> getKycList(@RequestParam(required = false) KycStatus status) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(kycService.getkycList(status));
      
    }


    @GetMapping("/investor/{investorId}")
    public ResponseEntity<KycRecordDto> forInvestor(@PathVariable Long investorId) {
    	  return ResponseEntity.status(HttpStatus.ACCEPTED).body(kycService.kycForInvestor(investorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KycRecordDto> getByIdKyc(@PathVariable long id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(kycService.getKycById(id));
    }
    
    @GetMapping("/mine")
    public KycRecordDto mine() {
        return kycService.mine();
    }

  
    @PostMapping
    public ResponseEntity<KycRecordDto> submit(@Valid @RequestBody SubmitKycRequest request) {
        return ResponseEntity.ok(kycService.createKyc(request));
    }

  
    @PatchMapping("/{id}/status")
    public ResponseEntity<KycRecordDto> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateKycStatusRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(kycService.updateStatus(id, request.kycStatus()));
    }

    @GetMapping("/status/{investorId}")
    public ResponseEntity<KycStatusDto>  status(@PathVariable Long investorId) {
         return ResponseEntity.status(HttpStatus.ACCEPTED).body(kycService.kycStatusFor(investorId));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<KycRecordDto> reneawalKyc(@RequestBody UpdateKycRequest request,@PathVariable long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kycService.renuewalKyc(request,id));
    }
}
