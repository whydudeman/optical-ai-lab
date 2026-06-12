package io.github.whydudeman.opticailab.labplan;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/lab-plans")
public class LabPlanController {

    private final LabPlanService labPlanService;
    private final PlanChatService planChatService;

    public LabPlanController(LabPlanService labPlanService, PlanChatService planChatService) {
        this.labPlanService = labPlanService;
        this.planChatService = planChatService;
    }

    @PostMapping
    public LabPlanResponse generate(@Valid @RequestBody LabPlanRequest request, Principal principal) {
        return labPlanService.generate(request, principal.getName());
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@Valid @RequestBody PlanChatRequest request, Principal principal) {
        return Map.of("answer", planChatService.answer(request, principal.getName()));
    }
}
