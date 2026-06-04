package com.memoryforge.dto;

public class PdfExportRequest {
    private String html;
    private boolean landscape;
    private String gridType;   // "grid" | "dot" | "iso" | "eng-solid" | "eng-dashed" | "hex" | null
    private String gridColor;  // hex like "#d2d2d2" or null for default

    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }

    public boolean isLandscape() { return landscape; }
    public void setLandscape(boolean landscape) { this.landscape = landscape; }

    public String getGridType() { return gridType; }
    public void setGridType(String gridType) { this.gridType = gridType; }

    public String getGridColor() { return gridColor; }
    public void setGridColor(String gridColor) { this.gridColor = gridColor; }
}
