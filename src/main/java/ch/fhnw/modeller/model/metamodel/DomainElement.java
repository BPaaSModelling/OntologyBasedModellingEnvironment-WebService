package ch.fhnw.modeller.model.metamodel;

public class DomainElement {
private String id;
private String label;
private String parentElement;
private boolean isRoot;

public DomainElement() {

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

public String getParentElement() {
	return parentElement;
}

public void setParentElement(String parentElement) {
	this.parentElement = parentElement;
}

public boolean isRoot() {
	return isRoot;
}

public void setRoot(boolean isRoot) {
	this.isRoot = isRoot;
}

}
