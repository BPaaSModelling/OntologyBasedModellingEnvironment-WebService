package ch.fhnw.modeller.model.metamodel;

public class ModelingLanguage {
	
	private String id;
	private String label;
	private boolean hasModelingView;
	private String viewIsPartOfModelingLanguage;
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
	public boolean isHasModelingView() {
		return hasModelingView;
	}
	public void setHasModelingView(boolean hasModelingView) {
		this.hasModelingView = hasModelingView;
	}
	public String getViewIsPartOfModelingLanguage() {
		return viewIsPartOfModelingLanguage;
	}
	public void setViewIsPartOfModelingLanguage(String viewIsPartOfModelingLanguage) {
		this.viewIsPartOfModelingLanguage = viewIsPartOfModelingLanguage;
	}
	

}
