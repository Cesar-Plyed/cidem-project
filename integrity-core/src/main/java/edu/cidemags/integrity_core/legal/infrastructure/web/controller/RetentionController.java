package edu.cidemags.integrity_core.legal.infrastructure.web.controller;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.application.service.DataMinimizationService;
import edu.cidemags.integrity_core.legal.application.service.DataRetentionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Politica de retencion y minimizacion (specs 38 y 41). */
@RestController
@RequestMapping("/api/legal")
public class RetentionController {

    private final DataRetentionService retentionService;
    private final DataMinimizationService minimizationService;

    public RetentionController(DataRetentionService retentionService,
                               DataMinimizationService minimizationService) {
        this.retentionService = retentionService;
        this.minimizationService = minimizationService;
    }

    @GetMapping("/data-retention")
    public LegalViews.RetentionInfo retention() {
        return retentionService.getRetentionInfo();
    }

    @GetMapping("/data-types")
    public LegalViews.DataTypesInfo dataTypes() {
        return minimizationService.getDataTypesInfo();
    }
}
