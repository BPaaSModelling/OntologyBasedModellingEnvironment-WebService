package ch.fhnw.modeller.webservice.dto;

import ch.fhnw.modeller.persistence.GlobalVariables;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
public class DiagramDetailDto {

    private List<ModelElementAttribute> modelElementAttributes;

    private String id;
    private String paletteConstruct;
    private int x;
    private int y;
    private String modelingLanguageConstructInstance;
    private int width;
    private int height;
    private String diagramRepresentsModel;
    private String note;
    private String label;
    private String modelElementType;
    private String imageUrl;
    private String fromArrow;
    private String toArrow;

    public DiagramDetailDto() {}

    public static DiagramDetailDto from(String diagramId, Map<String, String> diagramAttributes, Map<String, String> modelElementAttributes, String modelElementType, PaletteVisualInformationDto visualInformationDto) {

        DiagramDetailDto dto = new DiagramDetailDto();

        dto.setId(diagramId);
        String[] paletteConstruct = diagramAttributes.get("diagramInstantiatesPaletteConstruct").split("#");
        dto.setPaletteConstruct(GlobalVariables.getNamespaceMap().get(paletteConstruct[0]) + ":" + paletteConstruct[1]);
        dto.setX(Integer.parseInt(diagramAttributes.get("diagramPositionsOnCoordinateX").split("\\^\\^")[0]));
        dto.setY(Integer.parseInt(diagramAttributes.get("diagramPositionsOnCoordinateY").split("\\^\\^")[0]));
        dto.setModelingLanguageConstructInstance(diagramAttributes.get("diagramVisualisesModelingLanguageConstructInstance").split("#")[1]);
        dto.setWidth(Integer.parseInt(diagramAttributes.get("diagramHasWidth").split("\\^\\^")[0]));
        dto.setHeight(Integer.parseInt(diagramAttributes.get("diagramHasLength").split("\\^\\^")[0]));
        dto.setLabel(diagramAttributes.get("label"));

        if (diagramAttributes.get("diagramRepresentsModel") != null) {
            dto.setDiagramRepresentsModel(diagramAttributes.get("diagramRepresentsModel").split("#")[1]);
        }

        dto.setNote(diagramAttributes.get("diagramHasNote"));

        dto.setModelElementType(modelElementType);

        List<ModelElementAttribute> attributes = new ArrayList<>();

        modelElementAttributes.forEach((key, value) -> {
            ModelElementAttribute attribute = new ModelElementAttribute();
            attribute.setRelationPrefix(key.split("#")[0]);
            attribute.setRelation(key.split("#")[1]);
            if (value != null) {
                attribute.setValuePrefix(value.split("#")[0]);
                attribute.setValue(value.split("#")[1]);
            }

            attributes.add(attribute);
        });

        dto.setModelElementAttributes(attributes);

        dto.setImageUrl(visualInformationDto.getImageUrl());
        dto.setFromArrow(visualInformationDto.getFromArrow());
        dto.setToArrow(visualInformationDto.getToArrow());

        return dto;
    }

    public boolean hasOptionalValues() {
        return getDiagramRepresentsModel() != null || getLabel() != null || getNote() != null;
    }
}
