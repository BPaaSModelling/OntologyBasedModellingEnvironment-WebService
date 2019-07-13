package ch.fhnw.modeller.model.palette;

import java.util.ArrayList;

import ch.fhnw.modeller.model.graphEnvironment.Answer;
import ch.fhnw.modeller.model.metamodel.DatatypeProperty;
import ch.fhnw.modeller.model.metamodel.DomainElement;

public class PaletteModel {

	private String uuid;
	private String id;
	private String label;
	private String paletteCategory; //points to the uuid of category
	private String parentElement;	//point to the uuid of parent element
	private String parentLanguageClass; //point to the language concept of the parent
	private Boolean hiddenFromPalette;
	private ArrayList<PaletteElement> childElements;
	private String representedLanguageClass;
	private ArrayList<Answer> languageSubclasses;
	private ArrayList<String> representedDomainClass;
	private String categoryLabel;
	private String languagePrefix;
	private String comment;
	private ArrayList<DatatypeProperty> inheritedProperties;
	/*private String datatypePropertyId;
	private String datatypePropertyLabel;
	private String datatypePropertyValue;*/
	
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
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
	public String getPaletteCategory() {
		return paletteCategory;
	}
	public void setPaletteCategory(String paletteCategory) {
		this.paletteCategory = paletteCategory;
	}
	public String getCategoryLabel() {
		return categoryLabel;
	}
	public void setCategoryLabel(String categoryLabel) {
		this.categoryLabel = categoryLabel;
	}
	public String getParentElement() {
		return parentElement;
	}
	public void setParentElement(String parentElement) {
		this.parentElement = parentElement;
	}
	public Boolean getHiddenFromPalette() {
		return hiddenFromPalette;
	}
	public void setHiddenFromPalette(Boolean hiddenFromPalette) {
		this.hiddenFromPalette = hiddenFromPalette;
	}
	public ArrayList<PaletteElement> getChildElements() {
		return childElements;
	}
	public void setChildElements(ArrayList<PaletteElement> childElements) {
		this.childElements = childElements;
	}
	
	public String getRepresentedLanguageClass() {
		return representedLanguageClass;
	}
	public void setRepresentedLanguageClass(String representedLanguageClass) {
		this.representedLanguageClass = representedLanguageClass;
	}
	public PaletteModel() {
		super();
		this.childElements = new ArrayList<PaletteElement>();
	}
	public ArrayList<String> getRepresentedDomainClass() {
		return representedDomainClass;
	}
	public void setRepresentedDomainClass(ArrayList<String> representedDomainClass) {
		this.representedDomainClass = representedDomainClass;
	}
	
	/*public String getDatatypePropertyLabel() {
		return datatypePropertyLabel;
	}
	public void setDatatypePropertyLabel(String datatypePropertyLabel) {
		this.datatypePropertyLabel = datatypePropertyLabel;
	}
	public String getDatatypePropertyId() {
		return datatypePropertyId;
	}
	public void setDatatypePropertyId(String datatypePropertyId) {
		this.datatypePropertyId = datatypePropertyId;
	}
	public String getDatatypePropertyValue() {
		return datatypePropertyValue;
	}
	public void setDatatypePropertyValue(String datatypePropertyValue) {
		this.datatypePropertyValue = datatypePropertyValue;
	}*/
	public ArrayList<Answer> getLanguageSubclasses() {
		return languageSubclasses;
	}
	public void setLanguageSubclasses(ArrayList<Answer> languageSubclasses) {
		this.languageSubclasses = languageSubclasses;
	}
	public String getLanguagePrefix() {
		return languagePrefix;
	}
	public void setLanguagePrefix(String languagePrefix) {
		this.languagePrefix = languagePrefix;
	}
	public String getParentLanguageClass() {
		return parentLanguageClass;
	}
	public void setParentLanguageClass(String parentLanguageClass) {
		this.parentLanguageClass = parentLanguageClass;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public ArrayList<DatatypeProperty> getInheritedProperties() {
		return inheritedProperties;
	}
	public void setInheritedProperties(ArrayList<DatatypeProperty> inheritedProperties) {
		this.inheritedProperties = inheritedProperties;
	}
	
}
