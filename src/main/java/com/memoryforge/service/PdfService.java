package com.memoryforge.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);
    private static final int TIMEOUT_SECONDS = 60;

    public byte[] generatePdf(String html, boolean landscape,
                              String gridType, String gridColor)
            throws IOException, InterruptedException {

        Path htmlFile = null;
        Path pdfFile = null;

        try {
            // 1. Puppeteer generates text PDF (vector quality)
            htmlFile = Files.createTempFile("mf-html-", ".html");
            pdfFile = Files.createTempFile("mf-pdf-", ".pdf");
            Files.writeString(htmlFile, html, StandardCharsets.UTF_8);

            String scriptPath = Path.of("pdf-service", "generate-pdf.js").toAbsolutePath().toString();
            ProcessBuilder pb = new ProcessBuilder(
                "node", scriptPath,
                htmlFile.toAbsolutePath().toString(),
                pdfFile.toAbsolutePath().toString(),
                landscape ? "landscape" : "portrait"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("PDF generation timed out");
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IOException("PDF generation failed (exit " + exitCode + "): " + output);
            }

            byte[] pdfBytes = Files.readAllBytes(pdfFile);

            // 2. Overlay grid with PDFBox (true vector — no browser rasterization)
            if (gridType != null && !gridType.isEmpty()) {
                pdfBytes = overlayGrid(pdfBytes, gridType, gridColor);
            }

            log.info("PDF generated: {} bytes, grid={}", pdfBytes.length, gridType);
            return pdfBytes;

        } finally {
            if (htmlFile != null) {
                try { Files.deleteIfExists(htmlFile); } catch (IOException ignored) {}
            }
            if (pdfFile != null) {
                try { Files.deleteIfExists(pdfFile); } catch (IOException ignored) {}
            }
        }
    }

    // ── Grid overlay ─────────────────────────────────────────────

    private byte[] overlayGrid(byte[] pdfBytes, String gridType, String gridColor) throws IOException {
        PDDocument doc = PDDocument.load(pdfBytes);

        float mmToPt = 72f / 25.4f;

        Color color = parseColor(gridColor);
        if (color == null) {
            switch (gridType) {
                case "grid":       color = new Color(210, 210, 210); break;
                case "dot":        color = new Color(208, 208, 208); break;
                case "iso":        color = new Color(200, 200, 200); break;
                case "eng-solid":
                case "eng-dashed": color = new Color(224, 224, 224); break;
                case "hex":        color = new Color(208, 208, 208); break;
                default:           color = new Color(210, 210, 210);
            }
        }

        for (PDPage page : doc.getPages()) {
            PDRectangle box = page.getMediaBox();
            float w = box.getWidth();
            float h = box.getHeight();

            float step = 5f * mmToPt; // 5 mm in PDF points

            PDPageContentStream cs = new PDPageContentStream(
                doc, page, PDPageContentStream.AppendMode.PREPEND, true, true);

            cs.setStrokingColor(color);
            cs.setLineWidth(0.25f * mmToPt);

            switch (gridType) {
                case "grid" -> drawGrid(cs, w, h, step);
                case "dot"  -> drawDots(cs, w, h, step, 0.3f * mmToPt);
                case "iso"  -> drawIso(cs, w, h, step);
                case "eng-solid", "eng-dashed" ->
                    drawEngGrid(cs, w, h, step, gridType.equals("eng-dashed"), color);
                case "hex"  -> drawHexDots(cs, w, h, step, 0.3f * mmToPt);
            }

            cs.close();
        }

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        doc.save(out);
        doc.close();
        return out.toByteArray();
    }

    private void drawGrid(PDPageContentStream cs, float w, float h,
                          float step) throws IOException {
        for (float x = 0; x <= w; x += step) {
            cs.moveTo(x, 0);
            cs.lineTo(x, h);
        }
        for (float y = 0; y <= h; y += step) {
            cs.moveTo(0, y);
            cs.lineTo(w, y);
        }
        cs.stroke();
    }

    private void drawDots(PDPageContentStream cs, float w, float h,
                          float step, float radius) throws IOException {
        float k = 0.5522847498f;
        float r = radius;
        float kr = k * r;

        for (float x = 0; x <= w + step; x += step) {
            for (float y = 0; y <= h + step; y += step) {
                float cx = Math.min(x, w);
                float cy = Math.min(y, h);
                cs.moveTo(cx + r, cy);
                cs.curveTo(cx + r, cy + kr, cx + kr, cy + r, cx, cy + r);
                cs.curveTo(cx - kr, cy + r, cx - r, cy + kr, cx - r, cy);
                cs.curveTo(cx - r, cy - kr, cx - kr, cy - r, cx, cy - r);
                cs.curveTo(cx + kr, cy - r, cx + r, cy - kr, cx + r, cy);
            }
        }
        cs.fill();
    }

    private void drawIso(PDPageContentStream cs, float w, float h, float step) throws IOException {
        // Matches the old jsPDF isometric grid algorithm exactly.
        // Vertical lines every 'step', plus two sets of diagonal lines
        // that form equilateral-triangle (isometric) cells.
        float cs_val = (float) (Math.sqrt(3) / 2); // cos(30°)

        // vertical lines
        for (float x = 0; x <= w; x += step) {
            cs.moveTo(x, 0);
            cs.lineTo(x, h);
        }

        // drawLine helper: find two boundary intersections for a line
        // defined by y = (k + sign*0.5*x) / cs_val
        float[] signs = {1f, -1f};
        for (float sign : signs) {
            float kMin = (sign > 0) ? -0.5f * w : 0;
            float kMax = (sign > 0) ? cs_val * h : 0.5f * w + cs_val * h;

            for (float k = kMin; k <= kMax; k += step) {
                float[] p1 = null, p2 = null;

                // left edge (x=0): y = k / cs_val
                float y0 = k / cs_val;
                if (y0 >= 0 && y0 <= h) { p1 = new float[]{0, y0}; }

                // right edge (x=w): y = (k + sign*0.5*w) / cs_val
                float yW = (k + sign * 0.5f * w) / cs_val;
                if (yW >= 0 && yW <= h) {
                    if (p1 == null) p1 = new float[]{w, yW};
                    else p2 = new float[]{w, yW};
                }

                // top edge (y=0): x = -sign * 2 * k
                float xT = -sign * 2 * k;
                if (p2 == null && xT >= 0 && xT <= w) {
                    if (p1 == null) p1 = new float[]{xT, 0};
                    else p2 = new float[]{xT, 0};
                }

                // bottom edge (y=h): x = sign * 2 * (cs_val*h - k)
                float xB = sign * 2 * (cs_val * h - k);
                if (p2 == null && xB >= 0 && xB <= w) {
                    p2 = new float[]{xB, h};
                }

                if (p1 != null && p2 != null) {
                    cs.moveTo(p1[0], p1[1]);
                    cs.lineTo(p2[0], p2[1]);
                }
            }
        }
        cs.stroke();
    }

    private void drawEngGrid(PDPageContentStream cs, float w, float h,
                             float step, boolean dashed, Color color) throws IOException {
        Color majorColor = new Color(
            Math.max(0, color.getRed() - 50),
            Math.max(0, color.getGreen() - 50),
            Math.max(0, color.getBlue() - 50));

        // 1mm minor grid
        float oneMm = 72f / 25.4f;
        cs.setStrokingColor(color);
        cs.setLineWidth(0.1f * oneMm);
        for (float x = 0; x <= w; x += oneMm) {
            cs.moveTo(x, 0); cs.lineTo(x, h);
        }
        for (float y = 0; y <= h; y += oneMm) {
            cs.moveTo(0, y); cs.lineTo(w, y);
        }
        cs.stroke();

        // 10mm major grid
        float major = step * 2;
        cs.setStrokingColor(majorColor);
        cs.setLineWidth(0.2f * oneMm);
        if (dashed) cs.setLineDashPattern(new float[]{1, 0.75f}, 0);
        for (float x = 0; x <= w; x += major) {
            cs.moveTo(x, 0); cs.lineTo(x, h);
        }
        for (float y = 0; y <= h; y += major) {
            cs.moveTo(0, y); cs.lineTo(w, y);
        }
        cs.stroke();
        if (dashed) cs.setLineDashPattern(new float[]{}, 0);
    }

    private void drawHexDots(PDPageContentStream cs, float w, float h,
                             float step, float radius) throws IOException {
        float dy = step * (float) Math.sqrt(3) / 2f;
        float k = 0.5522847498f;
        float r = radius;
        float kr = k * r;

        for (int row = -1; row < h / dy + 1; row++) {
            float y = row * dy / 2f;
            float ox = (row % 2) * step / 2f;
            for (int col = -1; col < w / step + 1; col++) {
                float cx = col * step + ox;
                cs.moveTo(cx + r, y);
                cs.curveTo(cx + r, y + kr, cx + kr, y + r, cx, y + r);
                cs.curveTo(cx - kr, y + r, cx - r, y + kr, cx - r, y);
                cs.curveTo(cx - r, y - kr, cx - kr, y - r, cx, y - r);
                cs.curveTo(cx + kr, y - r, cx + r, y - kr, cx + r, y);
            }
        }
        cs.fill();
    }

    private Color parseColor(String hex) {
        if (hex == null || hex.isEmpty()) return null;
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            if (h.length() == 3) {
                h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
            }
            return new Color(
                Integer.parseInt(h.substring(0, 2), 16),
                Integer.parseInt(h.substring(2, 4), 16),
                Integer.parseInt(h.substring(4, 6), 16));
        } catch (Exception e) {
            return null;
        }
    }
}
