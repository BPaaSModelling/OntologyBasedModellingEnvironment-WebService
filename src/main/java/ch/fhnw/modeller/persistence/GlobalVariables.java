package ch.fhnw.modeller.persistence;

import java.util.HashMap;

import ch.fhnw.modeller.webservice.ontology.NAMESPACE;

public class GlobalVariables {

	public static final String BOOLEAN_TRUE_URI = "true^^http://www.w3.org/2001/XMLSchema#boolean";
	public static final String BOOLEAN_FALSE_URI = "false^^http://www.w3.org/2001/XMLSchema#boolean";
	private static final HashMap<String, String> NAMESPACE_MAP = new HashMap<String, String>();
	
	public static HashMap<String, String> getNamespaceMap() {
		for (NAMESPACE ns : NAMESPACE.values()) {
			NAMESPACE_MAP.put(ns.getURI(), ns.getPrefix());
		}
		
		return NAMESPACE_MAP;
	}
	
}
