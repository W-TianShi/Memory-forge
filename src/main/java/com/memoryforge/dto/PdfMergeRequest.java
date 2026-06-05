package com.memoryforge.dto;

import java.util.List;

public class PdfMergeRequest {
    private List<PdfMergeItem> items;

    public List<PdfMergeItem> getItems() { return items; }
    public void setItems(List<PdfMergeItem> items) { this.items = items; }

    public static class PdfMergeItem {
        private String html;
        private boolean landscape;
        private String gridType;
        private String gridColor;
        private boolean newSheet;
        private boolean blank;   // true = manually inserted blank page
        private String blankHtml; // HTML template for auto-inserted blank page (word table etc.)

        public String getHtml() { return html; }
        public void setHtml(String html) { this.html = html; }

        public boolean isLandscape() { return landscape; }
        public void setLandscape(boolean landscape) { this.landscape = landscape; }

        public String getGridType() { return gridType; }
        public void setGridType(String gridType) { this.gridType = gridType; }

        public String getGridColor() { return gridColor; }
        public void setGridColor(String gridColor) { this.gridColor = gridColor; }

        public boolean isNewSheet() { return newSheet; }
        public void setNewSheet(boolean newSheet) { this.newSheet = newSheet; }

        public boolean isBlank() { return blank; }
        public void setBlank(boolean blank) { this.blank = blank; }

        public String getBlankHtml() { return blankHtml; }
        public void setBlankHtml(String blankHtml) { this.blankHtml = blankHtml; }
    }
}
