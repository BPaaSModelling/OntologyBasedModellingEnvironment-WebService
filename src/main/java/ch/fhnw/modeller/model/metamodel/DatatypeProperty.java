package ch.fhnw.modeller.model.metamodel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DatatypeProperty {
	private String id;
	private String domainName;
	private String label;
	private String range;
	private String defaultValue;
	private boolean isAvailableToModel;
}
