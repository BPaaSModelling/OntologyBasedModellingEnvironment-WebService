package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class NewModelingElementDto {

    private String uuid; // Usato per ID univoco e creazione della shape
    private String label;

    private String type; // "PaletteElement" o "PaletteConnector"
    private String imageURL;
    private String thumbnailURL;

    private int height;
    private int width;
    private boolean hiddenFromPalette;

    private String modelingView; // URI della view, es: http://fhnw.ch/SBM#SimpleBusinessView
    private String paletteCategory; // URI della categoria
    private String parentElement; // URI dell'elemento padre
    private String representedLanguageClass; // URI della classe concettuale (owl:Class)

    private int x;
    private int y;

    private String shapeRepresentsModel;
}
