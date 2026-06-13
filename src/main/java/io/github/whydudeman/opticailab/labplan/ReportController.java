package io.github.whydudeman.opticailab.labplan;

import io.github.whydudeman.opticailab.history.CustomReport;
import io.github.whydudeman.opticailab.history.CustomReportRepository;
import io.github.whydudeman.opticailab.history.PlanHistory;
import io.github.whydudeman.opticailab.history.PlanHistoryRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/lab-plans/{historyId}")
public class ReportController {

    private final LabReportService labReportService;
    private final ReportPdfService reportPdfService;
    private final PlanHistoryRepository planHistoryRepository;
    private final CustomReportRepository customReportRepository;

    public ReportController(LabReportService labReportService,
                            ReportPdfService reportPdfService,
                            PlanHistoryRepository planHistoryRepository,
                            CustomReportRepository customReportRepository) {
        this.labReportService = labReportService;
        this.reportPdfService = reportPdfService;
        this.planHistoryRepository = planHistoryRepository;
        this.customReportRepository = customReportRepository;
    }

    @GetMapping("/report.pdf")
    public ResponseEntity<Resource> reportPdf(@PathVariable Long historyId, Principal principal) {
        requireOwned(historyId, principal);
        LabReport report = labReportService.getReport(historyId, principal.getName());
        byte[] pdf = reportPdfService.render(report, principal.getName());
        return download(pdf, "report-" + historyId + ".pdf", MediaType.APPLICATION_PDF, true);
    }

    @PostMapping("/custom-report")
    public Map<String, String> uploadCustomReport(@PathVariable Long historyId,
                                                  @RequestParam("file") MultipartFile file,
                                                  Principal principal) {
        requireOwned(historyId, principal);
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read file", e);
        }
        CustomReport report = customReportRepository.findByHistoryId(historyId)
                .orElseGet(() -> new CustomReport(historyId, file.getOriginalFilename(),
                        resolveContentType(file), data));
        report.setFilename(file.getOriginalFilename());
        report.setContentType(resolveContentType(file));
        report.setData(data);
        report.setUploadedAt(Instant.now());
        customReportRepository.save(report);
        return Map.of("filename", report.getFilename());
    }

    @GetMapping("/custom-report")
    public ResponseEntity<Resource> downloadCustomReport(@PathVariable Long historyId, Principal principal) {
        requireOwned(historyId, principal);
        CustomReport report = customReportRepository.findByHistoryId(historyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return download(report.getData(), report.getFilename(),
                MediaType.parseMediaType(report.getContentType()), true);
    }

    @GetMapping("/custom-report/info")
    public ResponseEntity<Map<String, String>> customReportInfo(@PathVariable Long historyId, Principal principal) {
        requireOwned(historyId, principal);
        return customReportRepository.findByHistoryId(historyId)
                .map(report -> ResponseEntity.ok(Map.of("filename", report.getFilename())))
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<Resource> download(byte[] data, String filename, MediaType type, boolean inline) {
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(type)
                .body(new ByteArrayResource(data));
    }

    private String resolveContentType(MultipartFile file) {
        return file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType();
    }

    private PlanHistory requireOwned(Long historyId, Principal principal) {
        return planHistoryRepository.findByIdAndUserEmail(historyId, principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    }
}
