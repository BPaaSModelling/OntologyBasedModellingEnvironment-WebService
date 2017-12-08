package ch.fhnw.modeller.model.metamodel;

import java.util.ArrayList;

public class MetamodelElement {
	String uuid;
	String textLabel;
	String classType;	//Defines the type of the class (i.e. bpmn:Task)
	double left;
	double top;
	
	public MetamodelElement(String uuid, String label, String classType, double left, double top) {
		super();
		this.uuid = uuid;
		this.textLabel = label;
		this.classType = classType;
		this.left = left;
		this.top = top;
	}
	
	
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getTextLabel() {
		return textLabel;
	}
	public void setLabel(String textLabel) {
		this.textLabel = textLabel;
	}
	public String getClassType() {
		return classType;
	}
	public void setClassType(String classType) {
		this.classType = classType;
	}
	public double getLeft() {
		return left;
	}
	public void setLeft(int left) {
		this.left = left;
	}
	public double getTop() {
		return top;
	}
	public void setTop(int top) {
		this.top = top;
	}
	
	
	
	
}
