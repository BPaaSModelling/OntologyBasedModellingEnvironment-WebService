package ch.fhnw.modeller.model.palette;

import java.util.ArrayList;

public class PaletteElement {

	String id;
	String label;
	String imageURL;
	String thumbnailURL;
	boolean showedInPalette;
	String paletteCategory;			
	String representedClass;		//might be bpmn:Task
	String representedClassLabel;	//might be "Task"
	String parentElement;			
	int textLabelSizeX2;			//defines the dimension of the label
	int textLabelSizeY2;			// -- same
	ArrayList<PaletteElement> childElements;
	
	
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
	
	public String getImageURL() {
		return imageURL;
	}
	
	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}
	
	public String getThumbnailURL() {
		return thumbnailURL;
	}
	
	public void setThumbnailURL(String thumbnailURL) {
		this.thumbnailURL = thumbnailURL;
	}
	
	public String getPaletteCategory() {
		return paletteCategory;
	}
	
	public void setPaletteCategory(String paletteCategory) {
		this.paletteCategory = paletteCategory;
	}
	
	public String getRepresentedClass() {
		return representedClass;
	}
	
	public void setRepresentedClass(String representedClass) {
		this.representedClass = representedClass;
	}
	
	public String getRepresentedClassLabel() {
		return representedClassLabel;
	}
	
	public void setRepresentedClassLabel(String representedClassLabel) {
		this.representedClassLabel = representedClassLabel;
	}
	
	public int getTextLabelSizeX2() {
		return textLabelSizeX2;
	}
	
	public void setTextLabelSizeX2(int textLabelSizeX2) {
		this.textLabelSizeX2 = textLabelSizeX2;
	}
	
	public int getTextLabelSizeY2() {
		return textLabelSizeY2;
	}
	
	public void setTextLabelSizeY2(int textLabelSizeY2) {
		this.textLabelSizeY2 = textLabelSizeY2;
	}
	
	public ArrayList<PaletteElement> getChildElements() {
		return childElements;
	}
	
	public void setChildElements(ArrayList<PaletteElement> childElements) {
		this.childElements = childElements;
	}

	public String getParentElement() {
		return parentElement;
	}

	public void setParentElement(String parentElement) {
		this.parentElement = parentElement;
	}

	public boolean isShowedInPalette() {
		return showedInPalette;
	}

	public void setShowedInPalette(boolean showedInPalette) {
		this.showedInPalette = showedInPalette;
	}

	public PaletteElement(String id, String label, String imageURL, String thumbnailURL, boolean showedInPalette,
			String paletteCategory, String representedClass, String representedClassLabel, String parentElement,
			int textLabelSizeX2, int textLabelSizeY2) {
		super();
		this.id = id;
		this.label = label;
		this.imageURL = imageURL;
		this.thumbnailURL = thumbnailURL;
		this.showedInPalette = showedInPalette;
		this.paletteCategory = paletteCategory;
		this.representedClass = representedClass;
		this.representedClassLabel = representedClassLabel;
		this.parentElement = parentElement;
		this.textLabelSizeX2 = textLabelSizeX2;
		this.textLabelSizeY2 = textLabelSizeY2;
		this.childElements = new ArrayList<PaletteElement>();
	}

	public PaletteElement(String id, String label, String imageURL, String thumbnailURL, boolean showedInPalette,
			String paletteCategory, String representedClass, String representedClassLabel, String parentElement,
			int textLabelSizeX2, int textLabelSizeY2, ArrayList<PaletteElement> childElements) {
		super();
		this.id = id;
		this.label = label;
		this.imageURL = imageURL;
		this.thumbnailURL = thumbnailURL;
		this.showedInPalette = showedInPalette;
		this.paletteCategory = paletteCategory;
		this.representedClass = representedClass;
		this.representedClassLabel = representedClassLabel;
		this.parentElement = parentElement;
		this.textLabelSizeX2 = textLabelSizeX2;
		this.textLabelSizeY2 = textLabelSizeY2;
		this.childElements = childElements;
	}

	public PaletteElement(){
		this.childElements = new ArrayList<PaletteElement>();
	}
	
	

	
	
}
