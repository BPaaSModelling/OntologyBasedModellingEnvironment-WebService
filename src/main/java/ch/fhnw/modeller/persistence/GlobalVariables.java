package ch.fhnw.modeller.persistence;

import java.util.HashMap;

import ch.fhnw.modeller.webservice.ontology.NAMESPACE;

public class GlobalVariables {

	public static final String BOOLEAN_TRUE_URI = "true^^http://www.w3.org/2001/XMLSchema#boolean";
	public static final String BOOLEAN_FALSE_URI = "false^^http://www.w3.org/2001/XMLSchema#boolean";
	private static final HashMap<String, String> NAMESPACE_MAP = new HashMap<String, String>();
	private static final HashMap<String, String> PREFIX_MAP = new HashMap<String, String>();
	
	public static HashMap<String, String> getNamespaceMap() { // this is a dual hashmap to get prefix from uri and vice versa
		for (NAMESPACE ns : NAMESPACE.values()) {
			String namespace = ns.getURI().split("#")[0];
			NAMESPACE_MAP.put(namespace, ns.getPrefix());
			NAMESPACE_MAP.put(ns.getPrefix(), namespace);
		}
		
		return NAMESPACE_MAP;
	}
	
	public static HashMap<String, String> getPrefixMap() {
		for (NAMESPACE ns : NAMESPACE.values()) {
			PREFIX_MAP.put(ns.getPrefix(), ns.getURI()); 
		}
		
		return PREFIX_MAP;
	}
	
}
