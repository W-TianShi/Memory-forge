package com.memoryforge.service;

import com.memoryforge.dto.PdfAnnotateRequest;
import com.memoryforge.dto.PdfExportRequest;
import com.memoryforge.dto.PdfMergeRequest;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);
    private static final int TIMEOUT_SECONDS = 60;

    public byte[] generatePdf(String html, boolean landscape,
                              String gridType, String gridColor)
            throws IOException, InterruptedException {
        return generatePdf(html, landscape, gridType, gridColor, false, null);
    }

    public byte[] generatePdf(String html, boolean landscape,
                              String gridType, String gridColor,
                              boolean autoBlank)
            throws IOException, InterruptedException {
        return generatePdf(html, landscape, gridType, gridColor, autoBlank, null);
    }

    public byte[] generatePdf(String html, boolean landscape,
                              String gridType, String gridColor,
                              boolean autoBlank,
                              List<PdfExportRequest.Annotation> annotations)
            throws IOException, InterruptedException {

        Path htmlFile = null;
        Path pdfFile = null;

        try {
            // Inject annotations into HTML before Puppeteer rendering
            if (annotations != null && !annotations.isEmpty()) {
                html = injectAnnotations(html, annotations);
            }

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

            // 3. Auto-blank page for duplex printing: if page count is odd,
            //    append a blank page with the same grid template.
            //    Falls back to standard grid when no grid mode is active.
            if (autoBlank) {
                try (PDDocument doc = PDDocument.load(pdfBytes)) {
                    if (doc.getNumberOfPages() % 2 != 0) {
                        String bgType = (gridType != null && !gridType.isEmpty()) ? gridType : "grid";
                        String bgColor = (gridType != null && !gridType.isEmpty()) ? gridColor : null;
                        byte[] blankWithGrid = createBlankPagePdf(bgType, bgColor);
                        pdfBytes = appendPages(pdfBytes, blankWithGrid);
                        log.info("Auto-blank page appended (was {} pages, grid={})", doc.getNumberOfPages(), bgType);
                    }
                }
            }

            log.info("PDF generated: {} bytes, grid={}, autoBlank={}", pdfBytes.length, gridType, autoBlank);
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

    /** Append one PDF after another using PDFBox. */
    private byte[] appendPages(byte[] base, byte[] toAppend) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.addSource(new ByteArrayInputStream(base));
        merger.addSource(new ByteArrayInputStream(toAppend));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        merger.setDestinationStream(out);
        merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
        return out.toByteArray();
    }

    // ── Merge PDFs for print queue ────────────────────────────────

    public byte[] mergePdfs(List<PdfMergeRequest.PdfMergeItem> items)
            throws IOException, InterruptedException {

        List<byte[]> pdfs = new ArrayList<>();
        int cumulativePages = 0;
        byte[] plainBlank = null;

        for (PdfMergeRequest.PdfMergeItem item : items) {
            if (item.isBlank()) {
                if (plainBlank == null) plainBlank = createBlankPagePdf(null, null);
                pdfs.add(plainBlank);
                cumulativePages++;
                continue;
            }

            byte[] pdfBytes = generatePdf(item.getHtml(), item.isLandscape(),
                    item.getGridType(), item.getGridColor());

            int pageCount;
            try (PDDocument doc = PDDocument.load(pdfBytes)) {
                pageCount = doc.getNumberOfPages();
            }

            // Per-item odd-page fill: if this doc has odd pages and the
            // user opted in, append a blank page.
            // - Has blankHtml (word paper) → generate from that HTML
            // - Has grid (note) → blank page with same grid template
            // - Neither → plain blank A4 page
            if (item.isNewSheet() && pageCount % 2 != 0) {
                byte[] blankPage;
                if (item.getBlankHtml() != null && !item.getBlankHtml().isEmpty()) {
                    blankPage = generatePdf(item.getBlankHtml(), item.isLandscape(), null, null);
                    log.info("[merge] odd pages ({}), appending blank word table", pageCount);
                } else {
                    String bgType = (item.getGridType() != null && !item.getGridType().isEmpty())
                            ? item.getGridType() : null;
                    String bgColor = bgType != null ? item.getGridColor() : null;
                    blankPage = createBlankPagePdf(bgType, bgColor);
                    log.info("[merge] odd pages ({}), appending blank page grid={} color={}",
                            pageCount, bgType, bgColor);
                }
                pdfBytes = appendPages(pdfBytes, blankPage);
                pageCount++;
            }

            pdfs.add(pdfBytes);
            cumulativePages += pageCount;
        }

        PDFMergerUtility merger = new PDFMergerUtility();
        for (byte[] pdf : pdfs) {
            merger.addSource(new ByteArrayInputStream(pdf));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        merger.setDestinationStream(out);
        merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());

        log.info("Merged PDF: {} items → {} pages → {} bytes",
                items.size(), cumulativePages, out.size());
        return out.toByteArray();
    }

    /** Create a single blank A4 page PDF, optionally with a grid overlay. */
    private byte[] createBlankPagePdf(String gridType, String gridColor) throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage(PDRectangle.A4));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doc.save(bos);
        doc.close();
        byte[] bytes = bos.toByteArray();
        if (gridType != null && !gridType.isEmpty()) {
            bytes = overlayGrid(bytes, gridType, gridColor);
        }
        return bytes;
    }

    // ── Annotation injection ─────────────────────────────────────

    /**
     * Inject user annotations from ExportWorkshop into the HTML before
     * Puppeteer rendering. Each annotation becomes an absolutely-positioned
     * {@code <div>} inside the corresponding page container.
     *
     * @param html        the full export HTML document
     * @param annotations list of annotations with pageIndex and mm coordinates
     * @return modified HTML with annotation divs injected
     */
    private String injectAnnotations(String html, List<PdfExportRequest.Annotation> annotations) {
        // Find all content-area page containers
        List<String> pageContainers = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "<div[^>]*class=\"[^\"]*content-area[^\"]*\"[^>]*>"
        ).matcher(html);

        while (m.find()) {
            pageContainers.add(m.group());
        }

        if (pageContainers.isEmpty()) {
            // No content-area divs found — treat body as single page
            // Insert annotations right after <body>
            int bodyIdx = html.indexOf("<body>");
            if (bodyIdx >= 0) {
                StringBuilder sb = new StringBuilder(html);
                int insertAt = bodyIdx + 6; // after "<body>"
                for (PdfExportRequest.Annotation ann : annotations) {
                    if (ann.getPageIndex() == 0) {
                        sb.insert(insertAt, buildAnnotationDiv(ann));
                    }
                }
                html = sb.toString();
            }
            return html;
        }

        // Annotations grouped by pageIndex
        java.util.Map<Integer, List<PdfExportRequest.Annotation>> byPage = new java.util.LinkedHashMap<>();
        for (PdfExportRequest.Annotation ann : annotations) {
            byPage.computeIfAbsent(ann.getPageIndex(), k -> new ArrayList<>()).add(ann);
        }

        // Inject after each matching page container opening tag
        StringBuilder result = new StringBuilder(html);
        int offset = 0; // track shifts due to insertions

        for (int i = 0; i < pageContainers.size(); i++) {
            List<PdfExportRequest.Annotation> pageAnns = byPage.get(i);
            if (pageAnns == null || pageAnns.isEmpty()) continue;

            // Find this container tag's position in the current result
            String tag = pageContainers.get(i);
            int tagIdx = result.indexOf(tag, offset);
            if (tagIdx < 0) continue;

            int insertAt = tagIdx + tag.length(); // right after the opening tag

            StringBuilder annsHtml = new StringBuilder();
            for (PdfExportRequest.Annotation ann : pageAnns) {
                annsHtml.append(buildAnnotationDiv(ann));
            }

            result.insert(insertAt, annsHtml.toString());
            offset = insertAt + annsHtml.length();
        }

        return result.toString();
    }

    /**
     * Build a single annotation div with absolute positioning in mm units.
     */
    private String buildAnnotationDiv(PdfExportRequest.Annotation ann) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"");
        sb.append("position:absolute;");
        sb.append("left:").append(ann.getX()).append("mm;");
        sb.append("top:").append(ann.getY()).append("mm;");
        sb.append("font-size:").append(ann.getFontSize()).append("pt;");
        sb.append("color:").append(escHtml(ann.getColor())).append(";");
        if (ann.getWidth() > 0) sb.append("width:").append(ann.getWidth()).append("mm;");
        sb.append("z-index:10;");
        sb.append("white-space:pre-wrap;word-break:break-word;");
        sb.append("\">");
        String content = ann.getHtml();
        if (content != null && !content.isEmpty()) {
            sb.append(content);
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
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

    // ── PDF Preview & Annotate (ExportWorkshop) ──────────────────

    /**
     * Split a PDF into per-page PDFs (base64) for vector-quality iframe preview.
     * Each page keeps its original vector content — no rasterization.
     */
    public List<Map<String, Object>> previewPdf(String pdfBase64) throws IOException {
        byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);
        List<Map<String, Object>> pages = new ArrayList<>();

        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            int pageCount = doc.getNumberOfPages();

            for (int i = 0; i < pageCount; i++) {
                // Extract single page into its own PDF document
                PDDocument pageDoc = new PDDocument();
                pageDoc.addPage(doc.getPage(i));

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                pageDoc.save(bos);
                pageDoc.close();

                String pageBase64 = Base64.getEncoder().encodeToString(bos.toByteArray());

                PDRectangle box = doc.getPage(i).getMediaBox();
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("pageIndex", i);
                info.put("width", box.getWidth());
                info.put("height", box.getHeight());
                info.put("pdfBase64", pageBase64);
                pages.add(info);
            }
        }
        return pages;
    }

    /**
     * Write annotations directly into a PDF using PDFBox.
     * Text is drawn as vector content on each page, preserving the
     * original page content underneath.
     *
     * Font strategy: tries to load a CJK TrueType font for Chinese text;
     * falls back to PDType1Font.HELVETICA (ASCII only) if no CJK font found.
     */
    public byte[] annotatePdf(String pdfBase64, List<PdfAnnotateRequest.Annotation> annotations)
            throws IOException {

        byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);

        // Group annotations by page
        Map<Integer, List<PdfAnnotateRequest.Annotation>> byPage = new LinkedHashMap<>();
        for (PdfAnnotateRequest.Annotation ann : annotations) {
            byPage.computeIfAbsent(ann.getPageIndex(), k -> new ArrayList<>()).add(ann);
        }

        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            // Try to load a CJK font for Chinese text support
            java.io.File cjkFontFile = findCjkFont();
            float mmToPt = 72f / 25.4f;

            for (Map.Entry<Integer, List<PdfAnnotateRequest.Annotation>> entry : byPage.entrySet()) {
                int pageIdx = entry.getKey();
                if (pageIdx < 0 || pageIdx >= doc.getNumberOfPages()) continue;

                PDPage page = doc.getPage(pageIdx);
                PDRectangle box = page.getMediaBox();
                float pageH = box.getHeight();

                PDPageContentStream cs = new PDPageContentStream(
                    doc, page, PDPageContentStream.AppendMode.APPEND, true, true);

                for (PdfAnnotateRequest.Annotation ann : entry.getValue()) {
                    String text = ann.getHtml();
                    if (text == null || text.isEmpty()) continue;
                    // Strip HTML tags for plain text
                    text = text.replaceAll("<[^>]*>", "").trim();
                    if (text.isEmpty()) continue;

                    // Position: convert mm → PDF points, flip Y axis
                    float x = (float) (ann.getX() * mmToPt);
                    float y = pageH - (float) (ann.getY() * mmToPt) - (float) (ann.getFontSize() * 1.2f);

                    // Parse color
                    Color color = parseColor(ann.getColor());
                    if (color == null) color = Color.BLACK;
                    cs.setNonStrokingColor(color);

                    float fontSize = (float) ann.getFontSize();

                    if (cjkFontFile != null) {
                        // Use embedded CJK TTF font
                        PDType0Font font = PDType0Font.load(doc, cjkFontFile);
                        cs.setFont(font, fontSize);
                    } else {
                        // Fallback: built-in Helvetica (ASCII only)
                        cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, fontSize);
                    }

                    cs.beginText();
                    cs.newLineAtOffset(x, y);
                    cs.showText(text);
                    cs.endText();
                }

                cs.close();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            doc.close();
            return out.toByteArray();
        }
    }

    /**
     * Find a CJK TrueType font file on the system.
     * Searches common Windows font locations for SimSun, SimHei, or Microsoft YaHei.
     * Returns null if no suitable font found (caller falls back to Helvetica).
     */
    private java.io.File findCjkFont() {
        String[] searchPaths = {
            "C:/Windows/Fonts/simsun.ttf",
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/msyh.ttf",
            "C:/Windows/Fonts/simsun.ttc",
        };
        for (String path : searchPaths) {
            java.io.File f = new java.io.File(path);
            if (f.exists() && f.canRead()) return f;
        }
        // Check user's home font directory
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            java.io.File localFonts = new java.io.File(userHome, ".fonts");
            if (localFonts.isDirectory()) {
                java.io.File[] files = localFonts.listFiles((dir, name) ->
                    name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".ttc"));
                if (files != null && files.length > 0) return files[0];
            }
        }
        return null;
    }
}
