package br.com.fotocastro.template;

/**
 * Ícones SVG disponíveis para templates de venda.
 * Temática: Fotografia e produtos relacionados.
 */
public enum TemplateIcon {
    
    // Ícones Material Design adaptados para fotografia
    PHOTO("photo", "Foto", "#3b82f6", 
        "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"),
    
    FRAME("frame", "Moldura", "#10b981",
        "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zm-5.04-6.71l-2.75 3.54-1.96-2.36L6.5 17h11l-3.54-4.71z"),
    
    DOCUMENT("document", "Documento", "#f59e0b",
        "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"),
    
    POLAROID("polaroid", "Polaroid", "#ec4899",
        "M21 3H3C2 3 1 4 1 5v14c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V5c0-1-1-2-2-2zM5 17l3.5-4.5 2.5 3.01L14.5 11l4.5 6H5z");
    
    private final String svgName;
    private final String label;
    private final String color;
    private final String pathData;
    
    TemplateIcon(String svgName, String label, String color, String pathData) {
        this.svgName = svgName;
        this.label = label;
        this.color = color;
        this.pathData = pathData;
    }
    
    public String getSvgName() {
        return svgName;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getColor() {
        return color;
    }
    
    public String getPathData() {
        return pathData;
    }
    
    public String getDisplay() {
        return label;
    }
    
    /**
     * Retorna o caminho do SVG (para compatibilidade, mas não usado)
     */
    public String getSvgPath() {
        return "/icons/templates/" + svgName + ".svg";
    }
    
    @Override
    public String toString() {
        return label;
    }
}