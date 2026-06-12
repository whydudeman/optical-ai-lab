package io.github.whydudeman.opticailab.labplan;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lab-plans")
public class LabPlanController {

    private final LabPlanService labPlanService;

    public LabPlanController(LabPlanService labPlanService) {
        this.labPlanService = labPlanService;
    }

    @PostMapping
    public LabPlanResponse generate(@Valid @RequestBody LabPlanRequest request) {
        return labPlanService.generate(request);
    }
}
