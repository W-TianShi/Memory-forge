package com.memoryforge.dto;

import java.util.List;

public class PdfExportRequest {
    private String html;
    private boolean landscape;
    private String gridType;   // "grid" | "dot" | "iso" | "eng-solid" | "eng-dashed" | "hex" | null
    private String gridColor;  // hex like "#d2d2d2" or null for default
    private boolean autoBlank;  // append a blank page with grid when page count is odd
    private List<Annotation> annotations; // user annotations from ExportWorkshop

    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }

    public boolean isLandscape() { return landscape; }
    public void setLandscape(boolean landscape) { this.landscape = landscape; }

    public String getGridType() { return gridType; }
    public void setGridType(String gridType) { this.gridType = gridType; }

    public String getGridColor() { return gridColor; }
    public void setGridColor(String gridColor) { this.gridColor = gridColor; }

    public boolean isAutoBlank() { return autoBlank; }
    public void setAutoBlank(boolean autoBlank) { this.autoBlank = autoBlank; }

    public List<Annotation> getAnnotations() { return annotations; }
    public void setAnnotations(List<Annotation> annotations) { this.annotations = annotations; }

    public static class Annotation {
        private double x;          // mm from page content left
        private double y;          // mm from page content top
        private double width;      // mm, 0 = auto
        private double height;     // mm, 0 = auto
        private String html;       // annotation content (plain text or simple HTML)
        private double fontSize;   // pt
        private String color;      // hex like "#333333"
        private int pageIndex;     // 0-based page index

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }

        public double getY() { return y; }
        public void setY(double y) { this.y = y; }

        public double getWidth() { return width; }
        public void setWidth(double width) { this.width = width; }

        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }

        public String getHtml() { return html; }
        public void setHtml(String html) { this.html = html; }

        public double getFontSize() { return fontSize; }
        public void setFontSize(double fontSize) { this.fontSize = fontSize; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public int getPageIndex() { return pageIndex; }
        public void setPageIndex(int pageIndex) { this.pageIndex = pageIndex; }
    }
}
