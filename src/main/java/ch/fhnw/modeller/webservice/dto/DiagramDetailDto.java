package ch.fhnw.modeller.webservice.dto;

import ch.fhnw.modeller.persistence.GlobalVariables;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class DiagramDetailDto {

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
    private String arrowStroke;
    private ModelElementAttributes modelElementAttributes;
    private String fromDiagram;
    private String toDiagram;
    private List<String> containedDiagrams;
    private List<String> otherVisualisationsOfSameLanguageConstruct;

    public DiagramDetailDto() {}

    public static DiagramDetailDto from(String diagramId, Map<String, String> diagramAttributes, ModelElementAttributes modelElementAttributes, String modelElementType, PaletteVisualInformationDto visualInformationDto) {

        DiagramDetailDto dto = new DiagramDetailDto();

        dto.setId(diagramId);
        String[] paletteConstruct = diagramAttributes.get("diagramInstantiatesPaletteConstruct").split("#");
        dto.setPaletteConstruct(GlobalVariables.getNamespaceMap().get(paletteConstruct[0]) + ":" + paletteConstruct[1]);
        dto.setX(Integer.parseInt(diagramAttributes.get("diagramPositionsOnCoordinateX").split("\\^\\^")[0]));
        dto.setY(Integer.parseInt(diagramAttributes.get("diagramPositionsOnCoordinateY").split("\\^\\^")[0]));
        dto.setModelingLanguageConstructInstance(diagramAttributes.get("diagramVisualisesModelingLanguageConstructInstance").split("#")[1]);
        dto.setWidth(Integer.parseInt(diagramAttributes.get("diagramHasWidth").split("\\^\\^")[0]));
        dto.setHeight(Integer.parseInt(diagramAttributes.get("diagramHasHeight").split("\\^\\^")[0]));
        dto.setLabel(diagramAttributes.get("label"));

        if (diagramAttributes.get("diagramRepresentsModel") != null) {
            dto.setDiagramRepresentsModel(diagramAttributes.get("diagramRepresentsModel").split("#")[1]);
        }

        dto.setNote(diagramAttributes.get("diagramHasNote"));

        dto.setModelElementType(modelElementType);

        modelElementAttributes.getValues().forEach(modelElementAttribute -> {
            if ("modelingRelationHasSourceModelingElement".equals(modelElementAttribute.getRelation())) {
                dto.setFromDiagram(modelElementAttribute.getValue());
            }

            if ("modelingRelationHasTargetModelingElement".equals(modelElementAttribute.getRelation())) {
                dto.setToDiagram(modelElementAttribute.getValue());
            }

            if ("modelingContainerContainsModelingLanguageConstruct".equals(modelElementAttribute.getRelation())) {
                dto.addContainedDiagram(modelElementAttribute.getValue());
            }
        });

        if (modelElementAttributes.getReferencingDiagrams() != null && modelElementAttributes.getReferencingDiagrams().size() > 1) {
            List<String> referencingDiagrams = modelElementAttributes.getReferencingDiagrams();
            referencingDiagrams.remove(dto.getId());
            dto.setOtherVisualisationsOfSameLanguageConstruct(referencingDiagrams);
        }

        dto.setModelElementAttributes(modelElementAttributes);

        dto.setImageUrl(visualInformationDto.getImageUrl());
        dto.setFromArrow(visualInformationDto.getFromArrow());
        dto.setToArrow(visualInformationDto.getToArrow());
        dto.setArrowStroke(visualInformationDto.getArrowStroke());

        return dto;
    }

    public void addContainedDiagram(String diagramKey) {
        if (this.containedDiagrams == null) this.containedDiagrams = new ArrayList<>();
        this.containedDiagrams.add(diagramKey);
    }

    public boolean hasOptionalValues() {
        return getDiagramRepresentsModel() != null || getLabel() != null || getNote() != null;
    }
}
