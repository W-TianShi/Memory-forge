package com.memoryforge.controller;

import com.memoryforge.dto.PdfExportRequest;
import com.memoryforge.dto.PdfMergeRequest;
import com.memoryforge.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private static final Logger log = LoggerFactory.getLogger(PdfController.class);
    private final PdfService pdfService;

    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportPdf(@RequestBody PdfExportRequest request) {
        try {
            byte[] pdfBytes = pdfService.generatePdf(
                request.getHtml(), request.isLandscape(),
                request.getGridType(), request.getGridColor(),
                request.isAutoBlank());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename("note.pdf").build());
            headers.setCacheControl("no-cache");

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            log.error("PDF export failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("PDF generation failed: " + e.getMessage()).getBytes());
        }
    }

    @PostMapping("/merge")
    public ResponseEntity<byte[]> mergePdfs(@RequestBody PdfMergeRequest request) {
        try {
            byte[] pdfBytes = pdfService.mergePdfs(request.getItems());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("打印合集.pdf").build());
            headers.setCacheControl("no-cache");

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            log.error("PDF merge failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("PDF merge failed: " + e.getMessage()).getBytes());
        }
    }
}
