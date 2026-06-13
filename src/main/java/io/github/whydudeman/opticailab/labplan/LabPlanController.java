package io.github.whydudeman.opticailab.labplan;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/lab-plans")
public class LabPlanController {

    private final LabPlanService labPlanService;
    private final PlanChatService planChatService;
    private final LabReportService labReportService;

    public LabPlanController(LabPlanService labPlanService,
                             PlanChatService planChatService,
                             LabReportService labReportService) {
        this.labPlanService = labPlanService;
        this.planChatService = planChatService;
        this.labReportService = labReportService;
    }

    @PostMapping
    public LabPlanResponse generate(@Valid @RequestBody LabPlanRequest request, Principal principal) {
        return labPlanService.generate(request, principal.getName());
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@Valid @RequestBody PlanChatRequest request, Principal principal) {
        return Map.of("answer", planChatService.answer(request, principal.getName()));
    }

    @PostMapping("/{historyId}/report")
    public LabReport generateReport(@PathVariable Long historyId,
                                    @RequestParam(required = false) LlmProvider provider,
                                    Principal principal) {
        return labReportService.generateAndComplete(historyId,
                provider == null ? LlmProvider.ANTHROPIC : provider, principal.getName());
    }

    @GetMapping("/{historyId}/report")
    public LabReport getReport(@PathVariable Long historyId, Principal principal) {
        return labReportService.getReport(historyId, principal.getName());
    }
}
