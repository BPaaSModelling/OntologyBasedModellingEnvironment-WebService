package ch.fhnw.modeller.model.palette;

import java.util.ArrayList;

public class PaletteModel {

	private String uuid;
	private String id;
	private String label;
	private String paletteCategory; //points to the uuid of category
	private String parentElement;	//point to the uuid of parent element
	private Boolean hiddenFromPalette;
	private ArrayList<PaletteElement> childElements;
	private String representedLanguageClass;
	private String representedDomainClass;
	
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
	public String getRepresentedDomainClass() {
		return representedDomainClass;
	}
	public void setRepresentedDomainClass(String representedDomainClass) {
		this.representedDomainClass = representedDomainClass;
	}
	
	
}
