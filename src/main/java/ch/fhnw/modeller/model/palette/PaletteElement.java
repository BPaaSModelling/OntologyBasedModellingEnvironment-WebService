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
	private String borderColor;
	private String borderThickness;
	private String borderType;
	private String toArrow;
	private String fromArrow;
	private String arrowStroke;
	private String type;

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

	public String getBorderColor() {
		return borderColor;
	}

	public void setBorderColor(String borderColor) {
		this.borderColor = borderColor;
	}

	public String getBorderThickness() {
		return borderThickness;
	}

	public void setBorderThickness(String borderThickness) {
		this.borderThickness = borderThickness;
	}

	public String getBorderType() {
		return borderType;
	}

	public void setBorderType(String borderType) {
		this.borderType = borderType;
	}

	public String getToArrow() {
		return toArrow;
	}

	public void setToArrow(String toArrow) {
		this.toArrow = toArrow;
	}

	public String getFromArrow() {
		return fromArrow;
	}

	public void setFromArrow(String fromArrow) {
		this.fromArrow = fromArrow;
	}

	public String getArrowStroke() {
		return arrowStroke;
	}

	public void setArrowStroke(String arrowStroke) {
		this.arrowStroke = arrowStroke;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public PaletteElement() {
		super();
	}
	
}
