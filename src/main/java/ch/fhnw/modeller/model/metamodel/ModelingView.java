package ch.fhnw.modeller.model.metamodel;

public class ModelingView {
		
		private String id;
		private String label;
		private boolean isMainModelingView;
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
		public boolean isMainModelingView() {
			return isMainModelingView;
		}
		public void setMainModelingView(boolean isMainModelingView) {
			this.isMainModelingView = isMainModelingView;
		}
		public String getViewIsPartOfModelingLanguage() {
			return viewIsPartOfModelingLanguage;
		}
		public void setViewIsPartOfModelingLanguage(String viewIsPartOfModelingLanguage) {
			this.viewIsPartOfModelingLanguage = viewIsPartOfModelingLanguage;
		}
		



}
