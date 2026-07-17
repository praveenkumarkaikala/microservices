package com.fundmatrix.audit.controller;

import com.fundmatrix.audit.dto.AuditLogDto;
import com.fundmatrix.audit.dto.AuditLogRequest;
import com.fundmatrix.audit.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audit/logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<Void> record(@RequestBody AuditLogRequest request) {
        auditService.record(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<AuditLogDto> list(@RequestParam(required = false) String entityType,
                                   @RequestParam(required = false) String recordId) {
        return auditService.list(entityType, recordId);
    }
}
