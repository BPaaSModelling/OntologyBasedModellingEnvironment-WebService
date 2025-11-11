package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class ConnectorCreationDto {
    private String uuid;
    private String label;
    private String modelingView;
    private String paletteCategory;
    private String relatedLanguageConstruct;
    private String arrowStroke;
    private String toArrow;
    private boolean arrowBaseUsesImage;
    private int width;
    private int height;
    private String imageURL;
    private String thumbnailURL;
    private boolean hiddenFromPalette;
}

