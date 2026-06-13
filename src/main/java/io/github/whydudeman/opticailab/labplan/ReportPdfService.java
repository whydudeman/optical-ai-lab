package io.github.whydudeman.opticailab.labplan;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportPdfService {

    public byte[] render(LabReport report, String studentEmail) {
        String html = buildHtml(report, studentEmail);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFont(() -> getClass().getResourceAsStream("/fonts/DejaVuSans.ttf"),
                    "DejaVu Sans", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.useFont(() -> getClass().getResourceAsStream("/fonts/DejaVuSans-Bold.ttf"),
                    "DejaVu Sans", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render report PDF", e);
        }
    }

    private String buildHtml(LabReport report, String studentEmail) {
        StringBuilder body = new StringBuilder();
        body.append(section("Цель работы", paragraph(report.objective())));
        if (report.theory() != null && !report.theory().isBlank()) {
            body.append(section("Теоретическая часть", paragraph(report.theory())));
        }
        body.append(section("Оборудование", list(report.equipmentUsed())));
        body.append(section("Ход работы", list(report.procedure())));
        body.append(section("Результаты", paragraph(report.results())));
        body.append(section("Выводы", paragraph(report.conclusions())));
        if (report.questionsDiscussed() != null && !report.questionsDiscussed().isEmpty()) {
            body.append(section("Вопросы, проработанные в ходе работы", list(report.questionsDiscussed())));
        }

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        return """
                <!DOCTYPE html>
                <html><head><meta charset="utf-8"/><style>
                @page { size: A4; margin: 2cm 1.5cm; }
                body { font-family: 'DejaVu Sans', sans-serif; font-size: 11pt; color: #14161b; line-height: 1.5; }
                .title-block { text-align: center; margin-bottom: 1.4cm; }
                .org { font-size: 11pt; color: #444; }
                .doc-kind { font-size: 10pt; letter-spacing: 0.1em; text-transform: uppercase; color: #777; margin-top: 0.6cm; }
                h1 { font-size: 16pt; margin: 0.4cm 0; }
                .meta { font-size: 10pt; color: #555; margin-top: 0.8cm; }
                h2 { font-size: 12pt; border-bottom: 1pt solid #ccc; padding-bottom: 2pt; margin: 0.7cm 0 0.25cm; }
                p { margin: 0; white-space: pre-wrap; }
                ul { margin: 0; padding-left: 0.7cm; }
                li { margin-bottom: 2pt; }
                </style></head><body>
                <div class="title-block">
                  <div class="org">Satbayev University</div>
                  <div class="doc-kind">Отчёт о лабораторной работе</div>
                  <h1>%s</h1>
                  <div class="meta">Студент: %s &#160;&#160;|&#160;&#160; Дата: %s</div>
                </div>
                %s
                </body></html>
                """.formatted(escape(report.title()), escape(studentEmail), date, body);
    }

    private String section(String heading, String content) {
        return "<h2>" + escape(heading) + "</h2>" + content;
    }

    private String paragraph(String text) {
        return "<p>" + escape(text == null ? "" : text) + "</p>";
    }

    private String list(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "<p>—</p>";
        }
        StringBuilder sb = new StringBuilder("<ul>");
        for (String item : items) {
            sb.append("<li>").append(escape(item)).append("</li>");
        }
        return sb.append("</ul>").toString();
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
