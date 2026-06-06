package com.memoryforge.dto;

import java.util.List;

public class PdfAnnotateRequest {
    private String pdfBase64;
    private List<Annotation> annotations;

    public String getPdfBase64() { return pdfBase64; }
    public void setPdfBase64(String pdfBase64) { this.pdfBase64 = pdfBase64; }

    public List<Annotation> getAnnotations() { return annotations; }
    public void setAnnotations(List<Annotation> annotations) { this.annotations = annotations; }

    public static class Annotation {
        private int pageIndex;
        private double x;        // mm
        private double y;        // mm
        private String html;     // text content
        private double fontSize; // pt
        private String color;    // hex

        public int getPageIndex() { return pageIndex; }
        public void setPageIndex(int pageIndex) { this.pageIndex = pageIndex; }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }

        public double getY() { return y; }
        public void setY(double y) { this.y = y; }

        public String getHtml() { return html; }
        public void setHtml(String html) { this.html = html; }

        public double getFontSize() { return fontSize; }
        public void setFontSize(double fontSize) { this.fontSize = fontSize; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }
}
