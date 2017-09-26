package ch.fhnw.modeller.model.metamodel;

import java.util.ArrayList;

public class MetamodelElement {
	String id;
	String label;
	String classType;	//Defines the type of the class (i.e. bpmn:Task)
	int imagePositionX0;
	int imagePositionY0;
	int labelPositionX1;
	int labelPositionY1;
	
	
	public MetamodelElement(String id, String label, String classType, int imagePositionX0,
			int imagePositionY0, int labelPositionX1, int labelPositionY1) {
		this.id = id;
		this.label = label;
		this.classType = classType;
		this.imagePositionX0 = imagePositionX0;
		this.imagePositionY0 = imagePositionY0;
		this.labelPositionX1 = labelPositionX1;
		this.labelPositionY1 = labelPositionY1;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getClassType() {
		return classType;
	}
	
	public void setClassType(String classType) {
		this.classType = classType;
	}
	
	public int getImagePositionX0() {
		return imagePositionX0;
	}
	
	public void setImagePositionX0(int imagePositionX0) {
		this.imagePositionX0 = imagePositionX0;
	}
	
	public int getImagePositionY0() {
		return imagePositionY0;
	}
	
	public void setImagePositionY0(int imagePositionY0) {
		this.imagePositionY0 = imagePositionY0;
	}
	
	public int getLabelPositionX1() {
		return labelPositionX1;
	}
	
	public void setLabelPositionX1(int labelPositionX1) {
		this.labelPositionX1 = labelPositionX1;
	}
	
	public int getLabelPositionY1() {
		return labelPositionY1;
	}
	
	public void setLabelPositionY1(int labelPositionY1) {
		this.labelPositionY1 = labelPositionY1;
	}
	
	
	
}
