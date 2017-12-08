package ch.fhnw.modeller.model.palette;

import java.util.ArrayList;

public class PaletteElement extends PaletteModel {

	private String shape;
	private String backgroundColor;
	private String height;
	private String width;
	private String labelPosition;
	private String iconURL;
	private String iconPosition;
	private Boolean usesImages;
	private String imageURL;
	private String thumbnailURL;
			
	private String representedClass;		//might be bpmn:Task

	public String getShape() {
		return shape;
	}

	public void setShape(String shape) {
		this.shape = shape;
	}

	public String getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(String backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public String getHeight() {
		return height;
	}

	public void setHeight(String height) {
		this.height = height;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public String getLabelPosition() {
		return labelPosition;
	}

	public void setLabelPosition(String labelPosition) {
		this.labelPosition = labelPosition;
	}

	public String getIconURL() {
		return iconURL;
	}

	public void setIconURL(String iconURL) {
		this.iconURL = iconURL;
	}

	public String getIconPosition() {
		return iconPosition;
	}

	public void setIconPosition(String iconPosition) {
		this.iconPosition = iconPosition;
	}

	public Boolean getUsesImages() {
		return usesImages;
	}

	public void setUsesImages(Boolean usesImages) {
		this.usesImages = usesImages;
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

	public String getRepresentedClass() {
		return representedClass;
	}

	public void setRepresentedClass(String representedClass) {
		this.representedClass = representedClass;
	}

	public PaletteElement() {
		super();
	}
	

	
}
