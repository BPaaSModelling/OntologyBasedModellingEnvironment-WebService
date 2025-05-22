package ch.fhnw.modeller.webservice.dto;

public class PaletteCategoryDto {
    private String id;
    private String idSuffix;
    private String label;
    private Integer orderNumber;
    private boolean hiddenFromPalette;
    private String modelingView;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdSuffix() {
        return idSuffix;
    }

    public void setIdSuffix(String idSuffix) {
        this.idSuffix = idSuffix;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    public boolean isHiddenFromPalette() {
        return hiddenFromPalette;
    }

    public void setHiddenFromPalette(boolean hiddenFromPalette) {
        this.hiddenFromPalette = hiddenFromPalette;
    }

    public String getModelingView() {
        return modelingView;
    }

    public void setModelingView(String modelingView) {
        this.modelingView = modelingView;
    }
}
