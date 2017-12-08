package ch.fhnw.modeller.model.palette;

public class PaletteCategory {
	String id;
	String label;
	int orderNumber;
	boolean hiddenFromPalette;
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
	public int getOrderNumber() {
		return orderNumber;
	}
	public void setOrderNumber(int orderNumber) {
		this.orderNumber = orderNumber;
	}
	public boolean isHiddenFromPalette() {
		return hiddenFromPalette;
	}
	public void setHiddenFromPalette(boolean hiddenFromPalette) {
		this.hiddenFromPalette = hiddenFromPalette;
	}
	public PaletteCategory(String id, String label, int orderNumber, boolean hiddenFromPalette) {
		super();
		this.id = id;
		this.label = label;
		this.orderNumber = orderNumber;
		this.hiddenFromPalette = hiddenFromPalette;
	}
	
	
	public PaletteCategory() {
		super();
	}
	
	

	
}
