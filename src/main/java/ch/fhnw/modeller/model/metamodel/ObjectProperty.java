package ch.fhnw.modeller.model.metamodel;

public class ObjectProperty {
private String id;
private String domainName;
private String label;
private String range;
private String defaultValue;

public ObjectProperty() {

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

public String getDomainName() {
	return domainName;
}

public void setDomainName(String domainName) {
	this.domainName = domainName;
}

public String getRange() {
	return range;
}

public void setRange(String range) {
	this.range = range;
}

public String getDefaultValue() {
	return defaultValue;
}

public void setDefaultValue(String defaultValue) {
	this.defaultValue = defaultValue;
}

}
