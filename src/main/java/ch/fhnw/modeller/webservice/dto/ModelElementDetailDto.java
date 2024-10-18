package ch.fhnw.modeller.webservice.dto;

import ch.fhnw.modeller.persistence.GlobalVariables;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ModelElementDetailDto {

    private String id;
    private String paletteConstruct;
    private String modellingLanguageConstruct;
    private int x;
    private int y;
    private String modelingLanguageConstructInstance;
    private int width;
    private int height;
    private String shapeRepresentsModel;
    private String note;
    private String label;
    private String modelElementType;
    private String imageUrl;
    private String fromArrow;
    private String toArrow;
    private String arrowStroke;
    private AbstractElementAttributes abstractElementAttributes;
    private String fromShape;
    private String toShape;
    private List<String> containedShapes;
    private List<String> otherVisualisationsOfSameLanguageConstruct;
    private String xCoorDobotMagician;
    private String yCoorDobotMagician;
    private String zCoorDobotMagician;
    private boolean sucCupOnDobotMagician;
    private boolean sucCupOffDobotMagician;
    private boolean calDobotMagician;

    public ModelElementDetailDto() {}

    public static ModelElementDetailDto from(String shapeId, Map<String, String> shapeAttributes, AbstractElementAttributes abstractElementAttributes, String modelElementType, PaletteVisualInformationDto visualInformationDto) {

        ModelElementDetailDto dto = new ModelElementDetailDto();

        dto.setId(shapeId);
        String[] paletteConstruct = shapeAttributes.get("shapeInstantiatesPaletteConstruct").split("#");
        dto.setPaletteConstruct(GlobalVariables.getNamespaceMap().get(paletteConstruct[0]) + ":" + paletteConstruct[1]);
        dto.setModellingLanguageConstruct(abstractElementAttributes.getModellingLanguageConstruct());
        dto.setX(Integer.parseInt(shapeAttributes.get("shapePositionsOnCoordinateX").split("\\^\\^")[0]));
        dto.setY(Integer.parseInt(shapeAttributes.get("shapePositionsOnCoordinateY").split("\\^\\^")[0]));
        dto.setModelingLanguageConstructInstance(shapeAttributes.get("shapeVisualisesConceptualElement").split("#")[1]);
        dto.setWidth(Integer.parseInt(shapeAttributes.get("shapeHasWidth").split("\\^\\^")[0]));
        dto.setHeight(Integer.parseInt(shapeAttributes.get("shapeHasHeight").split("\\^\\^")[0]));
        dto.setLabel(shapeAttributes.get("label"));

        if (shapeAttributes.get("shapeRepresentsModel") != null) {
            dto.setShapeRepresentsModel(shapeAttributes.get("shapeRepresentsModel").split("#")[1]);
        }

        dto.setNote(shapeAttributes.get("shapeHasNote"));
        dto.setxCoorDobotMagician(shapeAttributes.get("shapeHasXCoorDobotMagician"));
        dto.setyCoorDobotMagician(shapeAttributes.get("shapeHasYCoorDobotMagician"));
        dto.setzCoorDobotMagician(shapeAttributes.get("shapeHasZCoorDobotMagician"));
        dto.setSucCupOnDobotMagician(Boolean.parseBoolean(shapeAttributes.get("shapeHasSucCupOnDobotMagician")));
        dto.setSucCupOffDobotMagician(Boolean.parseBoolean(shapeAttributes.get("shapeHasSucCupOffDobotMagician")));
        dto.setCalDobotMagician(Boolean.parseBoolean(shapeAttributes.get("shapeHasCalDobotMagician")));

        dto.setModelElementType(modelElementType);

        abstractElementAttributes.getValues().forEach(modelElementAttribute -> {
            if ("modelingRelationHasSourceModelingElement".equals(modelElementAttribute.getRelation())) {
                dto.setFromShape(modelElementAttribute.getValue().split(":")[1]);
            }

            if ("modelingRelationHasTargetModelingElement".equals(modelElementAttribute.getRelation())) {
                dto.setToShape(modelElementAttribute.getValue().split(":")[1]);
            }

            if ("modelingContainerContainsModelingLanguageConstruct".equals(modelElementAttribute.getRelation())) {
                dto.addContainedShape(modelElementAttribute.getValue().split(":")[1]);
            }
        });

        if (abstractElementAttributes.getReferencingShapes() != null && abstractElementAttributes.getReferencingShapes().size() > 1) {
            List<String> referencingShapes = abstractElementAttributes.getReferencingShapes();
            referencingShapes.remove(dto.getId());
            dto.setOtherVisualisationsOfSameLanguageConstruct(referencingShapes);
        }

        dto.setAbstractElementAttributes(abstractElementAttributes);

        dto.setImageUrl(visualInformationDto.getImageUrl());
        dto.setFromArrow(visualInformationDto.getFromArrow());
        dto.setToArrow(visualInformationDto.getToArrow());
        dto.setArrowStroke(visualInformationDto.getArrowStroke());

        return dto;
    }

    public void addContainedShape(String key) {
        if (this.containedShapes == null) this.containedShapes = new ArrayList<>();
        this.containedShapes.add(key);
    }

    public boolean hasOptionalValues() {
        return getShapeRepresentsModel() != null || getLabel() != null || getNote() != null || getxCoorDobotMagician() != null || getyCoorDobotMagician() != null || getzCoorDobotMagician() != null;
    }





    public String getxCoorDobotMagician() {
        return xCoorDobotMagician;
    }

    public void setxCoorDobotMagician(String xCoorDobotMagician) {
        this.xCoorDobotMagician = xCoorDobotMagician;
    }

    public String getyCoorDobotMagician() {
        return yCoorDobotMagician;
    }

    public void setyCoorDobotMagician(String yCoorDobotMagician) {
        this.yCoorDobotMagician = yCoorDobotMagician;
    }

    public String getzCoorDobotMagician() {
        return zCoorDobotMagician;
    }

    public void setzCoorDobotMagician(String zCoorDobotMagician) {
        this.zCoorDobotMagician = zCoorDobotMagician;
    }

    public boolean getSucCupOnDobotMagician() {
        return sucCupOnDobotMagician;
    }

    public void setSucCupOnDobotMagician(boolean sucCupOnDobotMagician) {
        this.sucCupOnDobotMagician = sucCupOnDobotMagician;
    }

    public boolean getSucCupOffDobotMagician() {
        return sucCupOffDobotMagician;
    }

    public void setSucCupOffDobotMagician(boolean sucCupOffDobotMagician) {
        this.sucCupOffDobotMagician = sucCupOffDobotMagician;
    }

    public boolean getCalDobotMagician() {
        return calDobotMagician;
    }

    public void setCalDobotMagician(boolean calDobotMagician) {
        this.calDobotMagician = calDobotMagician;
    }
}
