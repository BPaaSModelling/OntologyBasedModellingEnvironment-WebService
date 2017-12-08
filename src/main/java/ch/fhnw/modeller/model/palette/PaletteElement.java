package ch.fhnw.modeller.model.palette;

import java.util.ArrayList;

public class PaletteElement extends PaletteModel {

	private String shape;
	private String backgroundColor;
	private int height;
	private int width;
	private String labelPosition;
	private String iconURL;
	private String iconPosition;
	private Boolean usesImage;
	private String imageURL;
	private String thumbnailURL;
					

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

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
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

	public Boolean getUsesImage() {
		return usesImage;
	}

	public void setUsesImage(Boolean usesImage) {
		this.usesImage = usesImage;
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


	public PaletteElement() {
		super();
	}
	

	
}
