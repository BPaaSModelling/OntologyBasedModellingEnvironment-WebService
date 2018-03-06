package ch.fhnw.modeller.model.metamodel;

import java.util.ArrayList;

public class GraphicalElement {
	String uuid;
	String label;
	String classType;	//Defines the type of the class (i.e. bpmn:Task)
	String hiddenFromPallette;
	String parent;
	String category;
	double x;
	double y;
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
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
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	public GraphicalElement() {
		super();
	}
	public String getHiddenFromPallette() {
		return hiddenFromPallette;
	}
	public void setHiddenFromPallette(String hiddenFromPallette) {
		this.hiddenFromPallette = hiddenFromPallette;
	}
	public String getParent() {
		return parent;
	}
	public void setParent(String parent) {
		this.parent = parent;
	}
	
	
	
}
